package com.combatreforged.fabric;

import com.combatreforged.CombatReforged;
import com.combatreforged.item.LootBooks;
import com.combatreforged.platform.Platform;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Livres enchantés dans les coffres. Lit les mêmes fichiers que NeoForge (data/combat_reforged/loot_modifiers/) : la liste
 * des coffres et des enchantements reste écrite à un seul endroit.
 */
public final class FabricLootBooks {
    private static final List<String> FILES = List.of("nether_enchanted_books.json", "end_enchanted_books.json");
    private static Map<ResourceLocation, List<ResourceKey<Enchantment>>> byLootTable;

    private FabricLootBooks() {
    }

    public static void maybeAdd(ResourceLocation lootTable, List<ItemStack> loot, LootContext context) {
        if (byLootTable == null) byLootTable = read();
        List<ResourceKey<Enchantment>> enchantments = byLootTable.get(lootTable);
        if (enchantments != null) LootBooks.maybeAdd(loot, context, enchantments);
    }

    private static Map<ResourceLocation, List<ResourceKey<Enchantment>>> read() {
        Map<ResourceLocation, List<ResourceKey<Enchantment>>> result = new HashMap<>();
        for (String file : FILES) {
            Platform.get().readModFile("data", CombatReforged.MODID, "loot_modifiers", file).ifPresent(text -> {
                JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                List<ResourceKey<Enchantment>> enchantments = new ArrayList<>();
                for (JsonElement id : json.getAsJsonArray("enchantments")) {
                    enchantments.add(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(id.getAsString())));
                }
                for (JsonElement condition : json.getAsJsonArray("conditions")) {
                    for (JsonElement term : condition.getAsJsonObject().getAsJsonArray("terms")) {
                        result.put(ResourceLocation.parse(term.getAsJsonObject().get("loot_table_id").getAsString()), List.copyOf(enchantments));
                    }
                }
            });
        }
        return result;
    }
}
