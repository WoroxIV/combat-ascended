package com.combatascended.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Combo d'une arme (objet "combo" d'un fichier d'arme) : une suite de coups qui se répète.
 * Les coups qui touchent font avancer le combo ; un coup dans le vide ne compte pas.
 * Le combo d'une arme ambidextre n'existe qu'avec une arme du même type dans l'autre main.
 * <p>
 * Le champ "finisher" n'est pas gardé ici : au chargement, il est fusionné dans le dernier coup de {@code steps}.
 *
 * @param enabled           combo actif
 * @param resetAfterSeconds le combo repart du début après ce temps sans toucher
 * @param minCharge         un coup chargé à moins de ce pourcentage remet le combo à zéro (anti-spam)
 * @param maxBonusDamage    dégâts ajoutés au plus par un coup spécial (0 = pas de limite)
 * @param steps             les coups, dans l'ordre ; après le dernier, on recommence au premier
 */
public record ComboDefinition(boolean enabled, double resetAfterSeconds, double minCharge, double maxBonusDamage,
                              List<ComboStep> steps) {

    private static final int MAX_STEPS = 8;

    public static final DataField<Boolean> ENABLED = DataField.bool("enabled", false);
    public static final DataField<Double> RESET_AFTER = DataField.number("reset_after_seconds", 0.5D, 10.0D, 2.0D);
    public static final DataField<Double> MIN_CHARGE = DataField.number("min_charge", 0.0D, 100.0D, 90.0D);
    public static final DataField<Double> MAX_BONUS_DAMAGE = DataField.number("max_bonus_damage", 0.0D, 1024.0D, 0.0D);
    public static final DataField<List<ComboStep>> STEPS = DataField.objectList("steps", ComboStep.FIELDS, ComboStep.CODEC, MAX_STEPS);
    public static final DataField<ComboStep> FINISHER = DataField.object("finisher", ComboStep.FIELDS, ComboStep.CODEC, ComboStep.NORMAL);

    public static final FieldSet FIELDS = new FieldSet("weapon.combo",
            List.of(ENABLED, RESET_AFTER, MIN_CHARGE, MAX_BONUS_DAMAGE, STEPS, FINISHER),
            Map.of("enable", "enabled",
                    "reset_after", "reset_after_seconds",
                    "reset_seconds", "reset_after_seconds",
                    "charge", "min_charge",
                    "hits", "steps",
                    "last_step", "finisher"));

    public static final Codec<ComboDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENABLED.forGetter(ComboDefinition::enabled),
            RESET_AFTER.forGetter(ComboDefinition::resetAfterSeconds),
            MIN_CHARGE.forGetter(ComboDefinition::minCharge),
            MAX_BONUS_DAMAGE.forGetter(ComboDefinition::maxBonusDamage),
            STEPS.forGetter(ComboDefinition::steps)
    ).apply(instance, ComboDefinition::new));

    public static final ComboDefinition NONE = new ComboDefinition(false, RESET_AFTER.defaultValue(),
            MIN_CHARGE.defaultValue(), MAX_BONUS_DAMAGE.defaultValue(), List.of());

    public ComboDefinition {
        steps = List.copyOf(steps);
    }
}
