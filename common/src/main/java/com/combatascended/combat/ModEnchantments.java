package com.combatascended.combat;

import com.combatascended.CombatAscended;
import com.combatascended.data.RulesDefinition;
import com.combatascended.data.WeaponAbility;
import com.combatascended.data.WeaponData;
import com.combatascended.data.WeaponDefinition;
import com.combatascended.platform.Platform;
import com.combatascended.registry.ModTags;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.entity.EquipmentSlot;
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
import java.util.Set;
import java.util.function.Predicate;

/**
 * Les enchantements du mod : Reflective, Riposte et Élan, liés à la parade et au dash.
 * Coût, niveaux et objets acceptés sont dans {@link #init()}.
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> REFLECTIVE = key("reflective");
    public static final ResourceKey<Enchantment> RIPOSTE = key("riposte");
    public static final ResourceKey<Enchantment> MOMENTUM = key("momentum");

    public static final List<ResourceKey<Enchantment>> ALL = List.of(REFLECTIVE, RIPOSTE, MOMENTUM);

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

    private static final EquipmentSlot[] MAIN_HAND = {EquipmentSlot.MAINHAND};
    private static final EquipmentSlot[] BOTH_HANDS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    private static final ModEnchantment.Costs COMMON_COSTS = new ModEnchantment.Costs(12, 11, 62, 11, 3, false);
    private static final ModEnchantment.Costs TREASURE_COSTS = new ModEnchantment.Costs(25, 25, 75, 25, 3, true);

    private ModEnchantments() {
    }

    /** Force la déclaration des enchantements. */
    public static void init() {
        register(REFLECTIVE, Enchantment.Rarity.VERY_RARE, BOTH_HANDS, TREASURE_COSTS,
                stack -> stack.is(ModTags.ENCHANTABLE_PARRYING) || Platform.get().isShield(stack), Set.of(RIPOSTE.location()));
        register(RIPOSTE, Enchantment.Rarity.RARE, MAIN_HAND, COMMON_COSTS, stack -> stack.is(ModTags.ENCHANTABLE_PARRY_WEAPONS), Set.of(REFLECTIVE.location()));
        register(MOMENTUM, Enchantment.Rarity.RARE, MAIN_HAND, COMMON_COSTS, stack -> stack.is(ModTags.ENCHANTABLE_DASHING), Set.of());
    }

    private static void register(ResourceKey<Enchantment> key, Enchantment.Rarity rarity, EquipmentSlot[] slots, ModEnchantment.Costs costs,
                                 Predicate<ItemStack> accepted, Set<ResourceLocation> exclusiveWith) {
        Platform.get().register(Registries.ENCHANTMENT, key.location().getPath(), () -> new ModEnchantment(rarity, slots, costs, accepted, exclusiveWith));
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, new ResourceLocation(CombatAscended.MODID, path));
    }

    /** Niveau de cet enchantement sur l'objet, 0 s'il ne l'a pas. */
    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (stack.isEmpty()) return 0;
        Enchantment value = BuiltInRegistries.ENCHANTMENT.get(enchantment);
        return value == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(value, stack);
    }

    /** Vrai pour nos trois enchantements : ils suivent la capacité de l'arme. */
    public static boolean followsAbility(Enchantment enchantment) {
        return is(enchantment, REFLECTIVE) || is(enchantment, RIPOSTE) || is(enchantment, MOMENTUM);
    }

    /** Décision d'après la capacité : parade pour Reflective et Riposte, dash pour Élan. Sans profil, le type d'objet décide. */
    public static Optional<Boolean> supportedByAbility(ItemStack stack, Enchantment enchantment) {
        Optional<WeaponDefinition> definition = WeaponProfiles.definition(stack);
        if (definition.isEmpty()) return Optional.empty();
        RulesDefinition rules = WeaponData.rules();
        boolean parries = definition.get().ability() == WeaponAbility.PARRY;
        if (is(enchantment, REFLECTIVE)) return Optional.of(parries && rules.parryActive());
        if (is(enchantment, RIPOSTE)) return Optional.of(parries && rules.perfectParryActive());
        if (is(enchantment, MOMENTUM)) return Optional.of(definition.get().ability() == WeaponAbility.DASH && rules.dashActive());
        return Optional.empty();
    }

    private static boolean is(Enchantment enchantment, ResourceKey<Enchantment> key) {
        return key.location().equals(BuiltInRegistries.ENCHANTMENT.getKey(enchantment));
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
        GearWear.applyPostAttackEnchantments(defender, attacker, attackerWeapon);
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
