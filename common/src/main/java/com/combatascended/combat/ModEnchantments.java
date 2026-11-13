package com.combatascended.combat;

import com.combatascended.CombatAscended;
import com.combatascended.data.RulesDefinition;
import com.combatascended.data.WeaponAbility;
import com.combatascended.data.WeaponData;
import com.combatascended.data.WeaponDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Les enchantements du mod : Reflective, Riposte et Élan, liés à la parade et au dash.
 * Ils sont déclarés dans data/combat_ascended/enchantment/ (coût, niveaux, objets acceptés).
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> REFLECTIVE = key("reflective");
    public static final ResourceKey<Enchantment> RIPOSTE = key("riposte");
    public static final ResourceKey<Enchantment> MOMENTUM = key("momentum");

    public static final List<ResourceKey<Enchantment>> ALL = List.of(REFLECTIVE, RIPOSTE, MOMENTUM);

    private static final String DESCRIPTION_PREFIX = "enchantment." + CombatAscended.MODID + ".";
    private static final double REFLECT_PER_LEVEL = 0.1D;
    public static final double RIPOSTE_PERCENT_PER_LEVEL = 25.0D;
    private static final double MOMENTUM_PER_LEVEL = 0.15D;
    private static final double MOMENTUM_MIN_MULTIPLIER = 0.25D;
    /** Recul de base d'un coup renvoyé, comme un coup normal. */
    private static final double REFLECT_KNOCKBACK = 0.4D;
    private static final int REFLECT_POISON_TICKS = 140;
    private static final int REFLECT_WITHER_TICKS = 200;
    private static final int REFLECT_HUNGER_TICKS = 140;
    private static final int TRAIL_POINTS = 8;

    private ModEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(CombatAscended.MODID, path));
    }

    /** Niveau de cet enchantement sur l'objet, 0 s'il ne l'a pas (ou si un datapack a retiré l'enchantement). */
    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (stack.isEmpty()) return 0;
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(enchantment)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
    }

    /**
     * Enchantements liés à une capacité : c'est l'arme telle que configurée qui décide, pas un tag figé. Donner "parry"
     * à une arme dans l'éditeur lui ouvre Reflective et Riposte.
     *
     * @return vide si l'enchantement ne dépend d'aucune capacité (le tag de l'enchantement décide alors)
     */
    public static Optional<Boolean> supportedByAbility(WeaponDefinition definition, Holder<Enchantment> enchantment) {
        RulesDefinition rules = WeaponData.rules();
        boolean parries = definition.ability() == WeaponAbility.PARRY;
        if (enchantment.is(REFLECTIVE)) return Optional.of(parries && rules.parryActive());
        if (enchantment.is(RIPOSTE)) return Optional.of(parries && rules.perfectParryActive());
        if (enchantment.is(MOMENTUM)) return Optional.of(definition.ability() == WeaponAbility.DASH && rules.dashActive());
        return Optional.empty();
    }

    /** Vrai pour nos trois enchantements. Un enchantement ne connaît pas sa propre clé : on reconnaît sa description. */
    public static boolean followsAbility(Enchantment enchantment) {
        Component description = enchantment.description();
        return description.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith(DESCRIPTION_PREFIX);
    }

    /** Décision d'après la capacité : parade pour Reflective et Riposte, dash pour Élan. Sans profil, le tag de l'enchantement décide. */
    public static Optional<Boolean> supportedByAbility(ItemStack stack, Enchantment enchantment) {
        Optional<WeaponDefinition> definition = WeaponProfiles.definition(stack);
        if (definition.isEmpty() || !(enchantment.description().getContents() instanceof TranslatableContents translatable)) return Optional.empty();
        RulesDefinition rules = WeaponData.rules();
        boolean parries = definition.get().ability() == WeaponAbility.PARRY;
        return switch (translatable.getKey().substring(DESCRIPTION_PREFIX.length())) {
            case "reflective" -> Optional.of(parries && rules.parryActive());
            case "riposte" -> Optional.of(parries && rules.perfectParryActive());
            case "momentum" -> Optional.of(definition.get().ability() == WeaponAbility.DASH && rules.dashActive());
            default -> Optional.empty();
        };
    }

    /** Borné : un niveau obtenu par commande ou par un mod ne doit pas supprimer la recharge. */
    public static double momentumMultiplier(int level) {
        return Math.max(MOMENTUM_MIN_MULTIPLIER, 1.0D - level * MOMENTUM_PER_LEVEL);
    }

    /** L'attaquant s'il a frappé lui-même au corps à corps : ni projectile, ni dégâts renvoyés. */
    @Nullable
    public static LivingEntity meleeAttacker(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && !source.is(DamageTypes.THORNS) ? attacker : null;
    }

    /**
     * Reflective : renvoie une part du coup à l'attaquant, avec les propriétés de son attaque (effets de son arme,
     * Aura de feu, poison naturel) et du recul. Passe par des dégâts d'épines, qui ne peuvent pas être renvoyés à leur tour.
     *
     * @param guard objet qui a paré (arme ou bouclier)
     */
    public static void reflect(Player defender, ItemStack guard, DamageSource source, float damage) {
        LivingEntity attacker = meleeAttacker(source);
        if (attacker == null || !(defender.level() instanceof ServerLevel world)) return;
        int enchantmentLevel = level(world, guard, REFLECTIVE);
        // Après un renvoi, l'objet se recharge : l'icône vanilla de la barre d'objets le montre.
        if (enchantmentLevel <= 0 || defender.getCooldowns().isOnCooldown(guard.getItem())) return;
        double cooldown = WeaponData.gear().reflectiveCooldownSeconds();
        if (cooldown > 0.0D) AbilityCooldowns.start(defender, guard, cooldown);

        ItemStack attackerWeapon = attacker.getMainHandItem();
        DamageSource reflected = defender.damageSources().thorns(defender);
        attacker.hurt(reflected, (float) (damage * enchantmentLevel * REFLECT_PER_LEVEL));
        WeaponActions.applyOnHitEffects(attackerWeapon, attacker, defender);
        EnchantmentHelper.doPostAttackEffectsWithItemSource(world, attacker, reflected, attackerWeapon);
        applyNaturalEffect(attacker, defender);
        pushBack(attacker, defender, REFLECT_KNOCKBACK + attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK));

        trail(world, ParticleTypes.END_ROD, defender.getEyePosition(), attacker.getEyePosition());
        world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.3F);
    }

    /** Les mobs dont la morsure ou le coup empoisonne reçoivent leur propre effet. */
    private static void applyNaturalEffect(LivingEntity attacker, LivingEntity defender) {
        if (attacker instanceof CaveSpider) attacker.addEffect(new MobEffectInstance(MobEffects.POISON, REFLECT_POISON_TICKS), defender);
        else if (attacker instanceof WitherSkeleton) attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, REFLECT_WITHER_TICKS), defender);
        else if (attacker instanceof Husk) attacker.addEffect(new MobEffectInstance(MobEffects.HUNGER, REFLECT_HUNGER_TICKS), defender);
    }

    /** Pousse {@code target} à l'opposé de {@code from}. */
    public static void pushBack(LivingEntity target, LivingEntity from, double strength) {
        target.knockback(strength, from.getX() - target.getX(), from.getZ() - target.getZ());
        // Le mouvement d'un joueur est géré par son client : il faut le prévenir.
        if (target instanceof ServerPlayer) target.hurtMarked = true;
    }

    /** Traînée de particules entre deux points (ex : la vie qui revient vers le joueur). */
    public static void trail(ServerLevel world, ParticleOptions particle, Vec3 from, Vec3 to) {
        for (int i = 0; i <= TRAIL_POINTS; i++) {
            Vec3 point = from.lerp(to, (double) i / TRAIL_POINTS);
            world.sendParticles(particle, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }
}
