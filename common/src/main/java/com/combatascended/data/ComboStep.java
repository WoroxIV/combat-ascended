package com.combatascended.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Un coup d'un combo (élément de la liste "combo.steps"). {@code {}} = coup normal ; les champs écrits
 * en font un coup spécial, toujours porté par l'arme principale.
 *
 * @param bothHands          frappe avec les deux armes à la fois (même type d'arme en main secondaire) :
 *                           dégâts = moyenne des deux armes, effets et usure des deux armes
 * @param radius             0 = coup normal sur la cible ; sinon touche tous les ennemis à cette distance (blocs)
 * @param angle              largeur de la zone devant le joueur, en degrés (360 = tout autour)
 * @param damageMultiplier   multiplicateur des dégâts de ce coup
 * @param knockback          recul en plus, en niveaux d'enchantement Recul (1 = comme Recul I)
 * @param cooldownMultiplier multiplicateur de la recharge du coup suivant (1.5 = 50 % plus longue)
 * @param gesture            mouvement du bras pour ce coup
 * @param sound              son joué par ce coup ({@link DataField#NONE} = aucun son en plus)
 * @param particles          particules jouées par ce coup ({@link DataField#NONE} = aucune)
 */
public record ComboStep(boolean bothHands, double radius, double angle, double damageMultiplier, double knockback,
                        double cooldownMultiplier, ComboGesture gesture, ResourceLocation sound, ResourceLocation particles) {

    public static final DataField<Boolean> BOTH_HANDS = DataField.bool("both_hands", false);
    public static final DataField<Double> RADIUS = DataField.number("radius", 0.0D, 8.0D, 0.0D);
    public static final DataField<Double> ANGLE = DataField.number("angle", 10.0D, 360.0D, 360.0D);
    public static final DataField<Double> DAMAGE_MULTIPLIER = DataField.number("damage_multiplier", 0.0D, 10.0D, 1.0D);
    public static final DataField<Double> KNOCKBACK = DataField.number("knockback", 0.0D, 5.0D, 0.0D);
    public static final DataField<Double> COOLDOWN_MULTIPLIER = DataField.number("cooldown_multiplier", 0.1D, 5.0D, 1.0D);
    public static final DataField<ComboGesture> GESTURE = DataField.choice("gesture", ComboGesture.values(), ComboGesture.DEFAULT);
    public static final DataField<ResourceLocation> SOUND = DataField.optionalId("sound", "sound",
            BuiltInRegistries.SOUND_EVENT::keySet, "minecraft:entity.player.attack.crit");
    public static final DataField<ResourceLocation> PARTICLES = DataField.optionalId("particles", "particle",
            BuiltInRegistries.PARTICLE_TYPE::keySet, "minecraft:crit");

    public static final FieldSet FIELDS = new FieldSet("weapon.combo.steps",
            List.of(BOTH_HANDS, RADIUS, ANGLE, DAMAGE_MULTIPLIER, KNOCKBACK, COOLDOWN_MULTIPLIER, GESTURE, SOUND, PARTICLES),
            Map.of("dual", "both_hands",
                    "two_hands", "both_hands",
                    "range", "radius",
                    "area", "radius",
                    "arc", "angle",
                    "damage", "damage_multiplier",
                    "cooldown", "cooldown_multiplier",
                    "animation", "gesture",
                    "particle", "particles"));

    public static final Codec<ComboStep> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BOTH_HANDS.forGetter(ComboStep::bothHands),
            RADIUS.forGetter(ComboStep::radius),
            ANGLE.forGetter(ComboStep::angle),
            DAMAGE_MULTIPLIER.forGetter(ComboStep::damageMultiplier),
            KNOCKBACK.forGetter(ComboStep::knockback),
            COOLDOWN_MULTIPLIER.forGetter(ComboStep::cooldownMultiplier),
            GESTURE.forGetter(ComboStep::gesture),
            SOUND.forGetter(ComboStep::sound),
            PARTICLES.forGetter(ComboStep::particles)
    ).apply(instance, ComboStep::new));

    /** Coup normal : ne change rien au coup vanilla. */
    public static final ComboStep NORMAL = new ComboStep(BOTH_HANDS.defaultValue(), RADIUS.defaultValue(), ANGLE.defaultValue(),
            DAMAGE_MULTIPLIER.defaultValue(), KNOCKBACK.defaultValue(), COOLDOWN_MULTIPLIER.defaultValue(), GESTURE.defaultValue(), DataField.NONE, DataField.NONE);

    /** Coup en zone : remplace le coup balayé vanilla. */
    public boolean hasArea() {
        return radius > 0.0D;
    }

    /** Un coup spécial change quelque chose au coup vanilla ; un coup normal ({}) ne fait que compter. Le geste, le son et les particules ne comptent pas. */
    public boolean isSpecial() {
        return bothHands || hasArea() || damageMultiplier != 1.0D || knockback > 0.0D || cooldownMultiplier != 1.0D;
    }
}
