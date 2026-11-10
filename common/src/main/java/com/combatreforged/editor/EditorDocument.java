package com.combatreforged.editor;

import com.combatreforged.data.DataResults;
import com.combatreforged.CombatReforged;
import com.combatreforged.config.ConfigFolder;
import com.combatreforged.data.FieldSet;
import com.combatreforged.data.GearDefinition;
import com.combatreforged.data.JsonTree;
import com.combatreforged.data.RulesDefinition;
import com.combatreforged.data.WeaponData;
import com.combatreforged.data.WeaponDataLoader;
import com.combatreforged.data.WeaponDefinition;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un fichier modifiable dans l'éditeur : un archétype, un fichier d'arme, les règles ou la parade au bouclier.
 *
 * @param id           identifiant envoyé dans les paquets : "rules", "gear", "archetype/mod:nom" ou "weapon/mod:nom"
 * @param weaponId     identifiant de l'archétype ou du fichier d'arme, null pour les règles et la parade au bouclier
 * @param directory    "archetypes" ou "weapons", null pour les règles et la parade au bouclier
 * @param fields       champs du fichier
 * @param namespace    mod qui porte ce fichier dans les datapacks
 * @param relativePath chemin dans le dossier des données, ex : "weapons/katana.json"
 * @param schemaPath   lien vers le schéma, relatif au fichier dans config/combat_reforged
 */
public record EditorDocument(String id, @Nullable ResourceLocation weaponId, @Nullable String directory, FieldSet fields, String namespace, String relativePath, String schemaPath) {
    public static final String RULES_ID = "rules";
    public static final String GEAR_ID = "gear";
    private static final String ARCHETYPE_PREFIX = "archetype/";
    private static final String WEAPON_PREFIX = "weapon/";

    private static EditorDocument definition(String prefix, String directory, ResourceLocation id) {
        return new EditorDocument(prefix + id, id, directory, WeaponDefinition.FIELDS, id.getNamespace(), directory + "/" + id.getPath() + ".json", ConfigFolder.WEAPON_SCHEMA_FROM_DATA);
    }

    public static EditorDocument archetype(ResourceLocation id) {
        return definition(ARCHETYPE_PREFIX, WeaponDataLoader.ARCHETYPES_DIRECTORY, id);
    }

    public static EditorDocument weapon(ResourceLocation id) {
        return definition(WEAPON_PREFIX, WeaponDataLoader.WEAPONS_DIRECTORY, id);
    }

    public static EditorDocument rules() {
        return new EditorDocument(RULES_ID, null, null, RulesDefinition.FIELDS, CombatReforged.MODID, WeaponDataLoader.RULES_FILE, "./" + ConfigFolder.RULES_SCHEMA);
    }

    public static EditorDocument gear() {
        return new EditorDocument(GEAR_ID, null, null, GearDefinition.FIELDS, CombatReforged.MODID, WeaponDataLoader.GEAR_FILE, "./" + ConfigFolder.GEAR_SCHEMA);
    }

    /** Les règles, la parade au bouclier, les fichiers d'armes puis les archétypes. */
    public static List<EditorDocument> all() {
        List<EditorDocument> documents = new ArrayList<>();
        documents.add(rules());
        documents.add(gear());
        WeaponData.Snapshot data = WeaponData.current();
        data.weapons().keySet().stream().sorted().forEach(id -> documents.add(weapon(id)));
        data.archetypes().keySet().stream().sorted().forEach(id -> documents.add(archetype(id)));
        return documents;
    }

    public static Optional<EditorDocument> byId(String id) {
        if (RULES_ID.equals(id)) return Optional.of(rules());
        if (GEAR_ID.equals(id)) return Optional.of(gear());
        if (id.startsWith(ARCHETYPE_PREFIX)) return Optional.ofNullable(ResourceLocation.tryParse(id.substring(ARCHETYPE_PREFIX.length()))).map(EditorDocument::archetype);
        if (id.startsWith(WEAPON_PREFIX)) return Optional.ofNullable(ResourceLocation.tryParse(id.substring(WEAPON_PREFIX.length()))).map(EditorDocument::weapon);
        return Optional.empty();
    }

    public boolean isWeapon() {
        return weaponId != null;
    }

    public boolean isArchetype() {
        return WeaponDataLoader.ARCHETYPES_DIRECTORY.equals(directory);
    }

    /** Fichier de l'utilisateur dans config/combat_reforged. */
    public Path configFile() {
        if (weaponId != null) return ConfigFolder.dataFile(directory, weaponId);
        return RULES_ID.equals(id) ? ConfigFolder.rulesFile() : ConfigFolder.gearFile();
    }

    /** Valeurs complètes de ce fichier dans ces données (ex : celles en vigueur, ou celles du jar). */
    public JsonObject json(WeaponData.Snapshot snapshot) {
        JsonElement encoded;
        if (weaponId != null) encoded = DataResults.getOrThrow(WeaponDefinition.CODEC.encodeStart(JsonOps.INSTANCE, snapshot.definition(weaponId)));
        else if (RULES_ID.equals(id)) encoded = DataResults.getOrThrow(RulesDefinition.CODEC.encodeStart(JsonOps.INSTANCE, snapshot.rules()));
        else encoded = DataResults.getOrThrow(GearDefinition.CODEC.encodeStart(JsonOps.INSTANCE, snapshot.gear()));
        return JsonTree.expand(fields, encoded.getAsJsonObject());
    }

    /** Valeurs par défaut du mod (fichiers du jar). */
    public JsonObject defaults() {
        return json(WeaponData.builtin());
    }
}
