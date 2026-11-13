package com.combatascended.tutorial;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Ce qu'un personnage tient dans un tutoriel : un objet du jeu, ou un modèle de démonstration, qui n'existe que dans les tutoriels.
 *
 * @param model modèle de démonstration, sinon null
 */
public record SceneItem(ItemStack stack, @Nullable ResourceLocation model) {
    public static final SceneItem EMPTY = new SceneItem(ItemStack.EMPTY, null);

    public static SceneItem of(ItemStack stack) {
        return new SceneItem(stack, null);
    }

    public static SceneItem demo(ResourceLocation model) {
        return new SceneItem(ItemStack.EMPTY, model);
    }

    public boolean isEmpty() {
        return model == null && stack.isEmpty();
    }
}
