package com.combatascended.mixin;

import com.combatascended.combat.ComboSystem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vue première personne d'un coup à deux armes (combo) : le vanilla n'anime que la main qui frappe ;
 * on donne la même progression de coup aux deux mains, qui frappent donc ensemble.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class DualSwingFirstPersonMixin {

    /** Vue première personne d'un geste de combo : remplace le mouvement vanilla de l'arme pendant le coup. */
    @org.spongepowered.asm.mixin.injection.Inject(method = "applyItemArmAttackTransform", at = @At("HEAD"), cancellable = true)
    private void combatascended$gesture(PoseStack pose, net.minecraft.world.entity.HumanoidArm arm, float swingProgress, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;
        com.combatascended.data.ComboGesture gesture = ComboSystem.gestureOf(player);
        if (gesture == com.combatascended.data.ComboGesture.DEFAULT) return;
        float side = arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float swing = net.minecraft.util.Mth.sin(swingProgress * net.minecraft.util.Mth.PI);
        switch (gesture) {
            case THRUST -> pose.translate(0.0F, 0.1F * swing, -0.55F * swing);
            case OVERHEAD -> {
                pose.translate(0.0F, 0.25F * swing, -0.1F * swing);
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-85.0F * net.minecraft.util.Mth.cos(swingProgress * net.minecraft.util.Mth.PI)));
            }
            case SLASH -> pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side * (70.0F - 140.0F * swingProgress)));
            default -> {
            }
        }
        ci.cancel();
    }

    @WrapOperation(
            method = "renderHandsWithItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    )
    private void combatascended$swingBothHands(ItemInHandRenderer renderer, AbstractClientPlayer player, float partialTick,
                                               float pitch, InteractionHand hand, float swingProgress, ItemStack stack,
                                               float equipProgress, PoseStack poseStack, MultiBufferSource buffer,
                                               int packedLight, Operation<Void> original) {
        float swing = ComboSystem.isDualSwinging(player) ? player.getAttackAnim(partialTick) : swingProgress;
        original.call(renderer, player, partialTick, pitch, hand, swing, stack, equipProgress, poseStack, buffer, packedLight);
    }
}
