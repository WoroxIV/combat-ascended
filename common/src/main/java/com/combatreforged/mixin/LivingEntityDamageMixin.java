package com.combatreforged.mixin;

import com.combatreforged.combat.CombatEvents;
import com.combatreforged.combat.ParrySystem;
import com.combatreforged.combat.ShieldSystem;
import com.combatreforged.platform.IncomingDamage;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** La 1.20.1 n'a pas d'événement qui modifie un coup avant le bouclier : le mod se branche directement dans {@code hurt}. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Unique private static final int NO_OVERRIDE = -1;

    @Unique private float combatreforged$armorKept = 1.0F;
    @Unique private int combatreforged$invulnerabilityTicks = NO_OVERRIDE;

    @Inject(method = "hurt", cancellable = true,
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noActionTime:I", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void combatreforged$incomingDamage(DamageSource source, float originalAmount, CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalFloatRef amount) {
        combatreforged$armorKept = 1.0F;
        combatreforged$invulnerabilityTicks = NO_OVERRIDE;
        LivingEntity self = (LivingEntity) (Object) this;
        float[] current = {amount.get()};
        boolean[] canceled = {false};
        IncomingDamage damage = new IncomingDamage() {
            @Override
            public LivingEntity target() {
                return self;
            }

            @Override
            public DamageSource source() {
                return source;
            }

            @Override
            public float amount() {
                return current[0];
            }

            @Override
            public void setAmount(float value) {
                current[0] = value;
            }

            @Override
            public void cancel() {
                canceled[0] = true;
            }

            @Override
            public boolean isCanceled() {
                return canceled[0];
            }

            @Override
            public void scaleArmorReduction(float kept) {
                combatreforged$armorKept *= kept;
            }

            @Override
            public void setPostAttackInvulnerabilityTicks(int ticks) {
                combatreforged$invulnerabilityTicks = ticks;
            }
        };
        ParrySystem.onHitWithPerfectBonus(damage);
        CombatEvents.onIncomingDamage(damage);
        if (canceled[0]) {
            cir.setReturnValue(false);
            return;
        }
        amount.set(current[0]);
    }

    /** Pénétration d'armure : l'armure retire moins de dégâts. */
    @WrapMethod(method = "getDamageAfterArmorAbsorb")
    private float combatreforged$armorPenetration(DamageSource source, float amount, Operation<Float> original) {
        float result = original.call(source, amount);
        float kept = combatreforged$armorKept;
        combatreforged$armorKept = 1.0F;
        return kept >= 1.0F ? result : amount - (amount - result) * kept;
    }

    @WrapOperation(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I", opcode = Opcodes.PUTFIELD))
    private void combatreforged$postAttackInvulnerability(LivingEntity self, int ticks, Operation<Void> original) {
        original.call(self, combatreforged$invulnerabilityTicks != NO_OVERRIDE ? combatreforged$invulnerabilityTicks : ticks);
    }

    /** Coup arrêté par un bouclier : une parade parfaite ne l'use pas. */
    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"))
    private void combatreforged$shieldBlock(LivingEntity self, float damage, Operation<Void> original, @Local(argsOnly = true) DamageSource source) {
        if (!ShieldSystem.onShieldBlock(self, source, damage)) original.call(self, damage);
    }
}
