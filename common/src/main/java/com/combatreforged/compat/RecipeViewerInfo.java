package com.combatreforged.compat;

import com.combatreforged.CombatReforged;
import com.combatreforged.combat.ModEnchantments;
import com.combatreforged.combat.WeaponProfiles;
import com.combatreforged.data.RulesDefinition;
import com.combatreforged.data.WeaponAbility;
import com.combatreforged.data.WeaponData;
import com.combatreforged.data.WeaponDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pages d'information de JEI et d'EMI : ce que font nos enchantements et les capacités de chaque arme.
 * Cette classe ne dépend d'aucun des deux mods : leurs plugins ne font qu'afficher ce qu'elle renvoie.
 */
public final class RecipeViewerInfo {
    private static final String KEY = CombatReforged.MODID + ".info.";

    private RecipeViewerInfo() {
    }

    /** Une page : les objets concernés et ses lignes. */
    public record Entry(String id, List<ItemStack> stacks, List<Component> lines) {
    }

    public static List<Entry> entries(RegistryAccess registries) {
        List<Entry> entries = new ArrayList<>();
        for (ResourceKey<Enchantment> key : ModEnchantments.ALL) {
            registries.registryOrThrow(Registries.ENCHANTMENT).getHolder(key).ifPresent(holder -> entries.add(enchantmentEntry(key, holder)));
        }
        RulesDefinition rules = WeaponData.rules();
        Map<ResourceLocation, List<Item>> byProfile = new TreeMap<>();
        WeaponProfiles.table().keySet().forEach(item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(item)
                .ifPresent(found -> WeaponProfiles.id(new ItemStack(found)).ifPresent(profile -> byProfile.computeIfAbsent(profile, ignored -> new ArrayList<>()).add(found))));
        byProfile.forEach((profile, items) -> {
            List<Component> lines = weaponLines(WeaponData.definition(profile), rules);
            if (!lines.isEmpty()) entries.add(new Entry(profile.getNamespace() + "_" + profile.getPath(), items.stream().map(ItemStack::new).toList(), lines));
        });
        return entries;
    }

    /** Un livre par niveau : JEI et EMI les listent séparément. */
    private static Entry enchantmentEntry(ResourceKey<Enchantment> key, Holder<Enchantment> enchantment) {
        List<ItemStack> books = new ArrayList<>();
        for (int level = enchantment.value().getMinLevel(); level <= enchantment.value().getMaxLevel(); level++) {
            books.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)));
        }
        return new Entry(key.location().getPath(), books, List.of(Component.translatable("enchantment." + CombatReforged.MODID + "." + key.location().getPath() + ".desc")));
    }

    /** Seulement ce qui est actif : une capacité coupée dans les règles (ou par Better Combat) n'est pas décrite. */
    private static List<Component> weaponLines(WeaponDefinition weapon, RulesDefinition rules) {
        List<Component> lines = new ArrayList<>();
        if (rules.twoHandedActive() && weapon.twoHanded()) lines.add(Component.translatable(KEY + "two_handed"));
        if (rules.dualWieldActive() && weapon.dualWielded()) lines.add(Component.translatable(KEY + "dual_wielded"));
        if (rules.parryActive() && weapon.ability() == WeaponAbility.PARRY) lines.add(Component.translatable(KEY + "parry"));
        if (rules.dashActive() && weapon.ability() == WeaponAbility.DASH) lines.add(Component.translatable(KEY + "dash"));
        if (rules.shieldDisablingActive() && weapon.disablesShield()) lines.add(Component.translatable(KEY + "disables_shield"));
        if (rules.combosActive() && weapon.combo().enabled()) lines.add(Component.translatable(KEY + "combo"));
        return lines;
    }
}
