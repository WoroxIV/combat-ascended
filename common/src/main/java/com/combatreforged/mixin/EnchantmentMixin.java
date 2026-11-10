package com.combatreforged.mixin;

import com.combatreforged.combat.ModEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reflective, Riposte et Élan suivent la capacité de l'objet : ce sont ses réglages qui décident, quel que soit son mod. */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

    @Inject(method = "isSupportedItem", at = @At("HEAD"), cancellable = true)
    private void combatreforged$followAbility(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        combatreforged$decide(stack, cir);
    }

    @Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
    private void combatreforged$followAbilityAtTable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        combatreforged$decide(stack, cir);
    }

    private void combatreforged$decide(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment) (Object) this;
        if (ModEnchantments.followsAbility(self)) ModEnchantments.supportedByAbility(stack, self).ifPresent(cir::setReturnValue);
    }
}
