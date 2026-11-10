package com.combatreforged.fabric.mixin;

import com.combatreforged.fabric.FabricReachClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** La cible visée suit la portée de l'arme. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final Minecraft minecraft;

    @Inject(method = "pick", at = @At("TAIL"))
    private void combatreforged$weaponReach(float partialTicks, CallbackInfo ci) {
        FabricReachClient.pick(minecraft, partialTicks);
    }
}
