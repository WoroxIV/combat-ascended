package com.combatreforged.gametest;

import com.combatreforged.data.DataResults;
import com.combatreforged.combat.WeaponProfiles;
import com.combatreforged.data.DataField;
import com.combatreforged.data.GearDefinition;
import com.combatreforged.data.JsonUtil;
import com.combatreforged.data.RulesDefinition;
import com.combatreforged.data.WeaponData;
import com.combatreforged.data.WeaponDefinition;
import com.combatreforged.platform.Platform;
import com.combatreforged.registry.ModAttributes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Compare le comportement des armes de Better Weaponry, chargées avec ce mod, à celui mesuré sur la version 2.0.1. */
final class ParitySnapshot {
    private static final double PLAYER_BASE_DAMAGE = 1.0D;
    private static final double PLAYER_BASE_SPEED = 4.0D;
    private static final double PLAYER_BASE_REACH = 3.0D;
    private static final double PERCENT = 100.0D;
    /** Champs nouveaux, absents du fichier de référence. */
    private static final List<String> NEW_FIELDS = List.of("format_version", "parent", "reach_bonus", "exact_reach", "items");

    private ParitySnapshot() {
    }

    /** @return les différences avec le fichier de référence, vide si tout est identique */
    static List<String> compareWithReference(String referenceJson) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject weapons = new JsonObject();
        List<ResourceLocation> ids = new ArrayList<>(BuiltInRegistries.ITEM.keySet());
        for (ResourceLocation id : ids) {
            if (!id.getNamespace().equals("better_weaponry")) continue;
            if (!id.getPath().matches("(wooden|stone|iron|golden|diamond|netherite)_(broadsword|spear|dagger|scythe|rapier|sickle|katana|battleaxe|glaive|hammer|claymore|chakram|sai)")) continue;
            weapons.add(id.toString(), describe(BuiltInRegistries.ITEM.get(id)));
        }
        root.add("weapons", weapons);
        JsonObject reference = JsonUtil.parseLenient(referenceJson).getAsJsonObject();
        JsonElement actual = sorted(root.get("weapons"));
        JsonElement expected = sorted(reference.get("weapons"));
        Path output = Platform.get().gameDir().resolve("parity-actual.json");
        Files.writeString(output, JsonUtil.PRETTY.toJson(actual) + "\n", StandardCharsets.UTF_8);
        List<String> differences = new ArrayList<>();
        diff("", expected, actual, differences);
        JsonObject expectedRules = reference.getAsJsonObject("rules");
        JsonObject actualRules = DataResults.getOrThrow(RulesDefinition.CODEC.encodeStart(JsonOps.INSTANCE, WeaponData.rules())).getAsJsonObject();
        expectedRules.remove("balance_abilities");
        diff("rules", sorted(expectedRules), sorted(actualRules), differences);
        JsonObject expectedGear = reference.getAsJsonObject("gear");
        JsonObject actualGear = DataResults.getOrThrow(GearDefinition.CODEC.encodeStart(JsonOps.INSTANCE, WeaponData.gear())).getAsJsonObject();
        for (String key : expectedGear.keySet().toArray(String[]::new)) {
            if (!actualGear.has(key)) expectedGear.remove(key);
        }
        actualGear.remove("mob_guard_chance");
        diff("gear", sorted(expectedGear), sorted(actualGear), differences);
        return differences;
    }

    private static JsonObject describe(Item item) {
        ItemStack stack = new ItemStack(item);
        JsonObject line = new JsonObject();
        line.addProperty("final_attack_damage", PLAYER_BASE_DAMAGE + sum(stack, Attributes.ATTACK_DAMAGE));
        line.addProperty("final_attack_speed", PLAYER_BASE_SPEED + sum(stack, Attributes.ATTACK_SPEED));
        line.addProperty("final_reach", PLAYER_BASE_REACH + sum(stack, Platform.get().entityReachAttribute()));
        line.addProperty("final_armor_penetration", sum(stack, ModAttributes.armorPenetration()) * PERCENT);
        line.addProperty("final_sweep_ratio", sum(stack, ModAttributes.SWEEPING_DAMAGE_RATIO.get()) * PERCENT);
        WeaponDefinition definition = WeaponProfiles.definition(stack).orElseThrow(() -> new IllegalStateException("no profile for " + item));
        JsonObject encoded = DataResults.getOrThrow(WeaponDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)).getAsJsonObject();
        NEW_FIELDS.forEach(encoded::remove);
        JsonObject combo = encoded.getAsJsonObject("combo");
        if (combo != null) combo.remove("max_bonus_damage");
        encoded.entrySet().forEach(entry -> line.add(entry.getKey(), entry.getValue()));
        return line;
    }

    private static double sum(ItemStack stack, Attribute attribute) {
        double total = 0.0D;
        for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute)) total += modifier.getAmount();
        return total;
    }

    private static void diff(String path, JsonElement expected, JsonElement actual, List<String> out) {
        if (expected == null || actual == null || expected.isJsonNull() || actual.isJsonNull()) {
            if (expected != actual) out.add(path + ": " + expected + " != " + actual);
            return;
        }
        if (expected.isJsonObject() && actual.isJsonObject()) {
            JsonObject a = expected.getAsJsonObject();
            JsonObject b = actual.getAsJsonObject();
            java.util.Set<String> keys = new java.util.TreeSet<>(a.keySet());
            keys.addAll(b.keySet());
            for (String key : keys) diff(path + "/" + key, a.get(key), b.get(key), out);
        } else if (expected.isJsonPrimitive() && actual.isJsonPrimitive() && expected.getAsJsonPrimitive().isNumber() && actual.getAsJsonPrimitive().isNumber()) {
            if (Math.abs(expected.getAsDouble() - actual.getAsDouble()) > 1.0E-6D) out.add(path + ": " + expected + " != " + actual);
        } else if (!expected.equals(actual)) {
            out.add(path + ": " + expected + " != " + actual);
        }
    }

    private static JsonElement sorted(JsonElement element) {
        if (element.isJsonObject()) {
            Map<String, JsonElement> entries = new TreeMap<>();
            element.getAsJsonObject().entrySet().forEach(entry -> entries.put(entry.getKey(), sorted(entry.getValue())));
            JsonObject result = new JsonObject();
            entries.forEach(result::add);
            return result;
        }
        if (element.isJsonArray()) {
            JsonArray result = new JsonArray();
            element.getAsJsonArray().forEach(child -> result.add(sorted(child)));
            return result;
        }
        return element;
    }
}
