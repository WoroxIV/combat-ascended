package com.combatreforged.fabric.mixin;

import com.combatreforged.client.DualParryHandRenderer;
import com.combatreforged.combat.TwoHandedSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Première personne : main secondaire cachée avec une arme à deux mains, pose de parade. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererFabricMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void combatreforged$customHands(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack,
                                            float equippedProgress, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (hand == InteractionHand.OFF_HAND && TwoHandedSystem.isOffhandLocked(player)) {
            ci.cancel();
        } else if (DualParryHandRenderer.render(hand, stack, pose, buffers, light, equippedProgress, true)) {
            ci.cancel();
        }
    }

}
