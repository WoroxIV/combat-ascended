package com.combatascended.mixin;

import com.combatascended.combat.ModEnchantments;
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
    private void combatascended$followAbility(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        combatascended$decide(stack, cir);
    }

    @Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
    private void combatascended$followAbilityAtTable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        combatascended$decide(stack, cir);
    }

    private void combatascended$decide(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment) (Object) this;
        if (ModEnchantments.followsAbility(self)) ModEnchantments.supportedByAbility(stack, self).ifPresent(cir::setReturnValue);
    }
}
