package com.combatreforged.forge;

import com.combatreforged.client.ShieldClientExtensions;
import com.combatreforged.item.WeaponMaterial;
import com.combatreforged.item.WeaponShieldItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Bouclier du mod sur Forge : le modèle 3D passe par l'objet. */
final class ForgeShieldItem extends WeaponShieldItem {
    ForgeShieldItem(WeaponMaterial material, Properties properties) {
        super(material, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new ShieldClientExtensions());
    }
}
