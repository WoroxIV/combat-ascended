package com.combatreforged.mixin;

import com.combatreforged.config.LegacyBetterWeaponry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Au chargement d'un objet : les enchantements de Better Weaponry 2.0 prennent leur nouvel identifiant. Couvre inventaires et coffres. */
@Mixin(ItemStack.class)
public abstract class ItemStackEnchantmentMixin {

    @ModifyVariable(method = "parse", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Tag combatreforged$renameEnchantments(Tag tag, HolderLookup.Provider provider) {
        return LegacyBetterWeaponry.renamedEnchantments(tag);
    }
}
