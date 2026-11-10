package com.combatreforged.mixin;

import com.combatreforged.combat.ParrySystem;
import com.combatreforged.combat.WeaponProfiles;
import com.combatreforged.combat.WeaponActions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Comportements d'arme des objets adoptés par un fichier d'arme (champ "items") : ce que
 * une arme pourrait faire elle-même, sans pouvoir modifier la classe d'un objet vanilla.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    /** Pose "bloquer" pendant la parade. */
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void combatreforged$parryAnimation(CallbackInfoReturnable<UseAnim> cir) {
        if (combatreforged$adoptedParry()) cir.setReturnValue(UseAnim.BLOCK);
    }

    /** La parade dure tant que le clic droit est maintenu. */
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void combatreforged$parryDuration(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (combatreforged$adoptedParry()) cir.setReturnValue(ParrySystem.USE_DURATION);
    }

    /** Effets à l'impact, appliqués au moment où le jeu compte le coup sur l'objet (comme le ferait la méthode hurtEnemy de l'arme). */
    @Inject(method = "hurtEnemy", at = @At("HEAD"))
    private void combatreforged$onHitEffects(LivingEntity target, Player attacker, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (WeaponProfiles.isAdopted(stack)) WeaponActions.applyOnHitEffects(stack, target, attacker);
    }

    private boolean combatreforged$adoptedParry() {
        ItemStack stack = (ItemStack) (Object) this;
        return WeaponProfiles.isAdopted(stack) && ParrySystem.parryOf(stack).isPresent();
    }
}
