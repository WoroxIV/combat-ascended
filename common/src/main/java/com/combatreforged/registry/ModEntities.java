package com.combatreforged.registry;

import com.combatreforged.platform.Registered;
import com.combatreforged.platform.Platform;
import com.combatreforged.entity.ShurikenEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entités du mod. */
public final class ModEntities {
    public static final Registered<EntityType<?>, EntityType<ShurikenEntity>> SHURIKEN = Platform.get().register(Registries.ENTITY_TYPE, "shuriken",
            () -> EntityType.Builder.<ShurikenEntity>of(ShurikenEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build("shuriken"));

    private ModEntities() {
    }

    /** Force la déclaration des entités. */
    public static void init() {
    }
}
