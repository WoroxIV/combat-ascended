package com.combatreforged.config;

import com.combatreforged.CombatReforged;
import com.combatreforged.config.ClientConfig;
import com.combatreforged.data.DataProblem;
import com.combatreforged.data.FieldSet;
import com.combatreforged.data.GearDefinition;
import com.combatreforged.data.JsonUtil;
import com.combatreforged.data.RulesDefinition;
import com.combatreforged.data.WeaponDataLoader;
import com.combatreforged.data.WeaponDefinition;
import com.combatreforged.platform.Platform;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reprise des réglages de Better Weaponry 2.0, quand le combat était dans ce mod.
 * Le dossier {@code config/better_weaponry/} est importé une fois et n'est jamais modifié ; les réglages propres à
 * un monde (datapacks) restent lus comme couche basse le temps d'une transition.
 */
public final class LegacyBetterWeaponry {
    private static final String OLD_NAMESPACE = "better_weaponry";
    private static final String OLD_DATA_DIRECTORY = "better_weaponry";
    private static final String IMPORTED_MARKER = ".imported_from_better_weaponry";
    private static final String OLD_REACH = "attack_reach";
    private static final String OLD_ENCHANTMENT_PREFIX = OLD_NAMESPACE + ":";
    private static final double PLAYER_BASE_REACH = 3.0D;
    private static final String OLD_MOBS = "mobs";
    private static final String OLD_GUARD_CHANCE = "guard_chance";
    private static final Set<String> RULE_FIELDS = RulesDefinition.FIELDS.byName().keySet();
    private static final Set<String> GEAR_FIELDS = GearDefinition.FIELDS.byName().keySet();
    private static final Set<String> WEAPON_FIELDS = WeaponDefinition.FIELDS.byName().keySet();

    private LegacyBetterWeaponry() {
    }

    /** Importe une seule fois les règles et les réglages de combat des armes, sans toucher à l'original. */
    static void importConfig() throws IOException {
        Path oldRoot = Platform.get().configDir().resolve(OLD_NAMESPACE);
        Path marker = ConfigFolder.root().resolve(IMPORTED_MARKER);
        if (!Files.isDirectory(oldRoot) || Files.exists(marker)) return;

        Path oldClient = Platform.get().configDir().resolve(OLD_NAMESPACE + "-client.json");
        Path newClient = Platform.get().configDir().resolve(ClientConfig.FILE_NAME);
        if (Files.isRegularFile(oldClient) && !Files.exists(newClient)) Files.copy(oldClient, newClient);

        importFile(oldRoot.resolve(WeaponDataLoader.RULES_FILE), ConfigFolder.rulesFile(), LegacyBetterWeaponry::keepRules, "./" + ConfigFolder.RULES_SCHEMA);
        importFile(oldRoot.resolve(WeaponDataLoader.GEAR_FILE), ConfigFolder.gearFile(), LegacyBetterWeaponry::keepGear, "./" + ConfigFolder.GEAR_SCHEMA);
        Path oldWeapons = oldRoot.resolve(WeaponDataLoader.WEAPONS_DIRECTORY);
        if (Files.isDirectory(oldWeapons)) {
            try (Stream<Path> files = Files.list(oldWeapons)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                    String name = file.getFileName().toString().replace(".json", "");
                    Path target = ConfigFolder.dataFile(WeaponDataLoader.WEAPONS_DIRECTORY, new ResourceLocation(OLD_NAMESPACE, name));
                    importFile(file, target, LegacyBetterWeaponry::keepWeapon, ConfigFolder.WEAPON_SCHEMA_FROM_DATA);
                }
            }
        }
        Files.writeString(marker, "Settings of Better Weaponry 2.0 were imported once. Delete this file to import them again.\n", StandardCharsets.UTF_8);
        CombatReforged.LOGGER.info("[Combat Reforged] Imported the combat settings of Better Weaponry 2.0.");
    }

    private interface Filter {
        JsonObject apply(JsonObject old);
    }

    private static void importFile(Path source, Path target, Filter filter, String schema) throws IOException {
        if (!Files.isRegularFile(source) || Files.exists(target)) return;
        JsonElement element = JsonUtil.parseLenient(Files.readString(source, StandardCharsets.UTF_8));
        if (!element.isJsonObject()) return;
        JsonObject kept = filter.apply(element.getAsJsonObject());
        if (kept.size() > 0) ConfigFolder.writeJson(target, ConfigFolder.withSchema(kept, schema));
    }

    private static JsonObject keep(JsonObject old, Set<String> fields) {
        JsonObject kept = new JsonObject();
        old.entrySet().forEach(entry -> {
            if (fields.contains(entry.getKey())) kept.add(entry.getKey(), entry.getValue());
        });
        kept.remove(RulesDefinition.FORMAT.name());
        return kept;
    }

    private static JsonObject keepRules(JsonObject old) {
        return keep(old, RULE_FIELDS);
    }

    private static JsonObject keepGear(JsonObject old) {
        JsonObject kept = keep(old, GEAR_FIELDS);
        if (old.has(OLD_MOBS) && old.get(OLD_MOBS).isJsonObject() && old.getAsJsonObject(OLD_MOBS).has(OLD_GUARD_CHANCE)) {
            kept.add(GearDefinition.MOB_GUARD_CHANCE.name(), old.getAsJsonObject(OLD_MOBS).get(OLD_GUARD_CHANCE));
        }
        return kept;
    }

    /** La portée devient un bonus : celle d'une arme s'ajoute à la portée normale d'un joueur. */
    static JsonObject keepWeapon(JsonObject old) {
        JsonObject kept = keep(old, WEAPON_FIELDS);
        kept.remove(WeaponDefinition.ITEMS.name());
        if (old.has(OLD_REACH) && old.get(OLD_REACH).isJsonPrimitive() && old.get(OLD_REACH).getAsJsonPrimitive().isNumber()) {
            kept.addProperty(WeaponDefinition.REACH_BONUS.name(), old.get(OLD_REACH).getAsDouble() - PLAYER_BASE_REACH);
        }
        return kept;
    }

    /** Remplace dans un objet sauvegardé les anciens identifiants de Reflective, Riposte et Élan. Rien n'est copié si l'objet n'en porte aucun. */
    public static CompoundTag renamedEnchantments(CompoundTag stack) {
        if (!stack.contains("tag", Tag.TAG_COMPOUND)) return stack;
        CompoundTag nbt = stack.getCompound("tag");
        renameIn(nbt, "Enchantments");
        renameIn(nbt, "StoredEnchantments");
        return stack;
    }

    private static void renameIn(CompoundTag nbt, String key) {
        if (!nbt.contains(key, Tag.TAG_LIST)) return;
        ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String id = entry.getString("id");
            for (String name : List.of("reflective", "riposte", "momentum")) {
                if (id.equals(OLD_ENCHANTMENT_PREFIX + name)) entry.putString("id", CombatReforged.MODID + ":" + name);
            }
        }
    }

    /** Réglages d'une arme de Better Weaponry dans les datapacks d'un monde 2.0, à l'ancien chemin. */
    public static List<FieldSet.Layer> weaponLayers(ResourceManager resourceManager, ResourceLocation id, List<DataProblem> problems) {
        List<FieldSet.Layer> layers = new ArrayList<>();
        if (!id.getNamespace().equals(OLD_NAMESPACE)) return layers;
        String relative = WeaponDataLoader.WEAPONS_DIRECTORY + "/" + id.getPath() + ".json";
        ResourceLocation oldFile = new ResourceLocation(OLD_NAMESPACE, OLD_DATA_DIRECTORY + "/" + relative);
        for (Resource resource : resourceManager.getResourceStack(oldFile)) {
            if (!WeaponDataLoader.isWorldDatapack(resource)) continue;
            Optional<FieldSet.Layer> layer = WeaponDataLoader.readResource(resource, relative, problems);
            layer.ifPresent(found -> layers.add(new FieldSet.Layer(found.source(), keepWeapon(found.json()))));
        }
        return layers;
    }
}
