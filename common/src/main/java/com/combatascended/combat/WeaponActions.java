package com.combatascended.combat;

import com.combatascended.CombatAscended;
import com.combatascended.compat.CombatCompat;
import com.combatascended.data.OnHitEffect;
import com.combatascended.data.WeaponData;
import com.combatascended.data.WeaponDefinition;
import com.combatascended.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Ce que fait une arme, quel que soit le mod qui l'ajoute : deux mains, effets à l'impact, balayage, portée. */
public final class WeaponActions {
    private static final double PERCENT = 100.0D;
    /** Hauteur de la zone de balayage autour de la cible (vanilla : 0.25 bloc). */
    private static final double SWEEP_HEIGHT = 0.25D;
    /** Un seul identifiant par bonus, comme les dégâts et la vitesse vanilla : changer d'arme remplace le bonus. */
    public static final ResourceLocation BASE_SWEEPING_RATIO_ID = id("base_sweeping_damage_ratio");
    public static final ResourceLocation BASE_REACH_ID = id("base_attack_reach");

    private WeaponActions() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CombatAscended.MODID, path);
    }

    /** Vrai si l'objet a le profil d'une arme à deux mains, et que cette règle est active. */
    public static boolean isTwoHanded(ItemStack stack) {
        return WeaponData.rules().twoHandedActive() && WeaponProfiles.definition(stack).map(WeaponDefinition::twoHanded).orElse(false);
    }

    /** Arme à deux mains d'après son fichier, même quand un autre mod gère le verrouillage de la main. */
    public static boolean isDeclaredTwoHanded(ItemStack stack) {
        return WeaponProfiles.definition(stack).map(WeaponDefinition::twoHanded).orElse(false);
    }

    /** Vrai si le joueur tient une arme à portée exacte et que notre portée s'applique : elle porte à la distance affichée, même en créatif. */
    public static boolean hasExactReach(Player player) {
        return WeaponData.rules().reachActive() && WeaponProfiles.definition(player.getMainHandItem()).map(WeaponDefinition::exactReach).orElse(false);
    }

    /**
     * Le jeu n'applique les bonus d'une arme qu'au moment où on la prend en main : après un changement de réglages,
     * on remplace ceux de l'arme déjà tenue. Certains bonus peuvent avoir disparu, d'où le retrait par identifiant.
     */
    public static void refreshEquippedModifiers(LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();
        if (WeaponProfiles.of(stack).isEmpty()) return;
        removeModifier(entity, Attributes.ENTITY_INTERACTION_RANGE, BASE_REACH_ID);
        removeModifier(entity, ModAttributes.armorPenetration(), ModAttributes.BASE_ARMOR_PENETRATION_ID);
        removeModifier(entity, Attributes.SWEEPING_DAMAGE_RATIO, BASE_SWEEPING_RATIO_ID);
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) return;
            instance.removeModifier(modifier.id());
            instance.addTransientModifier(modifier);
        });
    }

    private static void removeModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    /** Donne à la cible les effets à l'impact de cet objet, s'il a un profil d'arme (côté serveur), chacun selon sa chance. */
    public static void applyOnHitEffects(ItemStack weapon, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide || !WeaponData.rules().onHitEffectsActive() || CombatCompat.isFakePlayer(attacker)) return;
        for (OnHitEffect effect : WeaponProfiles.definition(weapon).map(WeaponDefinition::onHitEffects).orElse(List.of())) {
            if (attacker.getRandom().nextDouble() * PERCENT < effect.chance()) {
                effect.createInstance().ifPresent(instance -> target.addEffect(instance, attacker));
            }
        }
    }

    /** Zone touchée par le coup balayé autour de la cible (vanilla : 1 bloc). La portée d'attaque la limite aussi. */
    public static AABB sweepZone(WeaponDefinition definition, Entity target) {
        double radius = definition.sweep().radius();
        return target.getBoundingBox().inflate(radius, SWEEP_HEIGHT, radius);
    }
}
