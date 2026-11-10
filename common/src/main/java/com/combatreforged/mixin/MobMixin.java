package com.combatreforged.mixin;

import com.combatreforged.combat.ICombatMob;
import com.combatreforged.combat.DualWieldSystem;
import com.combatreforged.combat.HandSwap;
import com.combatreforged.combat.MobCombat;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BooleanSupplier;

/** Combat des mobs : portée de leur arme et coups alternés en dual wield. */
@Mixin(Mob.class)
public abstract class MobMixin implements ICombatMob {
    @Unique private boolean combatreforged$offhandTurn;
    @Unique private long combatreforged$guardBrokenUntil;

    @Override
    public boolean combatreforged$offhandTurn() {
        return combatreforged$offhandTurn;
    }

    @Override
    public void combatreforged$setOffhandTurn(boolean offhand) {
        combatreforged$offhandTurn = offhand;
    }

    @Override
    public long combatreforged$guardBrokenUntil() {
        return combatreforged$guardBrokenUntil;
    }

    @Override
    public void combatreforged$setGuardBrokenUntil(long gameTime) {
        combatreforged$guardBrokenUntil = gameTime;
    }

    /** Le jeu compare des distances au carré : la portée de l'arme s'ajoute à la distance, pas à son carré. */
    @ModifyReturnValue(method = "getMeleeAttackRangeSqr", at = @At("RETURN"))
    private double combatreforged$weaponReach(double rangeSqr) {
        double extra = MobCombat.extraReach((Mob) (Object) this);
        if (extra == 0.0D) return rangeSqr;
        double range = Math.max(0.0D, Math.sqrt(rangeSqr) + extra);
        return range * range;
    }

    /**
     * Une fois sur deux, c'est l'arme secondaire qui frappe : mains échangées le temps du coup, bonus compris.
     */
    @WrapMethod(method = "doHurtTarget")
    private boolean combatreforged$alternateHands(Entity target, Operation<Boolean> original) {
        Mob mob = (Mob) (Object) this;
        BooleanSupplier attack = () -> original.call(target);
        if (!DualWieldSystem.isDualWielding(mob)) return attack.getAsBoolean();
        boolean offhand = MobCombat.isOffhandTurn(mob);
        MobCombat.setOffhandTurn(mob, !offhand);
        if (!offhand) return attack.getAsBoolean();
        HandSwap swap = HandSwap.begin(mob);
        try {
            return attack.getAsBoolean();
        } finally {
            swap.restore();
        }
    }
}
