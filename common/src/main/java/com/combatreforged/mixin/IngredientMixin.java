package com.combatreforged.mixin;

import com.combatreforged.item.GearIngredients;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.stream.Stream;

/** Un ingrédient "arc vanilla" accepte aussi les arcs du mod, de même pour les arbalètes et les boucliers. */
@Mixin(Ingredient.class)
public abstract class IngredientMixin {

    @ModifyVariable(method = "<init>(Ljava/util/stream/Stream;)V", at = @At("HEAD"), argsOnly = true)
    private static Stream<? extends Ingredient.Value> combatreforged$acceptModGear(Stream<? extends Ingredient.Value> values) {
        return GearIngredients.withModVersions(values);
    }

    /** Les recettes lues dans les fichiers JSON passent par ce constructeur-ci. */
    @ModifyVariable(method = "<init>([Lnet/minecraft/world/item/crafting/Ingredient$Value;)V", at = @At("HEAD"), argsOnly = true)
    private static Ingredient.Value[] combatreforged$acceptModGear(Ingredient.Value[] values) {
        return GearIngredients.withModVersions(values);
    }
}
