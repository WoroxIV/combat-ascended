package com.combatascended.mixin;

import com.combatascended.combat.ComboSystem;
import com.combatascended.data.ComboGesture;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vue troisième personne d'un coup à deux armes (combo) : au lieu d'un bras qui frappe en tournant le buste,
 * les deux bras s'abattent ensemble, face à l'ennemi. Mêmes courbes que l'animation vanilla d'un bras.
 */
@Mixin(HumanoidModel.class)
public abstract class DualSwingModelMixin {

    /** Vue troisième personne d'un geste de combo : estoc droit devant, coup vertical, ou taille de côté. */
    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true)
    private void combatascended$gesture(LivingEntity entity, float ageInTicks, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        float time = model.attackTime;
        if (time <= 0.0F || !(entity instanceof Player player) || ComboSystem.isDualSwinging(player)) return;
        ComboGesture gesture = ComboSystem.gestureOf(player);
        if (gesture == ComboGesture.DEFAULT) return;
        boolean right = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        net.minecraft.client.model.geom.ModelPart arm = right ? model.rightArm : model.leftArm;
        float swing = Mth.sin(time * Mth.PI);
        switch (gesture) {
            case THRUST -> arm.xRot = -Mth.HALF_PI - swing * 0.35F;
            case OVERHEAD -> arm.xRot -= swing * 2.6F;
            case SLASH -> {
                arm.xRot = -Mth.HALF_PI * 0.9F;
                arm.yRot = (right ? -1.0F : 1.0F) * (1.0F - 2.0F * time) * 1.1F;
            }
            default -> {
            }
        }
        ci.cancel();
    }

    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true)
    private void combatascended$swingBothArms(LivingEntity entity, float ageInTicks, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        float time = model.attackTime;
        if (time <= 0.0F || !(entity instanceof Player player) || !ComboSystem.isDualSwinging(player)) return;

        // Courbe vanilla : montée rapide puis retour (HumanoidModel.setupAttackAnimation).
        float eased = 1.0F - time;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        float lift = Mth.sin(eased * Mth.PI) * 1.2F + Mth.sin(time * Mth.PI) * -(model.head.xRot - 0.7F) * 0.75F;
        float roll = Mth.sin(time * Mth.PI) * 0.4F;
        model.rightArm.xRot -= lift;
        model.leftArm.xRot -= lift;
        model.rightArm.zRot -= roll;
        model.leftArm.zRot += roll;
        ci.cancel();
    }
}
