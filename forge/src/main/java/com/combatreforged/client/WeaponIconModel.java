package com.combatreforged.client;

import com.combatreforged.CombatReforged;
import com.combatreforged.item.WeaponMaterial;
import com.combatreforged.item.WeaponMaterials;
import com.combatreforged.item.WeaponType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;

import java.util.Map;

/** Applique {@link WeaponIconLayout} dans les interfaces. Le reste (en main, au sol...) ne change pas. */
public final class WeaponIconModel extends BakedModelWrapper<BakedModel> {
    private final WeaponIconLayout layout;

    private WeaponIconModel(BakedModel original, WeaponIconLayout layout) {
        super(original);
        this.layout = layout;
    }

    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        for (WeaponType type : WeaponType.values()) {
            for (WeaponMaterial material : WeaponMaterials.all()) {
                ResourceLocation id = new ResourceLocation(CombatReforged.MODID, material.itemName(type));
                models.computeIfPresent(new ModelResourceLocation(id, "inventory"),
                        (key, model) -> WeaponIconLayout.of(model).<BakedModel>map(layout -> new WeaponIconModel(model, layout)).orElse(model));
            }
        }
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        if (context != ItemDisplayContext.GUI) return super.applyTransform(context, pose, leftHand);
        layout.apply(pose);
        return this;
    }
}
