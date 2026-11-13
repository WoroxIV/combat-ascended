package com.combatascended.combat;

import com.combatascended.platform.Platform;
import com.combatascended.CombatAscended;
import com.combatascended.api.CombatAscendedApi;
import com.combatascended.compat.CombatCompat;
import com.combatascended.data.ComboDefinition;
import com.combatascended.data.ComboGesture;
import com.combatascended.data.ComboStep;
import com.combatascended.data.DataField;
import com.combatascended.data.WeaponData;
import com.combatascended.data.WeaponDefinition;
import com.combatascended.network.ComboSyncPayload;
import com.combatascended.network.DualSwingPayload;
import com.combatascended.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Combos : chaque coup qui touche fait avancer le combo de l'arme principale ; certains coups sont spéciaux
 * (zone, deux armes à la fois, dégâts, recul, recharge). Appelé par PlayerCombatMixin autour de Player.attack(),
 * côté client (prédiction : animation, indicateur, son) et côté serveur (dégâts), avec la même logique.
 * <ul>
 *   <li>un coup dans le vide n'appelle pas attack() : il ne fait pas avancer le combo ;</li>
 *   <li>un coup chargé sous min_charge remet le combo à zéro, comme reset_after_seconds sans toucher
 *       ou un changement d'arme ;</li>
 *   <li>un coup spécial est toujours porté par l'arme principale, et le combo suivant repart de cette main.</li>
 * </ul>
 */
public final class ComboSystem {
    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double PERCENT = 100.0D;
    /** Une zone de combo touche aussi les ennemis un peu plus haut ou plus bas que le joueur. */
    private static final double AREA_HEIGHT = 1.0D;
    /** Comme le vanilla : 1 niveau de Recul = 0.5 de force de recul. */
    private static final double KNOCKBACK_PER_LEVEL = 0.5D;
    /** Particules de balayage : une tous les 30° de la zone. */
    private static final double DEGREES_PER_PARTICLE = 30.0D;
    private static final int STEP_PARTICLES = 8;

    private static final ResourceLocation DAMAGE_BONUS_ID = id("combo_damage_bonus");
    private static final ResourceLocation DAMAGE_MULTIPLIER_ID = id("combo_damage_multiplier");
    private static final ResourceLocation KNOCKBACK_ID = id("combo_knockback");

    private ComboSystem() {
    }

    /**
     * Ce qui va se passer pour un coup : l'étape du combo, si elle compte (coup assez chargé) et si les deux
     * armes frappent. {@code combo == null} : pas de combo pour ce coup.
     */
    public record Hit(@Nullable ComboDefinition combo, @Nullable ResourceLocation type, @Nullable ComboStep step, int index,
                      boolean counts, boolean bothHands, float strength) {
        static Hit none(float strength) {
            return new Hit(null, null, null, 0, false, false, strength);
        }

        /** Coup spécial du combo (zone, deux armes, multiplicateurs). */
        public boolean special() {
            return counts && step != null && step.isSpecial();
        }
    }

    /** Avancement du combo, pour l'indicateur à l'écran. */
    public record Progress(int steps, int done, boolean nextIsSpecial) {
    }

    // --- Quel combo ---

    /** Combo de cet objet s'il est activé (fichier de l'arme et rules.json). Il continue avec Better Combat, qui ne fait que porter les coups. */
    public static Optional<ComboDefinition> comboOf(ItemStack stack) {
        if (!WeaponData.rules().combosActive()) return Optional.empty();
        return WeaponProfiles.definition(stack).map(WeaponDefinition::combo).filter(combo -> combo.enabled() && !combo.steps().isEmpty());
    }

    /** Combo en vigueur pour ce joueur : celui de l'arme principale. Une arme ambidextre doit être tenue en paire. */
    public static Optional<ComboDefinition> activeCombo(Player player) {
        if (CombatCompat.isFakePlayer(player)) return Optional.empty();
        ItemStack main = player.getMainHandItem();
        return comboOf(main).filter(combo -> !DualWieldSystem.canDualWield(main) || holdsPair(player));
    }

    /** Le joueur tient le même type d'arme dans chaque main, en dual wield (pas d'arme à deux mains). */
    public static boolean holdsPair(Player player) {
        Optional<ResourceLocation> main = WeaponProfiles.id(player.getMainHandItem());
        return main.isPresent() && main.equals(WeaponProfiles.id(player.getOffhandItem())) && DualWieldSystem.holdsDualWeapons(player);
    }

    // --- Déroulement d'un coup (PlayerCombatMixin) ---

    /** Avant le coup : remet le combo à zéro si besoin et décide de l'étape. La force du coup est lue ici. */
    public static Hit beginHit(Player player, PlayerCombatState state) {
        float strength = player.getAttackStrengthScale(0.5F);
        Optional<ComboDefinition> active = activeCombo(player);
        if (active.isEmpty()) {
            state.resetCombo();
            state.gesture = ComboGesture.DEFAULT;
            return Hit.none(strength);
        }
        ComboDefinition combo = active.get();
        ResourceLocation type = WeaponProfiles.id(player.getMainHandItem()).orElseThrow();
        if (!type.equals(state.comboType) || player.level().getGameTime() > state.comboExpiresAt) state.comboStep = 0;
        state.comboType = type;

        int index = Math.floorMod(state.comboStep, combo.steps().size());
        ComboStep step = combo.steps().get(index);
        boolean counts = strength * PERCENT >= combo.minCharge();
        boolean bothHands = counts && step.bothHands() && holdsPair(player);
        // Better Combat joue ses propres animations.
        state.gesture = counts && !CombatCompat.betterCombat() ? step.gesture() : ComboGesture.DEFAULT;
        return new Hit(combo, type, step, index, counts, bothHands, strength);
    }

    /**
     * Coup spécial : le coup vanilla sur la cible, avec les dégâts et le recul du combo (bonus temporaires
     * sur les attributs du joueur), puis l'arme secondaire et la zone (côté serveur).
     *
     * @param vanillaAttack le Player.attack() d'origine
     */
    public static void performSpecialHit(Player player, Entity target, Hit hit, Runnable vanillaAttack) {
        PlayerCombatState state = PlayerCombatState.of(player);
        ComboStep step = hit.step();
        List<Runnable> undo = new ArrayList<>();
        double mainDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        // Deux armes à la fois : dégâts = moyenne des deux armes.
        double damage = hit.bothHands()
                ? (mainDamage + HandSwap.attributeWithHandsSwapped(player, Attributes.ATTACK_DAMAGE)) / 2.0D
                : mainDamage;
        addTemporaryModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_BONUS_ID,
                damage - mainDamage, AttributeModifier.Operation.ADD_VALUE, undo);
        addTemporaryModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_MULTIPLIER_ID,
                cappedMultiplier(hit.combo(), step, damage) - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, undo);
        addTemporaryModifier(player, Attributes.ATTACK_KNOCKBACK, KNOCKBACK_ID,
                step.knockback(), AttributeModifier.Operation.ADD_VALUE, undo);

        state.activeStep = step;
        try {
            vanillaAttack.run();
            if (state.attackLanded && player.level() instanceof ServerLevel level) {
                playStepEffects(level, player, step);
                DamageSource source = player.damageSources().playerAttack(player);
                if (hit.bothHands() && target instanceof LivingEntity living) applyOffhandWeapon(level, player, living, source);
                if (step.hasArea()) hitArea(level, player, target, hit, source);
                if (hit.bothHands()) player.getOffhandItem().hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
            }
        } finally {
            state.activeStep = null;
            undo.forEach(Runnable::run);
        }
    }

    /** Après le coup : fait avancer (ou remet à zéro) le combo, règle la recharge suivante, prévient le client. */
    public static void endHit(Player player, PlayerCombatState state, Hit hit, Entity target) {
        state.nextAttackDelay = 0.0F;
        if (hit.combo() == null) return;

        Level level = player.level();
        // Côté client, hurt() renvoie toujours faux : on prédit d'après la cible, et le serveur corrige ensuite.
        boolean landed = isCreature(target) && (level.isClientSide || state.attackLanded);
        if (landed) {
            if (hit.counts()) {
                if (!level.isClientSide) CombatAscendedApi.fireComboHit(player, player.getMainHandItem(), hit.step());
                state.comboStep = (hit.index() + 1) % hit.combo().steps().size();
                if (hit.special() && (hit.bothHands() || hit.step().cooldownMultiplier() != 1.0D)) {
                    state.nextAttackDelay = nextAttackDelay(player, hit);
                }
                // Better Combat anime lui-même les coups : pas de double mouvement de bras.
                if (hit.bothHands() && !CombatCompat.betterCombat()) {
                    state.dualSwingPending = true;
                    if (!level.isClientSide) Platform.get().sendToPlayersTracking(player, new DualSwingPayload(player.getId()));
                }
            } else {
                state.comboStep = 0;
            }
            state.comboExpiresAt = level.getGameTime() + Math.round(hit.combo().resetAfterSeconds() * TICKS_PER_SECOND);
        }

        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            Platform.get().sendToPlayer(serverPlayer, ComboSyncPayload.of(player, state));
        } else if (level.isClientSide && landed && hit.counts() && hit.combo().steps().get(state.comboStep).isSpecial()) {
            comboReadyFeedback(player);
        }
    }

    /** Une créature, ou la partie d'un boss à plusieurs corps (dragon). Un bateau ou un cadre ne font pas avancer le combo. */
    private static boolean isCreature(Entity target) {
        return target instanceof LivingEntity || Platform.get().isBossPart(target);
    }

    /** Recharge imposée par le dernier coup spécial, si le joueur tient toujours cette arme. */
    @Nullable
    public static Float attackDelayOverride(Player player, PlayerCombatState state) {
        if (state.nextAttackDelay <= 0.0F || !WeaponProfiles.id(player.getMainHandItem()).equals(Optional.ofNullable(state.comboType))) return null;
        return state.nextAttackDelay;
    }

    /** Vrai pendant un coup spécial en zone : le coup balayé vanilla est alors remplacé par la zone. */
    public static boolean isAreaAttackInProgress(Player player) {
        ComboStep step = PlayerCombatState.of(player).activeStep;
        return step != null && step.hasArea();
    }

    /** Geste à jouer pour ce joueur pendant son coup de bras en cours. */
    public static ComboGesture gestureOf(Player player) {
        return player.swinging ? PlayerCombatState.of(player).gesture : ComboGesture.DEFAULT;
    }

    /** Le coup de bras en cours anime les deux bras (coup à deux armes). */
    public static boolean isDualSwinging(Player player) {
        return player.swinging && PlayerCombatState.of(player).dualSwingActive;
    }

    /** Avancement du combo du joueur, ou null s'il n'a pas commencé (ou a expiré). */
    @Nullable
    public static Progress progress(Player player) {
        PlayerCombatState state = PlayerCombatState.of(player);
        Optional<ComboDefinition> combo = activeCombo(player);
        if (combo.isEmpty() || state.comboStep <= 0 || player.level().getGameTime() > state.comboExpiresAt) return null;
        if (!WeaponProfiles.id(player.getMainHandItem()).equals(Optional.ofNullable(state.comboType))) return null;
        List<ComboStep> steps = combo.get().steps();
        int done = Math.floorMod(state.comboStep, steps.size());
        return new Progress(steps.size(), done, steps.get(done).isSpecial());
    }

    // --- Coup en zone ---

    /**
     * Touche les ennemis de la zone (sauf la cible, déjà frappée par le coup vanilla), avec les mêmes règles
     * que le coup balayé vanilla : dégâts selon la charge et les enchantements, effets d'enchantement,
     * effets à l'impact de l'arme. Le recul pousse vers l'extérieur de la zone.
     */
    private static void hitArea(ServerLevel level, Player player, Entity primary, Hit hit, DamageSource source) {
        ComboStep step = hit.step();
        float strength = hit.strength();
        // Les bonus temporaires du combo (moyenne des deux armes, multiplicateur) sont encore appliqués.
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float charge = 0.2F + strength * strength * 0.8F;
        ItemStack weapon = player.getMainHandItem();

        PlayerCombatState state = PlayerCombatState.of(player);
        for (LivingEntity victim : areaTargets(player, primary, step)) {
            float enchanted = EnchantmentHelper.modifyDamage(level, weapon, victim, source, base);
            float amount = base * charge + (enchanted - base) * strength;
            if (step.knockback() > 0.0D) {
                victim.knockback(step.knockback() * KNOCKBACK_PER_LEVEL, player.getX() - victim.getX(), player.getZ() - victim.getZ());
            }
            if (victim.hurt(source, amount)) {
                state.swingVictims.add(victim.getId());
                EnchantmentHelper.doPostAttackEffects(level, victim, source);
                WeaponActions.applyOnHitEffects(weapon, victim, player);
                if (hit.bothHands()) applyOffhandWeapon(level, player, victim, source);
            }
        }
        spawnAreaParticles(level, player, step);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 0.8F);
    }

    /** Créatures dans la zone : à moins de radius blocs, dans l'angle devant le joueur, et qu'on a le droit de frapper. */
    private static List<LivingEntity> areaTargets(Player player, @Nullable Entity primary, ComboStep step) {
        double radius = effectiveRadius(player, step);
        Vec3 facing = horizontalFacing(player);
        double minDot = Math.cos(Math.toRadians(step.angle() / 2.0D));
        AABB zone = player.getBoundingBox().inflate(radius, AREA_HEIGHT, radius);

        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, zone)) {
            if (candidate == player || candidate == primary || !candidate.isAlive() || !canHit(player, candidate)) continue;
            Vec3 offset = candidate.position().subtract(player.position());
            Vec3 flat = new Vec3(offset.x, 0.0D, offset.z);
            if (flat.length() - candidate.getBbWidth() / 2.0D > radius) continue;
            if (step.angle() < 360.0D && flat.lengthSqr() > 1.0E-6D && flat.normalize().dot(facing) < minDot) continue;
            targets.add(candidate);
        }
        return targets;
    }

    /** Pas ses alliés, ses animaux apprivoisés, les porte-armures marqueurs, sa monture, ni les joueurs si le PvP est coupé. */
    private static boolean canHit(Player player, LivingEntity candidate) {
        if (candidate.isSpectator() || player.isAlliedTo(candidate)) return false;
        if (candidate instanceof ArmorStand stand && stand.isMarker()) return false;
        if (candidate instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())) return false;
        if (candidate instanceof Player other && !player.canHarmPlayer(other)) return false;
        return candidate != player.getVehicle() && !player.isPassengerOfSameVehicle(candidate);
    }

    /** Particules de balayage réparties sur l'arc de la zone. */
    private static void spawnAreaParticles(ServerLevel level, Player player, ComboStep step) {
        double angle = step.angle();
        int count = Math.max(3, (int) Math.round(angle / DEGREES_PER_PARTICLE));
        boolean fullCircle = angle >= 360.0D;
        double facingYaw = Math.toRadians(player.getYRot());
        double distance = step.radius() * 0.6D;
        for (int i = 0; i < count; i++) {
            double fraction = fullCircle ? (double) i / count : (double) i / (count - 1);
            double yaw = facingYaw + Math.toRadians(-angle / 2.0D + angle * fraction);
            double x = player.getX() - Math.sin(yaw) * distance;
            double z = player.getZ() + Math.cos(yaw) * distance;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, player.getY(0.5D), z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    // --- Outils ---

    /** Effets à l'impact et effets d'enchantement (Aura de feu...) de l'arme secondaire, pour un coup à deux armes. */
    private static void applyOffhandWeapon(ServerLevel level, Player player, LivingEntity victim, DamageSource source) {
        ItemStack offhand = player.getOffhandItem();
        WeaponActions.applyOnHitEffects(offhand, victim, player);
        EnchantmentHelper.doPostAttackEffectsWithItemSource(level, victim, source, offhand);
    }

    /**
     * Recharge du coup suivant un coup spécial : celle de l'arme principale (ou la moyenne des deux armes
     * pour un coup à deux armes) × cooldown_multiplier.
     */
    private static float nextAttackDelay(Player player, Hit hit) {
        double mainSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        float base;
        if (DualWieldSystem.isDualWielding(player)) {
            float mainDelay = DualWieldSystem.delayForSpeed(mainSpeed);
            base = hit.bothHands()
                    ? (mainDelay + DualWieldSystem.delayForSpeed(HandSwap.attackSpeedWithHandsSwapped(player))) / 2.0F
                    : mainDelay;
        } else {
            base = (float) (TICKS_PER_SECOND / mainSpeed);
        }
        return (float) (base * hit.step().cooldownMultiplier());
    }

    /** Côté client, avec Better Combat : le serveur annonce où en est le combo. */
    public static void onServerAdvance(Player player, PlayerCombatState state) {
        activeCombo(player).filter(combo -> combo.steps().get(Math.floorMod(state.comboStep, combo.steps().size())).isSpecial())
                .ifPresent(combo -> comboReadyFeedback(player));
    }

    /** Côté client : le prochain coup est le coup spécial. Petit son et étincelles devant le joueur. */
    private static void comboReadyFeedback(Player player) {
        Level level = player.level();
        level.playSound(player, player.getX(), player.getY(), player.getZ(), ModSounds.COMBO_READY.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(0.8D)).add(0.0D, -0.4D, 0.0D);
        for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.ENCHANTED_HIT,
                    front.x + (player.getRandom().nextDouble() - 0.5D) * 0.5D,
                    front.y + (player.getRandom().nextDouble() - 0.5D) * 0.5D,
                    front.z + (player.getRandom().nextDouble() - 0.5D) * 0.5D,
                    0.0D, 0.05D, 0.0D);
        }
    }

    /** La zone d'un objet détecté automatiquement ne dépasse pas sa portée ; un fichier d'arme, lui, décide librement. */
    private static double effectiveRadius(Player player, ComboStep step) {
        boolean detected = WeaponProfiles.of(player.getMainHandItem()).map(WeaponProfiles.Profile::detected).orElse(false);
        return detected ? Math.min(step.radius(), player.entityInteractionRange()) : step.radius();
    }

    /** Multiplicateur de ce coup, limité par {@code max_bonus_damage} : les armes très fortes ne font pas un coup spécial démesuré. */
    private static double cappedMultiplier(ComboDefinition combo, ComboStep step, double damage) {
        double multiplier = step.damageMultiplier();
        if (combo.maxBonusDamage() <= 0.0D || multiplier <= 1.0D || damage <= 0.0D) return multiplier;
        return Math.min(multiplier, 1.0D + combo.maxBonusDamage() / damage);
    }

    /** Son et particules propres à ce coup, s'il en a. */
    private static void playStepEffects(ServerLevel level, Player player, ComboStep step) {
        if (!step.sound().equals(DataField.NONE)) {
            BuiltInRegistries.SOUND_EVENT.getOptional(step.sound()).ifPresent(sound ->
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), 1.0F, 1.0F));
        }
        if (!step.particles().equals(DataField.NONE) && BuiltInRegistries.PARTICLE_TYPE.get(step.particles()) instanceof SimpleParticleType particle) {
            Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(1.2D)).add(0.0D, -0.3D, 0.0D);
            level.sendParticles(particle, front.x, front.y, front.z, STEP_PARTICLES, 0.3D, 0.3D, 0.3D, 0.05D);
        }
    }

    private static Vec3 horizontalFacing(Player player) {
        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    /** Ajoute un bonus d'attribut pour la durée du coup, et note comment le retirer. Rien si la valeur est nulle. */
    private static void addTemporaryModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount,
                                             AttributeModifier.Operation operation, List<Runnable> undo) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || amount == 0.0D) return;
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        undo.add(() -> instance.removeModifier(id));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CombatAscended.MODID, path);
    }
}
