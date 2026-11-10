package com.combatreforged.registry;

import com.combatreforged.platform.Registered;
import com.combatreforged.platform.Platform;
import com.combatreforged.item.ModShieldDecorationRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

/** Types de recettes propres au mod. */
public final class ModRecipes {
    public static final Registered<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<ModShieldDecorationRecipe>> SHIELD_DECORATION =
            Platform.get().register(Registries.RECIPE_SERIALIZER, "shield_decoration", () -> new SimpleCraftingRecipeSerializer<>(ModShieldDecorationRecipe::new));

    private ModRecipes() {
    }

    /** Force la déclaration des recettes. */
    public static void init() {
    }
}
