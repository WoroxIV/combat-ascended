package com.combatreforged.combat;

import com.combatreforged.data.DataProblem;
import com.combatreforged.data.WeaponData;
import com.combatreforged.data.WeaponDataLoader;
import com.combatreforged.data.WeaponDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le profil de combat d'un objet. Tout le code qui se demande "quelle arme est-ce ?" passe par ici.
 * Un objet reçoit son profil d'un fichier d'arme, d'un tag, ou de la détection automatique ({@link WeaponDetector}).
 * <p>
 * La table complète est calculée une seule fois par chargement de données, à la première demande : les tags ne
 * sont pas encore à jour quand les fichiers se chargent. Le client reçoit la table toute prête du serveur.
 */
public final class WeaponProfiles {
    /** Dernière table construite, réutilisée tant que les données n'ont pas changé. */
    private static volatile Bindings cache;

    private WeaponProfiles() {
    }

    /**
     * @param id         identifiant du fichier d'arme ou de l'archétype : deux objets de même id partagent combo et recharge
     * @param definition comportement de cet objet
     * @param source     d'où vient ce profil
     */
    public record Profile(ResourceLocation id, WeaponDefinition definition, WeaponData.Source source) {
        /** Vrai si le profil a été deviné : aucun fichier ne l'a demandé pour cet objet. */
        public boolean detected() {
            return source.detected();
        }
    }

    private record Bindings(int generation, Map<Item, Profile> items, Map<ResourceLocation, List<Item>> byProfile) {
    }

    public static Optional<Profile> of(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return Optional.ofNullable(bindings().items().get(stack.getItem()));
    }

    public static Optional<WeaponDefinition> definition(ItemStack stack) {
        return of(stack).map(Profile::definition);
    }

    public static Optional<ResourceLocation> id(ItemStack stack) {
        return of(stack).map(Profile::id);
    }

    /** Vrai si cet objet a un profil d'arme. */
    public static boolean isAdopted(ItemStack stack) {
        return of(stack).isPresent();
    }

    /** Les objets qui partagent ce profil (recharges communes à tous les matériaux d'une arme). */
    public static List<Item> itemsOf(ResourceLocation profile) {
        return bindings().byProfile().getOrDefault(profile, List.of());
    }

    /** La table complète, par identifiant d'objet : envoyée aux clients. */
    public static Map<ResourceLocation, WeaponData.ItemProfile> table() {
        return WeaponDetector.resolve(WeaponData.current());
    }

    private static Bindings bindings() {
        WeaponData.Versioned data = WeaponData.versioned();
        Bindings current = cache;
        if (current != null && current.generation() == data.generation()) return current;

        Map<ResourceLocation, WeaponData.ItemProfile> table = data.snapshot().items();
        if (table.isEmpty()) {
            // Pendant le calcul, les objets n'ont pas encore de profil : le calcul lit leurs bonus d'attributs.
            cache = new Bindings(data.generation(), Map.of(), Map.of());
            table = WeaponDetector.resolve(data.snapshot());
        }
        current = build(data, table);
        cache = current;
        return current;
    }

    private static Bindings build(WeaponData.Versioned data, Map<ResourceLocation, WeaponData.ItemProfile> table) {
        Map<Item, Profile> items = new IdentityHashMap<>();
        Map<ResourceLocation, List<Item>> byProfile = new HashMap<>();
        Map<String, Profile> shared = new HashMap<>();
        table.forEach((itemId, entry) -> BuiltInRegistries.ITEM.getOptional(itemId).ifPresent(item -> {
            WeaponDefinition definition = data.snapshot().definitionOrNull(entry.profile());
            if (definition == null) return;
            String key = entry.profile() + "|" + entry.source().getSerializedName() + "|" + entry.keepsOwnRightClick();
            Profile profile = shared.computeIfAbsent(key, ignored -> new Profile(entry.profile(),
                    entry.source().detected() ? definition.forDetectedItem(entry.keepsOwnRightClick()) : definition, entry.source()));
            items.put(item, profile);
            byProfile.computeIfAbsent(entry.profile(), ignored -> new ArrayList<>()).add(item);
        }));
        return new Bindings(data.generation(), items, byProfile);
    }

    /** Signale un objet (ou un tag) revendiqué par deux fichiers d'armes : seul le premier compte. */
    public static void reportConflicts(Map<ResourceLocation, WeaponDefinition> weapons, List<DataProblem> problems) {
        Map<String, ResourceLocation> owners = new HashMap<>();
        weapons.forEach((id, definition) -> {
            for (String entry : definition.items()) {
                ResourceLocation owner = owners.putIfAbsent(entry, id);
                if (owner != null && !owner.equals(id)) {
                    problems.add(new DataProblem(DataProblem.Kind.ITEM_CONFLICT, id.toString(), owner.toString(), entry));
                }
            }
        });
    }

    /** Pour la commande d'audit : le chemin d'un fichier d'arme, dans les messages. */
    public static String weaponFileName(ResourceLocation id) {
        return id.getNamespace() + "/" + WeaponDataLoader.DATA_DIRECTORY + "/" + WeaponDataLoader.WEAPONS_DIRECTORY + "/" + id.getPath() + ".json";
    }
}
