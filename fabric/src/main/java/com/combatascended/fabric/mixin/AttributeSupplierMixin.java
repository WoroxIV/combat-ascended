package com.combatascended.fabric.mixin;

import com.combatascended.fabric.FabricAttributes;
import com.combatascended.registry.ModAttributes;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Consumer;

/** Les attributs du mod existent sur toutes les créatures (joueurs, mais aussi mobs qui tiendraient une arme), à leur valeur par défaut. */
@Mixin(AttributeSupplier.class)
public abstract class AttributeSupplierMixin {
    @Shadow @Final private Map<Attribute, AttributeInstance> instances;

    @ModifyReturnValue(method = "createInstance", at = @At("RETURN"))
    @Nullable
    private AttributeInstance combatascended$modAttributes(@Nullable AttributeInstance instance, Consumer<AttributeInstance> onDirty, Attribute attribute) {
        return instance == null && combatascended$isModAttribute(attribute) ? new AttributeInstance(attribute, onDirty) : instance;
    }

    /** Lecture de la valeur par défaut, tant que la créature n'a pas sa propre copie de l'attribut. */
    @Inject(method = "getAttributeInstance", at = @At("HEAD"), cancellable = true)
    private void combatascended$modDefaults(Attribute attribute, CallbackInfoReturnable<AttributeInstance> cir) {
        if (!instances.containsKey(attribute) && combatascended$isModAttribute(attribute)) cir.setReturnValue(new AttributeInstance(attribute, instance -> { }));
    }

    @Unique
    private static boolean combatascended$isModAttribute(Attribute attribute) {
        return attribute == ModAttributes.ARMOR_PENETRATION.get() || attribute == ModAttributes.SWEEPING_DAMAGE_RATIO.get()
                || attribute == FabricAttributes.ENTITY_REACH.get();
    }
}
