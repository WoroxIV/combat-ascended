package com.combatreforged.compat.jei;

import com.combatreforged.CombatReforged;
import com.combatreforged.compat.RecipeViewerInfo;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Lu seulement si JEI est installé. */
@JeiPlugin
public final class CombatReforgedJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = new ResourceLocation(CombatReforged.MODID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        for (RecipeViewerInfo.Entry entry : RecipeViewerInfo.entries(Minecraft.getInstance().level.registryAccess())) {
            registration.addItemStackInfo(entry.stacks(), entry.lines().toArray(Component[]::new));
        }
    }
}
