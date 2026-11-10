package com.combatreforged.editor;

import com.combatreforged.data.DataResults;
import com.combatreforged.config.ConfigFolder;
import com.combatreforged.data.FieldSet;
import com.combatreforged.data.GearDefinition;
import com.combatreforged.data.JsonTree;
import com.combatreforged.data.RulesDefinition;
import com.combatreforged.data.WeaponData;
import com.combatreforged.data.WeaponDataLoader;
import com.combatreforged.data.WeaponDefinition;
import com.combatreforged.item.WeaponType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un fichier modifiable dans l'éditeur : une arme ({@code weapons/<type>.json}), les règles ({@code rules.json})
 * ou l'équipement ({@code gear.json}).
 *
 * @param id         identifiant envoyé dans les paquets : l'id du type d'arme, "rules" ou "gear"
 * @param type       type d'arme, null pour les règles et l'équipement
 * @param fields     champs du fichier
 * @param schemaPath lien vers le schéma, relatif au fichier dans config/combat_reforged
 */
public record EditorDocument(String id, @Nullable WeaponType type, FieldSet fields, String relativePath, String schemaPath) {
    public static final String RULES_ID = "rules";
    public static final String GEAR_ID = "gear";

    public static EditorDocument weapon(WeaponType type) {
        return new EditorDocument(type.id(), type, WeaponDefinition.FIELDS, WeaponDataLoader.weaponPath(type),
                "../" + ConfigFolder.WEAPON_SCHEMA);
    }

    public static EditorDocument rules() {
        return new EditorDocument(RULES_ID, null, RulesDefinition.FIELDS, WeaponDataLoader.RULES_FILE, "./" + ConfigFolder.RULES_SCHEMA);
    }

    public static EditorDocument gear() {
        return new EditorDocument(GEAR_ID, null, GearDefinition.FIELDS, WeaponDataLoader.GEAR_FILE, "./" + ConfigFolder.GEAR_SCHEMA);
    }

    /** Les règles, l'équipement puis les armes, dans l'ordre de l'onglet créatif. */
    public static List<EditorDocument> all() {
        List<EditorDocument> documents = new ArrayList<>();
        documents.add(rules());
        documents.add(gear());
        for (WeaponType type : WeaponType.values()) documents.add(weapon(type));
        return documents;
    }

    public static Optional<EditorDocument> byId(String id) {
        if (RULES_ID.equals(id)) return Optional.of(rules());
        if (GEAR_ID.equals(id)) return Optional.of(gear());
        return Optional.ofNullable(WeaponType.byId(id)).map(EditorDocument::weapon);
    }

    /** Fichier de l'utilisateur dans config/combat_reforged. */
    public Path configFile() {
        if (type != null) return ConfigFolder.weaponFile(type);
        return RULES_ID.equals(id) ? ConfigFolder.rulesFile() : ConfigFolder.gearFile();
    }

    /** Valeurs complètes de ce fichier dans ces données (ex : celles en vigueur, ou celles du jar). */
    public JsonObject json(WeaponData.Snapshot snapshot) {
        JsonElement encoded;
        if (type != null) encoded = DataResults.orThrow(WeaponDefinition.CODEC.encodeStart(JsonOps.INSTANCE, snapshot.weapon(type)));
        else if (RULES_ID.equals(id)) encoded = DataResults.orThrow(RulesDefinition.CODEC.encodeStart(JsonOps.INSTANCE, snapshot.rules()));
        else encoded = DataResults.orThrow(GearDefinition.CODEC.encodeStart(JsonOps.INSTANCE, snapshot.gear()));
        return JsonTree.expand(fields, encoded.getAsJsonObject());
    }

    /** Valeurs par défaut du mod (fichiers du jar). */
    public JsonObject defaults() {
        return json(WeaponData.builtin());
    }
}
