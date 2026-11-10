package com.combatreforged.tutorial;

import com.combatreforged.CombatReforged;
import com.combatreforged.data.WeaponData;
import com.combatreforged.data.WeaponDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Les armes montrées dans les tutoriels. Leurs modèles et textures sont dans le jar : elles n'existent pas comme objets.
 * Dégâts et portée sont fixés ici, le comportement (parade, combo, dash) est lu dans l'archétype du même nom.
 */
public enum DemoWeapon {
    CLAYMORE("claymore", 11.5D, 4.0D),
    BROADSWORD("broadsword", 8.0D, 4.0D),
    DAGGER("dagger", 4.0D, 2.0D),
    KATANA("katana", 8.0D, 3.5D),
    SPEAR("spear", 7.5D, 5.0D),
    SCYTHE("scythe", 15.5D, 5.0D),
    HAMMER("hammer", 19.0D, 4.0D),
    RAPIER("rapier", 4.5D, 3.0D);

    public static final double DIAMOND_DAGGER_DAMAGE = 5.0D;
    /** La main secondaire de la démonstration du dual wield. */
    public static final SceneItem DIAMOND_DAGGER = SceneItem.demo(model("diamond_dagger"));
    public static final SceneItem IRON_SHIELD = SceneItem.demo(model("iron_shield"));

    private final String id;
    private final double damage;
    private final double reach;

    DemoWeapon(String id, double damage, double reach) {
        this.id = id;
        this.damage = damage;
        this.reach = reach;
    }

    private static ResourceLocation model(String name) {
        return ResourceLocation.fromNamespaceAndPath(CombatReforged.MODID, "tutorial/" + name);
    }

    /** Tous les modèles de démonstration, à charger avec le jeu. */
    public static List<ResourceLocation> allModels() {
        List<ResourceLocation> models = new ArrayList<>();
        for (DemoWeapon weapon : values()) models.add(model("iron_" + weapon.id));
        models.add(DIAMOND_DAGGER.model());
        return models;
    }

    public SceneItem item() {
        return SceneItem.demo(model("iron_" + id));
    }

    public double damage() {
        return damage;
    }

    public double reach() {
        return reach;
    }

    /** Comportement en vigueur de l'archétype : un réglage modifié dans l'éditeur se retrouve dans la démonstration. */
    public WeaponDefinition definition() {
        return WeaponData.definition(ResourceLocation.fromNamespaceAndPath(CombatReforged.MODID, id));
    }
}
