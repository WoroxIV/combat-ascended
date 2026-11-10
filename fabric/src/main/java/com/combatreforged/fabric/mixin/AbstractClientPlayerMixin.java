package com.combatreforged.fabric.mixin;

import com.combatreforged.client.RangedWeaponClient;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Le zoom pendant le bandage d'un arc du mod. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @ModifyReturnValue(method = "getFieldOfViewModifier", at = @At("RETURN"))
    private float combatreforged$bowZoom(float modifier) {
        return RangedWeaponClient.modifyFov((AbstractClientPlayer) (Object) this, modifier);
    }
}
