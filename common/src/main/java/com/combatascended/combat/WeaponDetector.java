package com.combatascended.combat;

import com.combatascended.CombatAscended;
import com.combatascended.data.DetectionRules;
import com.combatascended.data.WeaponDataLoader;
import com.combatascended.platform.Platform;
import com.combatascended.data.WeaponData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Donne un profil à chaque objet : d'abord ce qui est déclaré (fichier d'arme, tag d'archétype), puis ce qui se
 * déduit (fichiers de Better Combat ou d'Epic Fight fournis par les mods, mots du nom, simple preuve d'arme).
 */
public final class WeaponDetector {
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final String TAG_PREFIX = "#";
    private static final String ARCHETYPE_TAG_FOLDER = "archetypes/";
    /** Sans rien de plus précis, une arme se comporte comme une épée vanilla. */
    static final String GENERIC_ARCHETYPE = "sword";
    private static final TagKey<Item> CONVENTIONAL_MELEE = TagKey.create(Registries.ITEM, new ResourceLocation(Platform.get().commonTag("c:tools/melee_weapon")));

    /** Archétypes déduits d'un parent de Better Combat ou d'un type d'Epic Fight, par nom sans namespace. */
    private static final Map<String, String> FOREIGN_ARCHETYPES = Map.ofEntries(
            Map.entry("uchigatana", "katana"), Map.entry("heavy_axe", "greataxe"), Map.entry("double_axe", "double_axe"),
            Map.entry("twin_blade", "twinblade"), Map.entry("soul_knife", "knife"), Map.entry("coral_blade", "sword"),
            Map.entry("battlestaff", "battlestaff"), Map.entry("staff", "quarterstaff"), Map.entry("wand", "quarterstaff"),
            Map.entry("vanilla_mace", "mace"), Map.entry("tachi", "tachi"));

    private WeaponDetector() {
    }

    /** Le profil de chaque objet, à partir de ces données. Les archétypes doivent déjà être chargés. */
    public static Map<ResourceLocation, WeaponData.ItemProfile> resolve(WeaponData.Snapshot data) {
        Map<ResourceLocation, WeaponData.ItemProfile> table = new TreeMap<>();
        Map<ResourceLocation, ResourceLocation> exact = new HashMap<>();
        List<TagBinding> tags = new ArrayList<>();
        data.weapons().forEach((id, definition) -> {
            for (String entry : definition.items()) {
                if (entry.startsWith(TAG_PREFIX)) tags.add(new TagBinding(TagKey.create(Registries.ITEM, new ResourceLocation(entry.substring(1))), id));
                else exact.putIfAbsent(new ResourceLocation(entry), id);
            }
        });
        List<ArchetypeTag> archetypeTags = data.archetypes().keySet().stream()
                .flatMap(id -> Stream.of(id.getNamespace(), id.getNamespace().equals(CombatAscended.MODID) ? WeaponDataLoader.LEGACY_DATA_DIRECTORY : null)
                        // Les archétypes de ce mod se remplissent aussi par l'ancien tag combat_reforged:archetypes/<id>.
                        .filter(Objects::nonNull)
                        .map(namespace -> new ArchetypeTag(TagKey.create(Registries.ITEM, new ResourceLocation(namespace, ARCHETYPE_TAG_FOLDER + id.getPath())), id)))
                .toList();
        List<Keyword> keywords = keywords(data.detection());
        Set<String> excludedWords = new HashSet<>(data.detection().excludedWords());
        Set<String> excludedNamespaces = new HashSet<>(data.detection().excludedNamespaces());
        Set<ResourceLocation> excludedItems = new HashSet<>(data.detection().excludedItems());

        for (Holder.Reference<Item> holder : BuiltInRegistries.ITEM.holders().toList()) {
            ResourceLocation id = holder.key().location();
            Item item = holder.value();
            WeaponData.ItemProfile profile = declared(id, holder, exact, tags, archetypeTags);
            if (profile == null && isDetectable(id, item, holder, excludedWords, excludedNamespaces, excludedItems)) {
                profile = detected(id, item, data, keywords);
            }
            if (profile != null && data.definitionOrNull(profile.profile()) != null) table.put(id, profile);
        }
        return table;
    }

    private record TagBinding(TagKey<Item> tag, ResourceLocation profile) {
    }

    private record ArchetypeTag(TagKey<Item> tag, ResourceLocation archetype) {
    }

    private record Keyword(List<String> parts, ResourceLocation archetype, boolean suffix, int length) {
    }

    private static WeaponData.ItemProfile declared(ResourceLocation id, Holder.Reference<Item> holder, Map<ResourceLocation, ResourceLocation> exact,
                                                   List<TagBinding> tags, List<ArchetypeTag> archetypeTags) {
        ResourceLocation file = exact.get(id);
        if (file != null) return new WeaponData.ItemProfile(file, WeaponData.Source.FILE, false);
        for (TagBinding binding : tags) {
            if (holder.is(binding.tag())) return new WeaponData.ItemProfile(binding.profile(), WeaponData.Source.FILE, false);
        }
        for (ArchetypeTag binding : archetypeTags) {
            if (holder.is(binding.tag())) return new WeaponData.ItemProfile(binding.archetype(), WeaponData.Source.TAG, false);
        }
        return null;
    }

    /** Une arme de vanilla n'est jamais détectée : seules les épées sont déclarées, par identifiant. */
    private static boolean isDetectable(ResourceLocation id, Item item, Holder<Item> holder, Set<String> excludedWords, Set<String> excludedNamespaces, Set<ResourceLocation> excludedItems) {
        if (id.getNamespace().equals(VANILLA_NAMESPACE) || excludedNamespaces.contains(id.getNamespace()) || excludedItems.contains(id)) return false;
        for (String word : words(id.getPath())) {
            if (excludedWords.contains(word)) return false;
        }
        return isWeapon(item, holder);
    }

    private static WeaponData.ItemProfile detected(ResourceLocation id, Item item, WeaponData.Snapshot data, List<Keyword> keywords) {
        boolean ownRightClick = hasOwnRightClick(item);
        Optional<ResourceLocation> foreign = ForeignWeaponData.archetype(id);
        if (foreign.isPresent()) {
            ResourceLocation archetype = mapForeign(foreign.get(), data);
            if (archetype != null) return new WeaponData.ItemProfile(archetype, ForeignWeaponData.source(id), ownRightClick);
        }
        ResourceLocation byName = fromName(id.getPath(), keywords);
        if (byName != null) return new WeaponData.ItemProfile(byName, WeaponData.Source.NAME, ownRightClick);
        return new WeaponData.ItemProfile(archetype(GENERIC_ARCHETYPE), WeaponData.Source.FALLBACK, ownRightClick);
    }

    private static ResourceLocation archetype(String name) {
        return new ResourceLocation("combat_ascended", name);
    }

    /** Un archétype du même nom que le preset étranger, sinon sa correspondance connue. */
    private static ResourceLocation mapForeign(ResourceLocation foreign, WeaponData.Snapshot data) {
        String name = FOREIGN_ARCHETYPES.getOrDefault(foreign.getPath(), foreign.getPath());
        ResourceLocation id = archetype(name);
        return data.archetypes().containsKey(id) ? id : null;
    }

    private static List<Keyword> keywords(DetectionRules rules) {
        List<Keyword> keywords = new ArrayList<>();
        for (DetectionRules.Keyword keyword : rules.keywords()) {
            for (String word : keyword.words()) {
                String normal = word.toLowerCase(Locale.ROOT);
                keywords.add(new Keyword(Arrays.asList(normal.split("_")), keyword.archetype(), keyword.suffix(), normal.length()));
            }
        }
        // Le plus précis d'abord : "great_sword" avant "sword".
        keywords.sort(Comparator.comparingInt((Keyword keyword) -> keyword.parts().size()).thenComparingInt(Keyword::length).reversed());
        return keywords;
    }

    /** Le mot le plus précis reconnu dans le nom. Découper en mots évite "sai" dans "mosaic" ou "axe" dans "pickaxe". */
    static ResourceLocation fromName(String path, List<Keyword> keywords) {
        List<String> words = words(path);
        for (Keyword keyword : keywords) {
            if (matches(words, keyword)) return keyword.archetype();
        }
        return null;
    }

    private static boolean matches(List<String> words, Keyword keyword) {
        List<String> parts = keyword.parts();
        for (int start = 0; start + parts.size() <= words.size(); start++) {
            if (words.subList(start, start + parts.size()).equals(parts)) return true;
        }
        if (!keyword.suffix() || parts.size() != 1) return false;
        String word = parts.get(0);
        for (String candidate : words) {
            if (candidate.length() > word.length() && candidate.endsWith(word)) return true;
        }
        return false;
    }

    /** Archétype déduit du nom de l'objet seul, sans vérifier que c'est une arme : null si aucun mot n'est reconnu. */
    public static ResourceLocation classify(String path, DetectionRules rules) {
        return fromName(path, keywords(rules));
    }

    static List<String> words(String path) {
        return Arrays.asList(path.toLowerCase(Locale.ROOT).split("[_/.\\-]+"));
    }

    /** Vrai si tout indique une arme : classe, tag, ou un bonus de dégâts en main principale. Les outils de minage en sont exclus. */
    static boolean isWeapon(Item item, Holder<Item> holder) {
        if (item instanceof SwordItem || item instanceof TridentItem) return true;
        if (item instanceof AxeItem) return true;
        if (item instanceof DiggerItem) return false;
        if (holder.is(ItemTags.SWORDS) || holder.is(ItemTags.AXES) || holder.is(CONVENTIONAL_MELEE)) return true;
        return hasAttackDamageBonus(item);
    }

    private static boolean hasAttackDamageBonus(Item item) {
        return item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().anyMatch(modifier -> modifier.getAmount() > 0.0D);
    }

    /**
     * Vrai si la classe de l'objet redéfinit son clic droit. La recherche se fait par signature, car les noms de
     * méthodes ne sont pas les mêmes selon le chargeur : on regarde les classes propres à l'objet, au-dessus de celles du jeu.
     */
    static boolean hasOwnRightClick(Item item) {
        for (Class<?> type = item.getClass(); type != null && !isVanillaBase(type); type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (method.getReturnType() == InteractionResultHolder.class && parameters.length == 3
                        && parameters[0] == Level.class && parameters[1] == Player.class && parameters[2] == InteractionHand.class) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isVanillaBase(Class<?> type) {
        return type == Item.class || type == TieredItem.class || type == DiggerItem.class || type == SwordItem.class || type == AxeItem.class;
    }

    /** Pour les tests : le profil calculé pour un objet déjà enregistré. */
    public static Optional<WeaponData.ItemProfile> profileOf(ItemStack stack) {
        return Optional.ofNullable(WeaponProfiles.table().get(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

}
