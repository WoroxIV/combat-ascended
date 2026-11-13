package com.combatascended.fabric.mixin;

import com.combatascended.combat.ShieldSystem;
import com.combatascended.platform.Platform;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ce que Forge modifie dans les créatures, et que Fabric laisse tel quel. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFabricMixin {

    /** Pour le jeu de base, tout objet en pose "bloquer" est un bouclier : une arme qui pare réduit les coups, elle ne les arrête pas. */
    @ModifyReturnValue(method = "isBlocking", at = @At("RETURN"))
    private boolean combatascended$onlyShieldsBlock(boolean blocking) {
        return blocking && Platform.get().isShield(((LivingEntity) (Object) this).getUseItem());
    }

    @Inject(method = "startUsingItem", at = @At("HEAD"))
    private void combatascended$startUsingItem(InteractionHand hand, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack stack = self.getItemInHand(hand);
        if (!stack.isEmpty() && !self.isUsingItem()) ShieldSystem.onStartUsingItem(self, stack);
    }
}
