package com.combatreforged.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Le comportement de combat d'une arme : un archétype ({@code combat_reforged/archetypes/<id>.json}) ou un fichier
 * d'arme ({@code combat_reforged/weapons/<fichier>.json}) qui s'applique aux objets de son champ {@code items}.
 * Un fichier peut reprendre un archétype avec {@code parent} : il n'écrit alors que ce qu'il change.
 * <p>
 * Ajouter un champ : le déclarer ci-dessous avec {@link DataField}, l'ajouter au record, au {@link #CODEC} et à
 * {@link #FIELDS}, puis écrire son nom et son aide dans les sources de traduction
 * ({@code combat_reforged.field.weapon.<champ>} et {@code .help}). La validation, le schéma JSON et le README suivent.
 *
 * @param formatVersion    version du format de fichier (1)
 * @param parent           archétype repris par ce fichier ({@link DataField#NONE} = aucun)
 * @param twoHanded        arme à deux mains : main secondaire bloquée et cachée
 * @param dualWielded      peut être maniée avec une autre arme ambidextre dans l'autre main (ignoré si à deux mains)
 * @param reachBonus       portée ajoutée à celle de l'objet, en blocs (peut être négative)
 * @param exactReach       la portée affichée est la portée réelle, même en créatif
 * @param armorPenetration part de l'armure de la cible ignorée, en % (0 à 100)
 * @param disablesShield   un coup sur un bouclier levé le désactive, comme une hache
 * @param onHitEffects     effets donnés à la cible touchée
 * @param sweep            coup balayé
 * @param ability          capacité du clic droit (une seule par arme)
 * @param parry            réglages de la parade, utilisés si {@code ability} vaut "parry"
 * @param dash             réglages du dash, utilisés si {@code ability} vaut "dash"
 * @param combo            combo : suite de coups qui se répète, avec des coups spéciaux
 * @param items            objets qui reçoivent ce comportement (ids, ou "#tag")
 */
public record WeaponDefinition(int formatVersion,
                               ResourceLocation parent,
                               boolean twoHanded,
                               boolean dualWielded,
                               double reachBonus,
                               boolean exactReach,
                               double armorPenetration,
                               boolean disablesShield,
                               List<OnHitEffect> onHitEffects,
                               SweepDefinition sweep,
                               WeaponAbility ability,
                               ParryDefinition parry,
                               DashDefinition dash,
                               ComboDefinition combo,
                               List<String> items) {

    public static final int FORMAT_VERSION = 1;
    private static final int MAX_ON_HIT_EFFECTS = 8;
    private static final int MAX_ITEMS = 64;

    public static final DataField<Integer> FORMAT = DataField.integer("format_version", 1, FORMAT_VERSION, FORMAT_VERSION);
    public static final DataField<ResourceLocation> PARENT = DataField.optionalId("parent", "archetype", null, "combat_reforged:katana");
    public static final DataField<Boolean> TWO_HANDED = DataField.bool("two_handed", false);
    public static final DataField<Boolean> DUAL_WIELDED = DataField.bool("dual_wielded", false);
    public static final DataField<Double> REACH_BONUS = DataField.number("reach_bonus", -2.5D, 61.0D, 0.0D);
    public static final DataField<Boolean> EXACT_REACH = DataField.bool("exact_reach", false);
    public static final DataField<Double> ARMOR_PENETRATION = DataField.number("armor_penetration", 0.0D, 100.0D, 0.0D);
    public static final DataField<Boolean> DISABLES_SHIELD = DataField.bool("disables_shield", false);
    public static final DataField<List<OnHitEffect>> ON_HIT_EFFECTS =
            DataField.objectList("on_hit_effects", OnHitEffect.FIELDS, OnHitEffect.CODEC, MAX_ON_HIT_EFFECTS);
    public static final DataField<SweepDefinition> SWEEP =
            DataField.object("sweep", SweepDefinition.FIELDS, SweepDefinition.CODEC, SweepDefinition.VANILLA);
    public static final DataField<WeaponAbility> ABILITY = DataField.choice("ability", WeaponAbility.values(), WeaponAbility.NONE);
    public static final DataField<ParryDefinition> PARRY =
            DataField.object("parry", ParryDefinition.FIELDS, ParryDefinition.CODEC, ParryDefinition.DEFAULT);
    public static final DataField<DashDefinition> DASH =
            DataField.object("dash", DashDefinition.FIELDS, DashDefinition.CODEC, DashDefinition.DEFAULT);
    public static final DataField<ComboDefinition> COMBO =
            DataField.object("combo", ComboDefinition.FIELDS, ComboDefinition.CODEC, ComboDefinition.NONE);
    public static final DataField<List<String>> ITEMS = DataField.itemList("items", MAX_ITEMS, "minecraft:diamond_sword", "#minecraft:axes");

    public static final FieldSet FIELDS = new FieldSet("weapon",
            List.of(FORMAT, PARENT, TWO_HANDED, DUAL_WIELDED, REACH_BONUS, EXACT_REACH,
                    ARMOR_PENETRATION, DISABLES_SHIELD, ON_HIT_EFFECTS, SWEEP, ABILITY, PARRY, DASH, COMBO, ITEMS),
            Map.ofEntries(
                    Map.entry("archetype", "parent"),
                    Map.entry("preset", "parent"),
                    Map.entry("reach", "reach_bonus"),
                    Map.entry("attack_reach", "reach_bonus"),
                    Map.entry("two_hand", "two_handed"),
                    Map.entry("dual_wield", "dual_wielded"),
                    Map.entry("dual_wieldable", "dual_wielded"),
                    Map.entry("dual_handed", "dual_wielded"),
                    Map.entry("penetration", "armor_penetration"),
                    Map.entry("armor_pen", "armor_penetration"),
                    Map.entry("effects", "on_hit_effects"),
                    Map.entry("slowness", "on_hit_effects"),
                    Map.entry("sweeping", "sweep"),
                    Map.entry("disable_shield", "disables_shield"),
                    Map.entry("abilities", "ability"),
                    Map.entry("right_click", "ability"),
                    Map.entry("block", "parry"),
                    Map.entry("blocking", "parry"),
                    Map.entry("dodge", "dash"),
                    Map.entry("lunge", "dash"),
                    Map.entry("combos", "combo"),
                    Map.entry("item", "items"),
                    Map.entry("applies_to", "items")));

    public static final Codec<WeaponDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FORMAT.forGetter(WeaponDefinition::formatVersion),
            PARENT.forGetter(WeaponDefinition::parent),
            TWO_HANDED.forGetter(WeaponDefinition::twoHanded),
            DUAL_WIELDED.forGetter(WeaponDefinition::dualWielded),
            REACH_BONUS.forGetter(WeaponDefinition::reachBonus),
            EXACT_REACH.forGetter(WeaponDefinition::exactReach),
            ARMOR_PENETRATION.forGetter(WeaponDefinition::armorPenetration),
            DISABLES_SHIELD.forGetter(WeaponDefinition::disablesShield),
            ON_HIT_EFFECTS.forGetter(WeaponDefinition::onHitEffects),
            SWEEP.forGetter(WeaponDefinition::sweep),
            ABILITY.forGetter(WeaponDefinition::ability),
            PARRY.forGetter(WeaponDefinition::parry),
            DASH.forGetter(WeaponDefinition::dash),
            COMBO.forGetter(WeaponDefinition::combo),
            ITEMS.forGetter(WeaponDefinition::items)
    ).apply(instance, WeaponDefinition::new));

    /** Utilisé si un fichier est illisible : aucune capacité, comme un objet ordinaire. */
    public static final WeaponDefinition FALLBACK = new WeaponDefinition(FORMAT_VERSION, DataField.NONE,
            false, false, 0.0D, false, 0.0D, false, List.of(), SweepDefinition.VANILLA, WeaponAbility.NONE,
            ParryDefinition.DEFAULT, DashDefinition.DEFAULT, ComboDefinition.NONE, List.of());

    public WeaponDefinition {
        onHitEffects = List.copyOf(onHitEffects);
        items = List.copyOf(items);
    }

    /** Copie pour une arme détectée automatiquement : jamais de main verrouillée, et pas de clic droit si l'objet a le sien. */
    public WeaponDefinition forDetectedItem(boolean keepsOwnRightClick) {
        return new WeaponDefinition(formatVersion, parent, false, dualWielded, reachBonus, exactReach, armorPenetration, disablesShield,
                onHitEffects, sweep, keepsOwnRightClick ? WeaponAbility.NONE : ability, parry, dash, combo, items);
    }
}
