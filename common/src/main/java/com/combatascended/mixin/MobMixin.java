package com.combatascended.mixin;

import com.combatascended.combat.ICombatMob;
import com.combatascended.combat.DualWieldSystem;
import com.combatascended.combat.HandSwap;
import com.combatascended.combat.MobCombat;
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
    @Unique private boolean combatascended$offhandTurn;
    @Unique private long combatascended$guardBrokenUntil;

    @Override
    public boolean combatascended$offhandTurn() {
        return combatascended$offhandTurn;
    }

    @Override
    public void combatascended$setOffhandTurn(boolean offhand) {
        combatascended$offhandTurn = offhand;
    }

    @Override
    public long combatascended$guardBrokenUntil() {
        return combatascended$guardBrokenUntil;
    }

    @Override
    public void combatascended$setGuardBrokenUntil(long gameTime) {
        combatascended$guardBrokenUntil = gameTime;
    }

    /** Le jeu compare des distances au carré : la portée de l'arme s'ajoute à la distance, pas à son carré. */
    @ModifyReturnValue(method = "getMeleeAttackRangeSqr", at = @At("RETURN"))
    private double combatascended$weaponReach(double rangeSqr) {
        double extra = MobCombat.extraReach((Mob) (Object) this);
        if (extra == 0.0D) return rangeSqr;
        double range = Math.max(0.0D, Math.sqrt(rangeSqr) + extra);
        return range * range;
    }

    /**
     * Une fois sur deux, c'est l'arme secondaire qui frappe : mains échangées le temps du coup, bonus compris.
     */
    @WrapMethod(method = "doHurtTarget")
    private boolean combatascended$alternateHands(Entity target, Operation<Boolean> original) {
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
