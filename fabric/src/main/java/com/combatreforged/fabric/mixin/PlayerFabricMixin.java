package com.combatreforged.fabric.mixin;

import com.combatreforged.combat.CombatEvents;
import com.combatreforged.combat.TwoHandedSystem;
import com.combatreforged.combat.WeaponProfiles;
import com.combatreforged.data.WeaponData;
import com.combatreforged.combat.WeaponActions;
import com.combatreforged.platform.Platform;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ce que NeoForge modifie dans le joueur, et que Fabric laisse tel quel. */
@Mixin(Player.class)
public abstract class PlayerFabricMixin {

    /** Après l'armure : plafond des dégâts entre joueurs. */
    @ModifyExpressionValue(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float combatreforged$capPvpHit(float damage, @Local(argsOnly = true) DamageSource source) {
        return CombatEvents.capPvpHit((Player) (Object) this, source, damage);
    }

    /** Armes sans coup balayé, et coup spécial en zone qui remplace le balayage. */
    @WrapOperation(method = "attack", constant = @Constant(classValue = SwordItem.class))
    private boolean combatreforged$mayNotSweep(Object item, Operation<Boolean> original) {
        return original.call(item) && !CombatEvents.preventsSweep((Player) (Object) this);
    }

    /** Zone touchée par le coup balayé autour de la cible. */
    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
    private AABB combatreforged$sweepZone(AABB box, double x, double y, double z, Operation<AABB> original, @Local(argsOnly = true) Entity target) {
        Player player = (Player) (Object) this;
        ItemStack stack = player.getMainHandItem();
        if (WeaponData.rules().sweepActive() && WeaponProfiles.isAdopted(stack)) return WeaponActions.sweepZone(WeaponProfiles.definition(stack).orElseThrow(), target);
        return original.call(box, x, y, z);
    }

    /** Le jeu de base met toujours en recharge le bouclier vanilla : ici, celui qui est tenu. */
    @WrapOperation(method = "disableShield", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V"))
    private void combatreforged$disableHeldShield(ItemCooldowns cooldowns, Item item, int ticks, Operation<Void> original) {
        ItemStack used = ((Player) (Object) this).getUseItem();
        original.call(cooldowns, Platform.get().isShield(used) ? used.getItem() : item, ticks);
    }

    /** Le jeu de base n'use que le bouclier vanilla. */
    @WrapOperation(method = "hurtCurrentlyUsedShield", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean combatreforged$wearAnyShield(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || Platform.get().isShield(stack);
    }

    /** Une utilisation commencée avant de prendre l'arme à deux mains : côté client et côté serveur. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void combatreforged$stopLockedOffhandUse(CallbackInfo ci) {
        TwoHandedSystem.stopLockedOffhandUse((Player) (Object) this);
    }
}
