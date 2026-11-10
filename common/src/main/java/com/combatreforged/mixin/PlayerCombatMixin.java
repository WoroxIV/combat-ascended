package com.combatreforged.mixin;

import com.combatreforged.combat.ComboSystem;
import com.combatreforged.combat.DualWieldSystem;
import com.combatreforged.combat.HandSwap;
import com.combatreforged.combat.ICombatPlayer;
import com.combatreforged.combat.ParrySystem;
import com.combatreforged.combat.PlayerCombatState;
import com.combatreforged.combat.WeaponProfiles;
import com.combatreforged.data.WeaponData;
import com.combatreforged.combat.WeaponActions;
import com.combatreforged.compat.CombatCompat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Donne à chaque joueur son état de combat ({@link PlayerCombatState}) et entoure ses attaques :
 * dual wield (la bonne main frappe) et combos (coups spéciaux).
 * Tourne côté client (prédiction) et côté serveur (dégâts réels) avec la même logique.
 */
@Mixin(Player.class)
public abstract class PlayerCombatMixin implements ICombatPlayer {

    @Unique private final PlayerCombatState combatreforged$combat = new PlayerCombatState();

    @Override
    public PlayerCombatState combatreforged$combat() {
        return combatreforged$combat;
    }

    /**
     * Entoure toute la méthode attack() :
     * <ul>
     *   <li>combo : l'étape est décidée avant le coup et le combo avance après ({@link ComboSystem}) ;</li>
     *   <li>coup spécial : toujours porté par l'arme principale, avec ses bonus ;</li>
     *   <li>sinon, si c'est au tour de la main secondaire, les deux mains sont échangées pendant l'attaque,
     *       puis remises en place dans un finally (même en cas de crash ou de retour anticipé).</li>
     * </ul>
     */
    @WrapMethod(method = "attack")
    private void combatreforged$attack(Entity target, Operation<Void> original) {
        Player player = (Player) (Object) this;
        PlayerCombatState state = combatreforged$combat;
        // Appel imbriqué pendant un échange de mains ou un coup spécial : comportement vanilla.
        if (state.activeSwap != null || state.activeStep != null) {
            original.call(target);
            return;
        }
        // Better Combat appelle attack() pour chaque cible d'une même frappe : une seule compte dans le combo.
        if (CombatCompat.betterCombat()) {
            long now = player.level().getGameTime();
            if (state.swingTick == now) {
                if (!state.swingVictims.contains(target.getId())) original.call(target);
                return;
            }
            state.swingTick = now;
            state.swingVictims.clear();
        }

        ComboSystem.Hit hit = ComboSystem.beginHit(player, state);
        state.attackLanded = false;
        boolean special = hit.special();
        boolean dualWielding = DualWieldSystem.isDualWielding(player);
        boolean offhandAttack = dualWielding && state.offhandTurn && !special;
        // Mémorisé pour que l'animation (swing) qui suit l'attaque montre la main qui a frappé.
        if (dualWielding) {
            state.justAttacked = true;
            state.lastAttackOffhand = offhandAttack;
            state.lastAttackSpecial = special;
        }

        try {
            if (special) {
                ComboSystem.performSpecialHit(player, target, hit, () -> original.call(target));
            } else if (offhandAttack) {
                state.activeSwap = HandSwap.begin(player);
                try {
                    original.call(target);
                } finally {
                    state.activeSwap.restore();
                    state.activeSwap = null;
                }
            } else {
                original.call(target);
            }
        } finally {
            ComboSystem.endHit(player, state, hit, target);
        }
    }

    /** Note si la cible principale a encaissé le coup. Le coup balayé passe par LivingEntity.hurt, il n'est donc pas compté ici. */
    @WrapOperation(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean combatreforged$rememberHit(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        boolean landed = original.call(target, source, amount);
        combatreforged$combat.attackLanded = landed;
        return landed;
    }

    /** Rayon du coup balayé d'un objet qui a un profil d'arme. */
    @WrapOperation(method = "attack", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getSweepHitBox(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/AABB;"))
    private AABB combatreforged$adoptedSweepZone(ItemStack stack, Player player, Entity target, Operation<AABB> original) {
        if (WeaponData.rules().sweepActive() && WeaponProfiles.isAdopted(stack)) {
            return WeaponActions.sweepZone(WeaponProfiles.definition(stack).orElseThrow(), target);
        }
        return original.call(stack, player, target);
    }

    /** Une parade parfaite au bouclier n'est jamais brisée par une hache. */
    @Inject(method = "disableShield", at = @At("HEAD"), cancellable = true)
    private void combatreforged$perfectParryKeepsShield(CallbackInfo ci) {
        if (ParrySystem.isInPerfectWindow((Player) (Object) this)) ci.cancel();
    }

    /**
     * Recharge : celle imposée par un coup spécial du combo, sinon en dual wield celle de l'arme qui va frapper,
     * divisée par le multiplicateur de la config. Sert à la force des coups (client et serveur) et à l'indicateur
     * de recharge du viseur.
     */
    @Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("HEAD"), cancellable = true)
    private void combatreforged$attackDelay(CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        Float comboDelay = ComboSystem.attackDelayOverride(player, combatreforged$combat);
        if (comboDelay != null) {
            cir.setReturnValue(comboDelay);
        } else if (DualWieldSystem.isDualWielding(player)) {
            cir.setReturnValue(DualWieldSystem.attackStrengthDelay(player, combatreforged$combat));
        }
    }
}
