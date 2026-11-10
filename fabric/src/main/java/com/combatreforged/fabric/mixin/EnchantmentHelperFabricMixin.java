package com.combatreforged.fabric.mixin;

import com.combatreforged.combat.ModEnchantment;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** À la table d'enchantement, les enchantements du mod suivent les mêmes règles qu'à l'enclume. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperFabricMixin {

    @WrapOperation(method = "getAvailableEnchantmentResults", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentCategory;canEnchant(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean combatreforged$modRules(EnchantmentCategory category, Item item, Operation<Boolean> original,
                                                   @Local Enchantment enchantment, @Local(argsOnly = true) ItemStack stack) {
        return enchantment instanceof ModEnchantment ? enchantment.canEnchant(stack) : original.call(category, item);
    }
}
