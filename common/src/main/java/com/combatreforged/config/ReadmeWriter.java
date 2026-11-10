package com.combatreforged.config;

import com.combatreforged.CombatReforged;
import com.combatreforged.data.DataField;
import com.combatreforged.data.FieldSet;
import com.combatreforged.data.GearDefinition;
import com.combatreforged.data.RulesDefinition;
import com.combatreforged.data.WeaponDefinition;

/**
 * Écrit le README.txt du dossier de config, dans la langue du jeu.
 * La liste des champs est générée à partir des déclarations ({@link DataField}) : elle est toujours à jour.
 */
final class ReadmeWriter {
    private static final String KEY = CombatReforged.MODID + ".doc.readme.";
    private static final String INDENT = "  ";
    private static final String DETAIL_INDENT = "      ";

    private ReadmeWriter() {
    }

    static String write(Docs docs) {
        StringBuilder out = new StringBuilder();
        String title = docs.text(KEY + "title");
        out.append(title).append('\n').append("=".repeat(title.length())).append("\n\n");

        for (String section : new String[]{"intro", "folders", "example", "priority", "errors"}) {
            out.append(docs.text(KEY + section)).append("\n\n");
        }

        appendFields(out, docs, WeaponDefinition.FIELDS, KEY + "weapon_fields");
        appendFields(out, docs, RulesDefinition.FIELDS, KEY + "rules_fields");
        appendFields(out, docs, GearDefinition.FIELDS, KEY + "gear_fields");
        return out.toString();
    }

    private static void appendFields(StringBuilder out, Docs docs, FieldSet set, String titleKey) {
        out.append(docs.text(titleKey)).append('\n');
        appendFieldList(out, docs, set, "");
    }

    /** Liste les champs, puis les sous-champs avec leur chemin (ex : "sweep.radius", "on_hit_effects[].level"). */
    private static void appendFieldList(StringBuilder out, Docs docs, FieldSet set, String prefix) {
        for (DataField<?> field : set.fields()) {
            out.append(INDENT).append(prefix).append(field.name()).append(" — ").append(docs.fieldName(set, field)).append('\n');
            out.append(DETAIL_INDENT).append(docs.fieldHelp(set, field)).append('\n');
            String limits = docs.fieldLimits(field);
            if (!limits.isEmpty()) out.append(DETAIL_INDENT).append(limits).append('\n');
            out.append('\n');

            FieldSet children = field.children();
            if (children != null) {
                String childPrefix = prefix + field.name() + (field.kind() == DataField.Kind.OBJECT_LIST ? "[]." : ".");
                appendFieldList(out, docs, children, childPrefix);
            }
        }
    }
}
