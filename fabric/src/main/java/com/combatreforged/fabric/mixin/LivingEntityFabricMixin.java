package com.combatreforged.fabric.mixin;

import com.combatreforged.combat.CombatEvents;
import com.combatreforged.combat.ParrySystem;
import com.combatreforged.combat.ShieldSystem;
import com.combatreforged.platform.IncomingDamage;
import com.combatreforged.platform.Platform;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric n'a pas d'événement pour modifier un coup : on se branche aux mêmes endroits que NeoForge. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFabricMixin {
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

    /** L'armure retire moins de dégâts : même calcul que la pénétration d'armure sur NeoForge. */
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

    /** Pour le jeu de base, tout objet en pose "bloquer" est un bouclier : une arme qui pare réduit les coups, elle ne les arrête pas. */
    @ModifyReturnValue(method = "isBlocking", at = @At("RETURN"))
    private boolean combatreforged$onlyShieldsBlock(boolean blocking) {
        return blocking && Platform.get().isShield(((LivingEntity) (Object) this).getUseItem());
    }

    @Inject(method = "startUsingItem", at = @At("HEAD"))
    private void combatreforged$startUsingItem(InteractionHand hand, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack stack = self.getItemInHand(hand);
        if (!stack.isEmpty() && !self.isUsingItem()) ShieldSystem.onStartUsingItem(self, stack);
    }

}
