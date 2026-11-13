package com.combatascended.client.tutorial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Trouve un modèle de démonstration des tutoriels : la façon de faire dépend du chargeur. */
public final class TutorialRendering {
    private static Function<ResourceLocation, BakedModel> lookup = id -> Minecraft.getInstance().getModelManager().getMissingModel();

    private TutorialRendering() {
    }

    public static void setModelLookup(Function<ResourceLocation, BakedModel> modelLookup) {
        lookup = modelLookup;
    }

    static BakedModel model(ResourceLocation id) {
        return lookup.apply(id);
    }
}
