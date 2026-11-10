package com.combatreforged.forge;

import com.combatreforged.client.WeaponClientExtensions;
import com.combatreforged.item.WeaponItem;
import com.combatreforged.item.WeaponMaterial;
import com.combatreforged.item.WeaponType;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Arme du mod sur Forge : le rendu en main passe par l'objet. */
final class ForgeWeaponItem extends WeaponItem {
    ForgeWeaponItem(WeaponType type, WeaponMaterial material, Properties properties) {
        super(type, material, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new WeaponClientExtensions());
    }
}
