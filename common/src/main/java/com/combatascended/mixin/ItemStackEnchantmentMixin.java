package com.combatascended.mixin;

import com.combatascended.config.LegacyBetterWeaponry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Au chargement d'un objet : les enchantements de Better Weaponry 2.0 prennent leur nouvel identifiant. Couvre inventaires et coffres. */
@Mixin(ItemStack.class)
public abstract class ItemStackEnchantmentMixin {

    @ModifyVariable(method = "of", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static CompoundTag combatascended$renameEnchantments(CompoundTag tag) {
        return LegacyBetterWeaponry.renamedEnchantments(tag);
    }
}
