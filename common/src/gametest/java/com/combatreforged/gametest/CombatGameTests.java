package com.combatreforged.gametest;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import com.combatreforged.data.DataResults;
import com.combatreforged.CombatReforged;
import com.combatreforged.client.TooltipLayout;
import com.combatreforged.combat.ComboSystem;
import com.combatreforged.combat.DashAbility;
import com.combatreforged.combat.DualWieldSystem;
import com.combatreforged.combat.ParrySystem;
import com.combatreforged.combat.ModEnchantments;
import com.combatreforged.combat.MobCombat;
import com.combatreforged.combat.MobGuardGoal;
import com.combatreforged.combat.PlayerCombatState;
import com.combatreforged.combat.ShieldSystem;
import com.combatreforged.combat.TwoHandedSystem;
import com.combatreforged.combat.WeaponProfiles;
import com.combatreforged.combat.WeaponActions;
import com.combatreforged.combat.AdoptedItemEvents;
import com.combatreforged.combat.WeaponDetector;
import com.combatreforged.data.DetectionRules;
import com.combatreforged.compat.CombatCompat;
import com.combatreforged.config.EditorFiles;
import com.combatreforged.config.TooltipCategory;
import com.combatreforged.config.TooltipPlacement;
import com.combatreforged.data.ComboDefinition;
import com.combatreforged.data.ComboStep;
import com.combatreforged.data.DashDefinition;
import com.combatreforged.data.DashDirection;
import com.combatreforged.data.DataProblem;
import com.combatreforged.data.FieldSet;
import com.combatreforged.data.GearDefinition;
import com.combatreforged.data.JsonUtil;
import com.combatreforged.data.OnHitEffect;
import com.combatreforged.data.ParryDefinition;
import com.combatreforged.data.RulesDefinition;
import com.combatreforged.data.WeaponAbility;
import com.combatreforged.data.WeaponData;
import com.combatreforged.data.WeaponDataLoader;
import com.combatreforged.data.WeaponDefinition;
import com.combatreforged.data.JsonTree;
import com.combatreforged.editor.EditorDocument;
import com.betterweaponry.entity.ShurikenEntity;
import com.combatreforged.editor.EditorService;
import com.combatreforged.editor.EditorTarget;
import com.betterweaponry.item.ModShieldDecorationRecipe;
import com.betterweaponry.item.RangedWeapon;
import com.betterweaponry.item.ShurikenItem;
import com.betterweaponry.item.VanillaGear;
import com.betterweaponry.item.WeaponBowItem;
import com.betterweaponry.item.WeaponCrossbowItem;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponShieldItem;
import com.combatreforged.mixin.LivingEntityAccessor;
import com.combatreforged.mixin.MobAccessor;
import com.betterweaponry.item.WeaponType;
import com.combatreforged.registry.ModAttributes;
import com.combatreforged.tutorial.TutorialScene;
import com.combatreforged.tutorial.TutorialScenes;
import com.betterweaponry.registry.ModCreativeTabs;
import com.combatreforged.registry.ModStats;
import com.betterweaponry.registry.ModEffects;
import com.betterweaponry.registry.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.locale.Language;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.combatreforged.tutorial.TutorialHints;
import com.combatreforged.platform.Platform;
import com.combatreforged.platform.Registered;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Tests automatiques du combat, lancés par {@code gradlew runGameTestServer} (ou {@code /test runall} en jeu, en dev).
 * Ils lisent les stats actuelles des armes : ils restent valables si les valeurs par défaut changent.
 * Ils ne tournent jamais dans une partie normale (les GameTests ne sont actifs qu'en développement).
 */
//@LOADER_TEST_ANNOTATIONS@
public final class CombatGameTests {
    /** Comportement de ce type d'arme de Better Weaponry, tel que Combat Reforged l'a chargé. */
    private static WeaponDefinition behavior(WeaponType type) {
        return WeaponData.definition(new ResourceLocation("better_weaponry", type.id()));
    }

    /** Structure vide (data/combat_reforged/structure/empty.nbt) qui sert de terrain. */
    private static final String ARENA = "@ARENA@";
    private static final double EPSILON = 0.01D;
    private static final float TEST_HEALTH = 100.0F;
    private static final float RAW_DAMAGE = 10.0F;
    private static final int VANILLA_INVULNERABILITY_TICKS = 20;

    public CombatGameTests() {
    }

    /** Un objet enchanté avec Better Weaponry 2.0 garde ses enchantements, sous leur nouvel identifiant. */
    @GameTest(template = ARENA)
    public static void oldEnchantmentsAreRenamed(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.get(WeaponType.KATANA, WeaponMaterials.IRON).get());
        stack.enchant(enchantment(helper, ModEnchantments.RIPOSTE), 2);
        net.minecraft.nbt.CompoundTag saved = stack.save(new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(ItemStack.of(saved.copy()).getEnchantmentTags().toString().contains("combat_reforged:riposte"), "the current id is kept");
        String legacy = saved.toString().replace("combat_reforged:riposte", "better_weaponry:riposte");
        try {
            ItemStack loaded = ItemStack.of(net.minecraft.nbt.TagParser.parseTag(legacy));
            helper.assertTrue(ModEnchantments.level(helper.getLevel(), loaded, ModEnchantments.RIPOSTE) == 2, "the old enchantment should still work");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("invalid test data: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    /** Les six épées vanilla parent, et ne reçoivent rien d'autre. */
    @GameTest(template = ARENA)
    public static void vanillaSwordsOnlyParry(GameTestHelper helper) {
        for (Item sword : List.of(Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD)) {
            WeaponDefinition definition = WeaponProfiles.definition(new ItemStack(sword)).orElseThrow();
            helper.assertTrue(definition.ability() == WeaponAbility.PARRY, "a vanilla sword should parry");
            helper.assertFalse(definition.combo().enabled(), "a vanilla sword has no combo");
            helper.assertFalse(definition.twoHanded() || definition.dualWielded(), "a vanilla sword keeps both hands free");
        }
        helper.assertTrue(WeaponProfiles.of(new ItemStack(Items.IRON_AXE)).isEmpty(), "other vanilla weapons are left alone");
        helper.succeed();
    }

    /** Les noms sont découpés en mots : "sai" n'est pas dans "mosaic", et le mot le plus précis l'emporte. */
    @GameTest(template = ARENA)
    public static void detectionReadsWords(GameTestHelper helper) {
        DetectionRules rules = WeaponData.current().detection();
        helper.assertTrue(archetype(rules, "iron_longsword").equals("longsword"), "longsword");
        helper.assertTrue(archetype(rules, "great_sword_of_fire").equals("greatsword"), "two words");
        helper.assertTrue(archetype(rules, "war_hammer").equals("warhammer"), "the most precise word wins");
        helper.assertTrue(archetype(rules, "flameblade").equals("sword"), "a word can end a longer one");
        helper.assertTrue(archetype(rules, "bamboo_mosaic").isEmpty(), "sai is not in mosaic");
        helper.assertTrue(archetype(rules, "iron_pickaxe").isEmpty(), "axe is not in pickaxe");
        helper.succeed();
    }

    private static String archetype(DetectionRules rules, String path) {
        ResourceLocation found = WeaponDetector.classify(path, rules);
        return found == null ? "" : found.getPath();
    }

    /** Avec Better Combat, quatre systèmes passent à sa charge et tous les autres restent actifs. */
    @GameTest(template = ARENA)
    public static void betterCombatManagesCommonSystems(GameTestHelper helper) {
        CombatCompat.simulateForTests(true);
        try {
            RulesDefinition rules = WeaponData.rules();
            helper.assertFalse(rules.reachActive() || rules.sweepActive() || rules.twoHandedActive() || rules.dualWieldActive(), "Better Combat manages these");
            helper.assertTrue(rules.combosActive() && rules.parryActive() && rules.perfectParryActive() && rules.guardBreakActive() && rules.onHitEffectsActive()
                    && rules.armorPenetrationActive() && rules.dashActive() && rules.shieldDisablingActive() && rules.mobBehaviorsActive() && rules.pvpHitCapActive(), "the rest stays active");
        } finally {
            CombatCompat.simulateForTests(false);
        }
        helper.succeed();
    }

    /** Better Combat appelle attack() pour chaque cible : une frappe ne compte qu'une fois dans le combo. */
    @GameTest(template = ARENA)
    public static void betterCombatSwingCountsOnce(GameTestHelper helper) {
        Player player = makeMockPlayer(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.get(WeaponType.KATANA, WeaponMaterials.IRON).get()));
        CombatCompat.simulateForTests(true);
        try {
            Zombie first = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
            Zombie second = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
            first.setHealth(first.getMaxHealth());
            second.setHealth(second.getMaxHealth());
            ((LivingEntityAccessor) player).combatreforged$setAttackStrengthTicker(100);
            player.attack(first);
            player.attack(second);
            helper.assertTrue(PlayerCombatState.of(player).comboStep == 1, "one swing is one step, got " + PlayerCombatState.of(player).comboStep);
        } finally {
            CombatCompat.simulateForTests(false);
        }
        helper.succeed();
    }

    /** Une arme à deux mains pare même si Better Combat gère le verrouillage et qu'un objet est en main secondaire. */
    @GameTest(template = ARENA)
    public static void betterCombatKeepsTwoHandedParry(GameTestHelper helper) {
        Player player = makeMockPlayer(helper, GameType.SURVIVAL);
        ItemStack katana = new ItemStack(ModItems.get(WeaponType.KATANA, WeaponMaterials.IRON).get());
        player.setItemInHand(InteractionHand.MAIN_HAND, katana);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
        CombatCompat.simulateForTests(true);
        try {
            helper.assertTrue(ParrySystem.canParry(player, InteractionHand.MAIN_HAND, katana), "the file of the weapon decides");
        } finally {
            CombatCompat.simulateForTests(false);
        }
        helper.succeed();
    }

    /** Les armes de Better Weaponry, chargées avec ce mod, se comportent exactement comme en 2.0.1. */
    @GameTest(template = ARENA)
    public static void parityWithBetterWeaponry(GameTestHelper helper) {
        try {
            String reference = Files.readString(Platform.get().gameDir().resolve("parity-reference.json"));
            List<String> differences = ParitySnapshot.compareWithReference(reference);
            helper.assertTrue(differences.isEmpty(), differences.size() + " differences, first ones: " + differences.subList(0, Math.min(8, differences.size())));
        } catch (java.io.IOException e) {
            helper.fail("could not compare with the reference: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    /** Avec X % de pénétration, l'armure retire X % de moins de dégâts qu'avec une arme sans pénétration. */
    @GameTest(template = ARENA)
    public static void armorPenetrationReducesArmor(GameTestHelper helper) {
        WeaponType piercing = WeaponType.HAMMER;
        for (WeaponType type : WeaponType.values()) {
            if (behavior(type).armorPenetration() > behavior(piercing).armorPenetration()) piercing = type;
        }
        double penetration = behavior(piercing).armorPenetration() / 100.0D;

        Pig withoutPenetration = armoredPig(helper, new BlockPos(1, 1, 1));
        Pig withPenetration = armoredPig(helper, new BlockPos(3, 1, 3));
        Player unarmed = player(helper, ItemStack.EMPTY, ItemStack.EMPTY);
        Player armed = player(helper, weapon(piercing), ItemStack.EMPTY);
        withoutPenetration.hurt(unarmed.damageSources().playerAttack(unarmed), RAW_DAMAGE);
        withPenetration.hurt(armed.damageSources().playerAttack(armed), RAW_DAMAGE);

        double armorReduction = RAW_DAMAGE - (TEST_HEALTH - withoutPenetration.getHealth());
        double reducedArmorReduction = RAW_DAMAGE - (TEST_HEALTH - withPenetration.getHealth());
        helper.assertTrue(armorReduction > 1.0D, "the armor should absorb damage, absorbed " + armorReduction);
        boolean apothic = Platform.get().isModLoaded("attributeslib");
        helper.assertTrue(ModAttributes.armorPenetration().equals(ModAttributes.ARMOR_PENETRATION.get()) != apothic, "the penetration attribute should depend on Apothic Attributes");
        double expected = armorReduction * (1.0D - penetration);
        // Avec Apothic Attributes, c'est son calcul de l'armure qui s'applique : l'armure protège seulement moins.
        helper.assertTrue(apothic ? reducedArmorReduction < armorReduction : Math.abs(reducedArmorReduction - expected) < EPSILON,
                piercing.id() + " (" + penetration * 100 + "%): armor absorbed " + reducedArmorReduction + " instead of " + expected);
        helper.succeed();
    }

    /** Chaque effet sûr (chance 100 %) est appliqué à la cible, au bon niveau. */
    @GameTest(template = ARENA)
    public static void onHitEffectsAreApplied(GameTestHelper helper) {
        int index = 0;
        for (WeaponType type : WeaponType.values()) {
            for (OnHitEffect effect : behavior(type).onHitEffects()) {
                if (effect.chance() < 100.0D || effect.mobEffect().isEmpty()) continue;
                Pig target = armoredPig(helper, new BlockPos(index % 5, 1, (index / 5) % 5));
                ItemStack stack = weapon(type);
                Player attacker = player(helper, stack, ItemStack.EMPTY);
                WeaponActions.applyOnHitEffects(stack, target, attacker);

                MobEffectInstance applied = target.getEffect(effect.mobEffect().get());
                helper.assertTrue(applied != null, type.id() + " should give " + effect.effect());
                helper.assertTrue(applied.getAmplifier() == effect.level() - 1, type.id() + ": wrong level for " + effect.effect());
                index++;
            }
        }
        helper.succeed();
    }

    /** En dual wield, la cible n'est protégée que le temps prévu par rules.json ; avec une seule arme, comme en vanilla. */
    @GameTest(template = ARENA)
    public static void dualWieldInvulnerability(GameTestHelper helper) {
        Player dualWielder = player(helper, weapon(WeaponType.DAGGER), weapon(WeaponType.DAGGER));
        Player singleWeapon = player(helper, weapon(WeaponType.DAGGER), ItemStack.EMPTY);
        assertModTagsLoaded(helper);
        helper.assertTrue(DualWieldSystem.isDualWielding(dualWielder), "two daggers should count as dual wielding");

        Pig dualTarget = armoredPig(helper, new BlockPos(1, 1, 1));
        Pig singleTarget = armoredPig(helper, new BlockPos(3, 1, 3));
        dualTarget.hurt(dualWielder.damageSources().playerAttack(dualWielder), 2.0F);
        singleTarget.hurt(singleWeapon.damageSources().playerAttack(singleWeapon), 2.0F);

        int expected = 10 + (int) Math.round(WeaponData.rules().dualWieldInvulnerabilitySeconds() * 20.0D);
        helper.assertTrue(dualTarget.invulnerableTime == expected, "dual wield: invulnerability " + dualTarget.invulnerableTime + " instead of " + expected);
        helper.assertTrue(singleTarget.invulnerableTime == VANILLA_INVULNERABILITY_TICKS, "single weapon: invulnerability " + singleTarget.invulnerableTime);
        helper.succeed();
    }

    /** Zone de balayage agrandie du rayon configuré, et désactivation des boucliers selon les données. */
    @GameTest(template = ARENA)
    public static void sweepZoneAndShields(GameTestHelper helper) {
        Pig target = armoredPig(helper, new BlockPos(2, 1, 2));
        Player attacker = player(helper, ItemStack.EMPTY, ItemStack.EMPTY);
        ItemStack shield = new ItemStack(Items.SHIELD);
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = behavior(type);
            ItemStack stack = weapon(type);
            WeaponItem item = (WeaponItem) stack.getItem();

            AABB zone = WeaponActions.sweepZone(WeaponProfiles.definition(stack).orElseThrow(), target);
            double expectedWidth = target.getBoundingBox().getXsize() + 2.0D * definition.sweep().radius();
            helper.assertTrue(Math.abs(zone.getXsize() - expectedWidth) < EPSILON, type.id() + ": sweep zone width " + zone.getXsize());
            attacker.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.assertTrue(attacker.canDisableShield() == definition.disablesShield(),
                    type.id() + ": disables_shield should be " + definition.disablesShield());
        }
        helper.succeed();
    }

    /**
     * Parade : un coup de face est réduit de damage_reduction % et use l'arme ; un coup dans le dos passe en entier.
     * Testé sur chaque arme qui pare.
     */
    @GameTest(template = ARENA)
    public static void parryBlocksFrontalHits(GameTestHelper helper) {
        int tested = 0;
        for (WeaponType type : WeaponType.values()) {
            ItemStack stack = weapon(type);
            Optional<ParryDefinition> settings = ParrySystem.parryOf(stack);
            if (settings.isEmpty()) continue;
            ParryDefinition parry = settings.get();

            // Le défenseur regarde vers le sud (+Z) : devant lui = Z plus grand.
            Player defender = player(helper, stack, ItemStack.EMPTY);
            Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
            defender.setPos(position);
            defender.setYRot(0.0F);
            defender.setYHeadRot(0.0F);
            setBase(defender.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
            defender.setHealth(TEST_HEALTH);
            helper.assertTrue(AdoptedItemEvents.onRightClickItem(defender, InteractionHand.MAIN_HAND, stack, helper.getLevel())
                    && ParrySystem.isParrying(defender), type.id() + ": a right click should start the parry");
            PlayerCombatState.of(defender).perfectParryUntil = 0L;  // fenêtre de parade parfaite passée

            defender.hurt(defender.damageSources().playerAttack(mockPlayerAt(helper, position.add(0.0D, 0.0D, 2.0D))), RAW_DAMAGE);
            double blocked = RAW_DAMAGE * parry.damageReduction() / 100.0D;
            double frontDamage = TEST_HEALTH - defender.getHealth();
            helper.assertTrue(Math.abs(frontDamage - (RAW_DAMAGE - blocked)) < EPSILON,
                    type.id() + ": a hit from the front did " + frontDamage + " damage instead of " + (RAW_DAMAGE - blocked));
            int expectedWear = (int) Math.floor(RAW_DAMAGE * parry.durabilityPerDamage());
            helper.assertTrue(stack.getDamageValue() == expectedWear,
                    type.id() + ": the parry wore the weapon by " + stack.getDamageValue() + " instead of " + expectedWear);

            defender.invulnerableTime = 0;
            defender.setHealth(TEST_HEALTH);
            defender.hurt(defender.damageSources().playerAttack(mockPlayerAt(helper, position.add(0.0D, 0.0D, -2.0D))), RAW_DAMAGE);
            double backDamage = TEST_HEALTH - defender.getHealth();
            helper.assertTrue(Math.abs(backDamage - RAW_DAMAGE) < EPSILON,
                    type.id() + ": a hit in the back did " + backDamage + " damage instead of " + RAW_DAMAGE);
            tested++;
        }
        helper.assertTrue(tested > 0, "no weapon has the parry ability");
        helper.succeed();
    }

    /** Usure à la parade : le plancher de la moitié des dégâts du coup ; partagée quand on pare avec une arme dans chaque main. */
    @GameTest(template = ARENA)
    public static void parryWear(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        Player attacker = mockPlayerAt(helper, position.add(0.0D, 0.0D, 2.0D));
        ItemStack main = weapon(WeaponType.SAI);
        ItemStack off = weapon(WeaponType.SAI);
        Player pair = player(helper, main, off);
        pair.setPos(position);
        pair.setYRot(0.0F);
        pair.setYHeadRot(0.0F);
        setBase(pair.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        pair.setHealth(TEST_HEALTH);
        startParry(helper, pair, main);
        PlayerCombatState.of(pair).perfectParryUntil = 0L;

        pair.hurt(pair.damageSources().playerAttack(attacker), 7.0F);
        helper.assertTrue(main.getDamageValue() == 2 && off.getDamageValue() == 1,
                "7 damage should cost 3 durability shared 2 and 1, got " + main.getDamageValue() + " and " + off.getDamageValue());
        pair.invulnerableTime = 0;
        pair.hurt(pair.damageSources().playerAttack(attacker), 1.0F);
        helper.assertTrue(main.getDamageValue() == 2 && off.getDamageValue() == 1, "a hit of 1 damage should cost nothing");

        ItemStack alone = weapon(WeaponType.SAI);
        Player single = parrier(helper, alone, position);
        startParry(helper, single, alone);
        PlayerCombatState.of(single).perfectParryUntil = 0L;
        single.hurt(single.damageSources().playerAttack(attacker), 7.0F);
        helper.assertTrue(alone.getDamageValue() == 3, "a lone weapon should take all the wear, got " + alone.getDamageValue());
        helper.succeed();
    }

    /**
     * Quand le clic droit lance une parade : arme qui pare en main principale, main secondaire vide,
     * arme à deux mains, ou arme ambidextre dans chaque main. Un bouclier ou une torche
     * en main secondaire gardent leur action ; une arme tenue en main secondaire ne pare jamais.
     */
    @GameTest(template = ARENA)
    public static void parryConditions(GameTestHelper helper) {
        assertModTagsLoaded(helper);
        ItemStack shield = new ItemStack(Items.SHIELD);
        ItemStack torch = new ItemStack(Items.TORCH);
        ItemStack dagger = weapon(WeaponType.DAGGER);
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = behavior(type);
            boolean parries = definition.ability() == WeaponAbility.PARRY && WeaponData.rules().parryEnabled();
            boolean twoHanded = definition.twoHanded();

            expectParry(helper, type, ItemStack.EMPTY, parries);
            expectParry(helper, type, shield, parries && twoHanded);
            expectParry(helper, type, torch, parries && twoHanded);
            expectParry(helper, type, dagger, parries && (twoHanded || definition.dualWielded()));
            expectParry(helper, type, weapon(type), parries && (twoHanded || definition.dualWielded()));

            // Avec une arme ambidextre dans chaque main (saïs), les deux armes prennent la pose de parade.
            ItemStack dualMain = weapon(type);
            Player dualParrier = player(helper, dualMain, dagger.copy());
            AdoptedItemEvents.onRightClickItem(dualParrier, InteractionHand.MAIN_HAND, dualMain, helper.getLevel());
            boolean bothHands = parries && !twoHanded && definition.dualWielded();
            helper.assertTrue(ParrySystem.isParryingWithBothHands(dualParrier) == bothHands,
                    type.id() + " with a dagger in the off hand: both hands should " + (bothHands ? "" : "not ") + "parry");

            ItemStack offhandWeapon = weapon(type);
            Player offhandOnly = player(helper, ItemStack.EMPTY, offhandWeapon);
            helper.assertFalse(AdoptedItemEvents.onRightClickItem(offhandOnly, InteractionHand.OFF_HAND, offhandWeapon, helper.getLevel()),
                    type.id() + " held in the off hand must not parry");
        }
        helper.succeed();
    }

    /** Direction du dash : vers le regard ("look") ou vers les touches de déplacement ("input"), et vitesse de départ. */
    @GameTest(template = ARENA)
    public static void dashDirection(GameTestHelper helper) {
        Player player = mockPlayerAt(helper, helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D)));
        player.setYRot(0.0F);  // regarde vers le sud (+Z)
        player.xxa = 1.0F;     // touche "gauche" : vers l'est (+X) quand on regarde au sud
        player.zza = 0.0F;
        assertDirection(helper, DashAbility.direction(player, DashDirection.LOOK), new Vec3(0.0D, 0.0D, 1.0D), "look");
        assertDirection(helper, DashAbility.direction(player, DashDirection.INPUT), new Vec3(1.0D, 0.0D, 0.0D), "input, left key");
        player.xxa = 0.0F;
        assertDirection(helper, DashAbility.direction(player, DashDirection.INPUT), new Vec3(0.0D, 0.0D, 1.0D), "input, no key");

        player.setOnGround(true);
        DashDefinition dash = new DashDefinition(4.0D, 1.0D, DashDirection.LOOK, false, 0.0D);
        DashAbility.launch(player, dash);
        Vec3 motion = player.getDeltaMovement();
        double expectedSpeed = DashAbility.initialSpeed(dash.distance());
        helper.assertTrue(Math.abs(motion.horizontalDistance() - expectedSpeed) < EPSILON && motion.z > 0.0D && motion.y > 0.0D,
                "launch: motion " + motion + ", expected a speed of " + expectedSpeed + " toward +Z with a small hop");
        helper.succeed();
    }

    /**
     * Combos d'une arme seule (faux, katana, hache de guerre...) : les coups normaux ne touchent que la cible ;
     * le coup en zone touche aussi l'ennemi proche, mais ni l'ennemi trop loin ni celui hors de l'angle ;
     * la recharge suivante est allongée ; un coup peu chargé remet le combo à zéro.
     */
    @GameTest(template = ARENA)
    public static void comboAreaFinisher(GameTestHelper helper) {
        int tested = 0;
        for (WeaponType type : WeaponType.values()) {
            ComboDefinition combo = behavior(type).combo();
            int finisher = firstStep(combo, step -> step.hasArea() && !step.bothHands());
            if (!combo.enabled() || behavior(type).dualWielded() || finisher < 0) continue;
            ComboStep area = combo.steps().get(finisher);

            // Le joueur est au bord de l'arène et regarde vers le sud (+Z).
            Vec3 origin = new Vec3(2.5D, 1.0D, 0.5D);
            Player player = player(helper, weapon(type), ItemStack.EMPTY);
            player.setPos(helper.absoluteVec(origin));
            player.setYRot(0.0F);
            player.setYHeadRot(0.0F);
            double sideAngle = Math.toRadians(Math.min(20.0D, area.angle() / 4.0D));
            double sideDistance = area.radius() * 0.7D;
            Pig target = pigAt(helper, origin.add(0.0D, 0.0D, 1.5D));
            Pig nearby = pigAt(helper, origin.add(Math.sin(sideAngle) * sideDistance, 0.0D, Math.cos(sideAngle) * sideDistance));
            Pig tooFar = pigAt(helper, origin.add(0.0D, 0.0D, area.radius() + 1.2D));
            Pig behind = pigAt(helper, origin.add(0.0D, 0.0D, -1.5D));
            List<Pig> pigs = List.of(target, nearby, tooFar, behind);

            for (int hit = 0; hit <= finisher; hit++) {
                ComboStep step = combo.steps().get(hit);
                pigs.forEach(CombatGameTests::heal);
                fullyCharge(player);
                player.attack(target);
                helper.assertTrue(isHurt(target), type.id() + " hit " + (hit + 1) + ": the target should be hurt");
                helper.assertTrue(isHurt(nearby) == step.hasArea(),
                        type.id() + " hit " + (hit + 1) + ": the nearby pig should " + (step.hasArea() ? "" : "not ") + "be hurt");
                helper.assertFalse(isHurt(tooFar), type.id() + " hit " + (hit + 1) + ": a pig out of the radius was hurt");
                if (step.angle() < 360.0D) {
                    helper.assertFalse(isHurt(behind), type.id() + " hit " + (hit + 1) + ": a pig out of the angle was hurt");
                }
            }

            if (area.cooldownMultiplier() != 1.0D) {
                double expectedDelay = 20.0D / player.getAttributeValue(Attributes.ATTACK_SPEED) * area.cooldownMultiplier();
                helper.assertTrue(Math.abs(player.getCurrentItemAttackStrengthDelay() - expectedDelay) < EPSILON,
                        type.id() + ": cooldown after the finisher " + player.getCurrentItemAttackStrengthDelay() + " instead of " + expectedDelay);
            }
            PlayerCombatState state = PlayerCombatState.of(player);
            helper.assertTrue(state.comboStep == (finisher + 1) % combo.steps().size(), type.id() + ": wrong combo step " + state.comboStep);

            // Un coup peu chargé remet le combo à zéro.
            pigs.forEach(CombatGameTests::heal);
            fullyCharge(player);
            player.attack(target);
            target.invulnerableTime = 0;
            ((LivingEntityAccessor) player).combatreforged$setAttackStrengthTicker(0);
            player.attack(target);
            helper.assertTrue(state.comboStep == 0, type.id() + ": a weak hit should reset the combo, step " + state.comboStep);

            pigs.forEach(Pig::discard);
            tested++;
        }
        helper.assertTrue(tested > 0, "no single-weapon combo with an area hit");
        helper.succeed();
    }

    /** PvP : après l'armure, un coup au corps à corps entre joueurs est plafonné ; les autres dégâts et les autres cibles ne le sont pas. */
    @GameTest(template = ARENA)
    public static void pvpHitCap(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        Player attacker = mockPlayerAt(helper, position.add(0.0D, 0.0D, 2.0D));
        Player defender = mockPlayerAt(helper, position);
        setBase(defender.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        defender.setHealth(TEST_HEALTH);
        double cap = TEST_HEALTH * WeaponData.rules().pvpMaxHitPercent() / 100.0D;

        defender.hurt(defender.damageSources().playerAttack(attacker), 1000.0F);
        helper.assertTrue(Math.abs(TEST_HEALTH - defender.getHealth() - cap) < EPSILON, "the hit should be capped at " + cap + ", took " + (TEST_HEALTH - defender.getHealth()));

        defender.setHealth(TEST_HEALTH);
        defender.invulnerableTime = 0;
        defender.hurt(defender.damageSources().playerAttack(attacker), RAW_DAMAGE);
        helper.assertTrue(Math.abs(TEST_HEALTH - defender.getHealth() - RAW_DAMAGE) < EPSILON, "a small hit should not change");

        defender.setHealth(TEST_HEALTH);
        defender.invulnerableTime = 0;
        defender.hurt(defender.damageSources().generic(), 90.0F);
        helper.assertTrue(Math.abs(TEST_HEALTH - defender.getHealth() - 90.0D) < EPSILON, "other damage should not be capped");

        Pig pig = pigAt(helper, new Vec3(1.5D, 1.0D, 1.5D));
        pig.hurt(pig.damageSources().playerAttack(attacker), 90.0F);
        helper.assertTrue(Math.abs(TEST_HEALTH - pig.getHealth() - 90.0D) < EPSILON, "a mob should not be protected by the cap");
        helper.succeed();
    }

    /** Joueur factice à 100 PV, regardant vers le sud (+Z), qui tient cette arme. */
    private static Player parrier(GameTestHelper helper, ItemStack stack, Vec3 position) {
        Player defender = player(helper, stack, ItemStack.EMPTY);
        defender.setPos(position);
        defender.setYRot(0.0F);
        defender.setYHeadRot(0.0F);
        setBase(defender.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        defender.setHealth(TEST_HEALTH);
        return defender;
    }

    private static void startParry(GameTestHelper helper, Player player, ItemStack stack) {
        helper.assertTrue(AdoptedItemEvents.onRightClickItem(player, InteractionHand.MAIN_HAND, stack, helper.getLevel()), "the parry should start");
    }

    /**
     * Rangement du tooltip : deux mains juste au-dessus du titre des stats sans indentation, portée placée avant les dégâts,
     * détails remplacés par l'invite tant que la touche n'est pas maintenue, couleur appliquée, catégorie cachée absente.
     */
    @GameTest(template = ARENA)
    public static void tooltipLayout(GameTestHelper helper) {
        TooltipLayout.Settings defaults = new TooltipLayout.Settings(TooltipCategory::defaultPlacement, TooltipCategory::defaultColor);
        Component hint = Component.literal("hint");

        List<Component> closed = arrangedTooltip(defaults, false, hint);
        helper.assertTrue(texts(closed).equals(List.of("Weapon", "", "two", "When in Main Hand:",
                " reach", " Attack Damage", " Attack Speed", "hint", "ID")),
                "wrong layout without the key: " + texts(closed));
        helper.assertTrue(closed.get(2).getStyle().getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY)),
                "the hands line should be dark gray");
        helper.assertTrue(closed.get(4).getStyle().getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN)),
                "the stats should be dark green");

        List<Component> open = arrangedTooltip(defaults, true, hint);
        helper.assertTrue(texts(open).equals(List.of("Weapon", "", "two", "When in Main Hand:",
                " reach", " Attack Damage", " Attack Speed", " parry", "ID")),
                "wrong layout with the key: " + texts(open));

        for (TooltipCategory category : List.of(TooltipCategory.SWEEP, TooltipCategory.SHIELD_DISABLING, TooltipCategory.PROJECTILE_SPEED)) {
            helper.assertTrue(category.defaultPlacement() == TooltipPlacement.HIDDEN, category.id() + " should be hidden by default");
        }

        // Tout caché : le titre des stats et sa ligne vide disparaissent aussi.
        TooltipLayout.Settings hidden = new TooltipLayout.Settings(category -> TooltipPlacement.HIDDEN, TooltipCategory::defaultColor);
        List<Component> empty = arrangedTooltip(hidden, true, hint);
        helper.assertTrue(texts(empty).equals(List.of("Weapon", "ID")), "wrong layout with everything hidden: " + texts(empty));
        helper.succeed();
    }

    private static List<Component> arrangedTooltip(TooltipLayout.Settings settings, boolean detailsVisible, Component hint) {
        List<Component> tooltip = new ArrayList<>(List.of(Component.literal("Weapon"), Component.empty(),
                Component.translatable("item.modifiers.mainhand"),
                Component.literal(" ").append(Component.translatable("attribute.name.generic.attack_damage")),
                Component.literal(" ").append(Component.translatable("attribute.name.generic.attack_speed")),
                Component.literal("ID")));
        Map<TooltipCategory, List<Component>> lines = new EnumMap<>(TooltipCategory.class);
        lines.put(TooltipCategory.DAMAGE, TooltipLayout.takeLines(tooltip, "attack_damage"));
        lines.put(TooltipCategory.SPEED, TooltipLayout.takeLines(tooltip, "attack_speed"));
        lines.put(TooltipCategory.HANDS, List.of(Component.literal(" ").append("two")));
        lines.put(TooltipCategory.REACH, List.of(Component.literal(" ").append("reach")));
        lines.put(TooltipCategory.ABILITY, List.of(Component.literal(" ").append("parry")));
        TooltipLayout.arrange(tooltip, lines, settings, detailsVisible, hint);
        return tooltip;
    }

    private static List<String> texts(List<Component> tooltip) {
        return tooltip.stream().map(Component::getString).toList();
    }

    /** Parade parfaite au bouclier : pas d'usure, pas de garde brisée, recul de l'attaquant, flèche renvoyée vers le tireur. */
    @GameTest(template = ARENA)
    public static void shieldPerfectParry(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        Vec3 front = position.add(0.0D, 0.0D, 2.0D);
        ItemStack shield = new ItemStack(ModItems.shield(WeaponMaterials.IRON).get());
        Player defender = shieldBearer(helper, shield, position);
        PlayerCombatState state = PlayerCombatState.of(defender);
        Player axeUser = player(helper, new ItemStack(Items.IRON_AXE), ItemStack.EMPTY);
        axeUser.setPos(front);

        helper.assertTrue(defender.isBlocking() && ParrySystem.isInPerfectWindow(defender), "the window should be open");
        defender.hurt(defender.damageSources().playerAttack(axeUser), RAW_DAMAGE);
        helper.assertTrue(defender.getHealth() == TEST_HEALTH && shield.getDamageValue() == 0, "a perfect parry should not hurt nor wear the shield");
        helper.assertTrue(defender.isBlocking(), "a perfect parry should not be broken by an axe");
        helper.assertTrue(axeUser.getDeltaMovement().horizontalDistance() > 0.0D, "the attacker should be pushed back");
        helper.assertTrue(state.perfectBonus > 0.0D, "a perfect parry should give the damage bonus");

        // Hors de la fenêtre : usure et garde brisée, comme en vanilla.
        state.perfectParryUntil = 0L;
        defender.invulnerableTime = 0;
        defender.hurt(defender.damageSources().playerAttack(axeUser), RAW_DAMAGE);
        helper.assertTrue(shield.getDamageValue() > 0, "a normal block should wear the shield");
        helper.assertFalse(defender.isUsingItem(), "an axe should disable a shield outside of the window");

        // Flèche renvoyée vers le tireur.
        ItemStack arrowShield = new ItemStack(ModItems.shield(WeaponMaterials.IRON).get());
        Player archerTarget = shieldBearer(helper, arrowShield, position);
        Pig shooter = pigAt(helper, new Vec3(2.5D, 1.0D, 4.5D));
        Arrow arrow = new Arrow(helper.getLevel(), shooter);
        arrow.setPos(position.add(0.0D, 1.0D, 1.0D));
        arrow.setDeltaMovement(0.0D, 0.0D, -2.0D);
        helper.assertTrue(ShieldSystem.tryReflectArrow(arrow, archerTarget), "the arrow should be reflected");
        helper.assertTrue(arrow.getOwner() == archerTarget && arrow.getDeltaMovement().z > 0.0D, "the arrow should fly back toward its shooter");
        PlayerCombatState.of(archerTarget).perfectParryUntil = 0L;
        Arrow late = new Arrow(helper.getLevel(), shooter);
        late.setPos(position.add(0.0D, 1.0D, 1.0D));
        helper.assertFalse(ShieldSystem.tryReflectArrow(late, archerTarget), "no reflection outside of the window");
        helper.succeed();
    }

    /** Joueur factice qui tient un bouclier levé depuis assez longtemps pour qu'il soit efficace. */
    private static Player shieldBearer(GameTestHelper helper, ItemStack shield, Vec3 position) {
        Player player = parrier(helper, shield, position);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        ((LivingEntityAccessor) player).combatreforged$setUseItemRemaining(ParrySystem.USE_DURATION - 6);
        return player;
    }

    private static Enchantment enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return BuiltInRegistries.ENCHANTMENT.getOrThrow(key);
    }

    /** Suggestion de tutoriel : le plus utile que le joueur ne connaît pas encore, parmi ceux des systèmes actifs. */
    @GameTest(template = ARENA)
    public static void tutorialHints(GameTestHelper helper) {
        RulesDefinition rules = WeaponData.rules();
        WeaponDefinition katana = behavior(WeaponType.KATANA);
        helper.assertTrue(TutorialHints.suggestion(katana, rules, id -> false).orElseThrow().equals("two_handed"), "a new katana should suggest two-handed weapons");
        helper.assertTrue(TutorialHints.suggestion(katana, rules, id -> id.equals("two_handed")).orElseThrow().equals("parry"), "then the parry");
        helper.assertTrue(TutorialHints.suggestion(behavior(WeaponType.SAI), rules, id -> false).orElseThrow().equals("dual_wield"), "a sai should suggest dual wielding");
        helper.assertTrue(TutorialHints.suggestion(behavior(WeaponType.RAPIER), rules, id -> id.equals("dual_wield")).orElseThrow().equals("dash"), "a rapier should suggest the dash");
        helper.assertTrue(TutorialHints.suggestion(katana, rules, id -> true).isEmpty(), "nothing to suggest when everything is known");
        helper.assertTrue(TutorialHints.relatedScenes(behavior(WeaponType.SPEAR), rules).equals(List.of("combos", "armor_penetration", "reach")), "spear scenes");
        helper.assertTrue(TutorialScenes.IDS.containsAll(TutorialHints.relatedScenes(katana, rules)), "only existing tutorials");

        CombatCompat.simulateForTests(true);
        try {
            helper.assertFalse(TutorialHints.relatedScenes(katana, WeaponData.rules()).contains("two_handed"), "no suggestion for what Better Combat manages");
        } finally {
            CombatCompat.simulateForTests(false);
        }
        helper.succeed();
    }

    /** Tutoriels : chaque scène est complète et cohérente, ses textes existent, et chaque tutoriel est relié à une option de l'éditeur. */
    @GameTest(template = ARENA)
    public static void tutorialScenesValid(GameTestHelper helper) {
        Language language = Language.getInstance();
        helper.assertTrue(Set.copyOf(TutorialScenes.IDS).size() == TutorialScenes.IDS.size() && TutorialScenes.IDS.size() == 10, "there should be 10 distinct tutorials");
        for (String id : TutorialScenes.IDS) {
            TutorialScene scene = TutorialScenes.create(id);
            helper.assertTrue(language.has(CombatReforged.MODID + ".tutorial." + id + ".title"), id + " has no title");
            helper.assertTrue(TutorialScenes.theme(id) != null, id + " has no theme");
            helper.assertTrue(scene.steps().get(0).start() == 0, id + " should start with a step");
            int previous = -1;
            for (TutorialScene.Step step : scene.steps()) {
                helper.assertTrue(step.start() > previous && step.start() < scene.duration(), id + " has a misplaced step");
                previous = step.start();
                helper.assertTrue(step.caption().getContents() instanceof TranslatableContents text && language.has(text.getKey()), id + " has an untranslated caption");
            }
            for (TutorialScene.Zone zone : scene.zones()) helper.assertTrue(scene.actor(zone.actor()) != null && zone.to() <= scene.duration(), id + " zone");
            for (TutorialScene.Label label : scene.labels()) {
                helper.assertTrue(scene.actor(label.actor()) != null && label.to() <= scene.duration() + 30, id + " label");
                helper.assertTrue(!(label.text().getContents() instanceof TranslatableContents text) || language.has(text.getKey()), id + " has an untranslated label");
            }
            for (TutorialScene.Spark spark : scene.sparks()) helper.assertTrue(scene.actor(spark.actor()) != null && spark.tick() < scene.duration(), id + " spark");
            for (TutorialScene.Actor actor : scene.actors()) {
                for (int tick = 0; tick <= scene.duration(); tick += 7) {
                    TutorialScene.ActorPose pose = scene.pose(actor, tick);
                    helper.assertTrue(Math.abs(pose.x()) <= 4.8D && Math.abs(pose.z()) <= 4.8D, id + ": " + actor.id() + " leaves the platform at tick " + tick);
                }
            }
        }
        for (Map.Entry<String, String> link : TutorialScenes.fieldLinks().entrySet()) {
            helper.assertTrue(TutorialScenes.IDS.contains(link.getValue()), "unknown tutorial for " + link.getKey());
        }
        for (String id : TutorialScenes.IDS) {
            helper.assertTrue(TutorialScenes.fieldLinks().containsValue(id), id + " should be linked to an option");
        }
        helper.assertTrue("dual_wield".equals(TutorialScenes.sceneForField("dual_wield_enabled")) && "dual_wield".equals(TutorialScenes.sceneForField("dual_wielded")), "both dual wield options should open the tutorial");
        helper.assertTrue(TutorialScenes.sceneForField("format_version") == null, "an unrelated option has no tutorial");
        helper.succeed();
    }

    private static Player thrower(GameTestHelper helper, ItemStack stack, GameType mode) {
        Player player = makeMockPlayer(helper, mode);
        player.setPos(helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D)));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    /**
     * Dual wield réservé aux armes qui ont l'option : les deux mains doivent l'avoir, une arme à deux mains ne l'a
     * jamais, et une épée vanilla ne se manie plus par paire.
     */
    @GameTest(template = ARENA)
    public static void dualWieldOption(GameTestHelper helper) {
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = behavior(type);
            boolean expected = definition.dualWielded() && !definition.twoHanded();
            helper.assertTrue(DualWieldSystem.isDualWielding(player(helper, weapon(type), weapon(type))) == expected,
                    type.id() + ": a pair should " + (expected ? "" : "not ") + "dual wield");
        }
        helper.assertTrue(DualWieldSystem.isDualWielding(player(helper, weapon(WeaponType.DAGGER), weapon(WeaponType.RAPIER))),
                "two different dual-wielded weapons should work");
        helper.assertFalse(DualWieldSystem.isDualWielding(player(helper, weapon(WeaponType.DAGGER), weapon(WeaponType.BROADSWORD))),
                "a dagger and a broadsword should not dual wield");
        helper.assertFalse(DualWieldSystem.isDualWielding(player(helper, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_SWORD))),
                "two vanilla swords should not dual wield");
        helper.assertTrue(DualWieldSystem.isMeleeWeapon(new ItemStack(Items.IRON_SWORD)), "a vanilla sword stays a melee weapon");

        // Le saï pare avec un saï dans l'autre main, pas avec une arme qui n'est pas ambidextre.
        ItemStack sai = weapon(WeaponType.SAI);
        Player withBroadsword = player(helper, sai, weapon(WeaponType.BROADSWORD));
        helper.assertFalse(AdoptedItemEvents.onRightClickItem(withBroadsword, InteractionHand.MAIN_HAND, sai, helper.getLevel()),
                "a sai should not parry with a broadsword in the other hand");
        helper.succeed();
    }

    private static void assertDirection(GameTestHelper helper, Vec3 actual, Vec3 expected, String situation) {
        helper.assertTrue(actual.distanceTo(expected) < EPSILON, "dash direction (" + situation + "): " + actual + " instead of " + expected);
    }

    private static void expectParry(GameTestHelper helper, WeaponType type, ItemStack offhand, boolean expected) {
        ItemStack main = weapon(type);
        Player player = player(helper, main, offhand.copy());
        boolean started = AdoptedItemEvents.onRightClickItem(player, InteractionHand.MAIN_HAND, main, helper.getLevel());
        String offhandName = offhand.isEmpty() ? "an empty off hand" : offhand.getItem().toString();
        helper.assertTrue(started == expected && player.isUsingItem() == expected,
                type.id() + " with " + offhandName + ": the parry should " + (expected ? "" : "not ") + "start");
    }

    // --- Corrections avant publication ---

    /** Parade parfaite : un trident ou un shuriken récupérable ne change pas de propriétaire, le shuriken d'un mob si. */
    @GameTest(template = ARENA)
    public static void reflectionKeepsOwnedProjectiles(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        Player defender = shieldBearer(helper, new ItemStack(ModItems.shield(WeaponMaterials.IRON).get()), position);
        Pig shooter = pigAt(helper, new Vec3(2.5D, 1.0D, 4.5D));

        ThrownTrident trident = new ThrownTrident(helper.getLevel(), shooter, new ItemStack(Items.TRIDENT));
        trident.setPos(position.add(0.0D, 1.0D, 1.0D));
        trident.setDeltaMovement(0.0D, 0.0D, -2.0D);
        helper.assertFalse(ShieldSystem.tryReflectArrow(trident, defender), "a trident should not be reflected");
        helper.assertTrue(trident.getOwner() == shooter, "a trident should keep its owner");

        ItemStack stack = new ItemStack(ModItems.shuriken(WeaponMaterials.IRON).get());
        Player thrower = thrower(helper, stack, GameType.SURVIVAL);
        ShurikenEntity recoverable = new ShurikenEntity(helper.getLevel(), thrower, stack, true);
        recoverable.setPos(position.add(0.0D, 1.0D, 1.0D));
        recoverable.setDeltaMovement(0.0D, 0.0D, -2.0D);
        helper.assertFalse(ShieldSystem.tryReflectArrow(recoverable, defender), "a recoverable shuriken should not be reflected");
        helper.assertTrue(recoverable.getOwner() == thrower, "its thrower should still be able to pick it up");

        ShurikenEntity fromMob = new ShurikenEntity(helper.getLevel(), shooter, stack.copyWithCount(1), false);
        fromMob.setPos(position.add(0.0D, 1.0D, 1.0D));
        fromMob.setDeltaMovement(0.0D, 0.0D, -2.0D);
        helper.assertTrue(ShieldSystem.tryReflectArrow(fromMob, defender), "the shuriken of a mob can be reflected");
        helper.succeed();
    }

    /** Un bouclier levé avant de prendre une arme à deux mains est baissé ; une main principale vide ne le fait pas. */
    @GameTest(template = ARENA)
    public static void twoHandedStopsRaisedOffhand(GameTestHelper helper) {
        WeaponType twoHanded = null;
        for (WeaponType type : WeaponType.values()) {
            if (behavior(type).twoHanded()) twoHanded = type;
        }
        helper.assertTrue(twoHanded != null, "no two-handed weapon");
        Player player = player(helper, ItemStack.EMPTY, new ItemStack(Items.SHIELD));
        player.startUsingItem(InteractionHand.OFF_HAND);
        helper.assertTrue(player.isUsingItem(), "the shield should be raised");
        TwoHandedSystem.stopLockedOffhandUse(player);
        helper.assertTrue(player.isUsingItem(), "an empty main hand does not lock the off hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon(twoHanded));
        TwoHandedSystem.stopLockedOffhandUse(player);
        helper.assertFalse(player.isUsingItem(), "a two-handed weapon should lower the shield");
        helper.succeed();
    }

    /** Momentum : la réduction de recharge reste bornée, même à un niveau obtenu par commande. */
    @GameTest(template = ARENA)
    public static void momentumStaysBounded(GameTestHelper helper) {
        helper.assertTrue(Math.abs(ModEnchantments.momentumMultiplier(1) - 0.85D) < EPSILON, "level 1");
        helper.assertTrue(ModEnchantments.momentumMultiplier(50) == 0.25D, "a huge level should be capped");
        helper.succeed();
    }

    // --- Outils ---

    /**
     * Les tags du mod (générés par runData) doivent être chargés. S'ils manquent, le dossier build/resources/main
     * était incomplet au lancement : souvent l'IDE qui recompile en même temps. Relancer suffit.
     */
    private static void assertModTagsLoaded(GameTestHelper helper) {
        helper.assertTrue(weapon(WeaponType.DAGGER).is(ItemTags.SWORDS), "the mod's item tags are not loaded: build/resources/main "
                + "was incomplete when the server started (an IDE build running at the same time?). Run the tests again.");
    }

    private static Player mockPlayerAt(GameTestHelper helper, Vec3 position) {
        Player player = makeMockPlayer(helper, GameType.SURVIVAL);
        player.setPos(position);
        return player;
    }

    private static ItemStack weapon(WeaponType type) {
        return weapon(type, WeaponMaterials.IRON);
    }

    private static ItemStack weapon(WeaponType type, WeaponMaterial material) {
        return new ItemStack(ModItems.get(type, material).get());
    }

    /** Cochon immobile sans armure, 100 PV, à cette position (relative à l'arène, au bloc près). */
    private static Pig pigAt(GameTestHelper helper, Vec3 relative) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, relative);
        pig.setNoGravity(true);
        setBase(pig.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        pig.setHealth(TEST_HEALTH);
        return pig;
    }

    private static boolean isHurt(Pig pig) {
        return pig.getHealth() < TEST_HEALTH;
    }

    /** Le prochain coup du joueur sera chargé à fond. */
    private static void fullyCharge(Player player) {
        ((LivingEntityAccessor) player).combatreforged$setAttackStrengthTicker(1000);
    }

    private static int firstStep(ComboDefinition combo, java.util.function.Predicate<ComboStep> condition) {
        for (int i = 0; i < combo.steps().size(); i++) {
            if (condition.test(combo.steps().get(i))) return i;
        }
        return -1;
    }

    /** Cochon immobile, 100 PV, armure de diamant complète (20 points, 8 de solidité). */
    private static Pig armoredPig(GameTestHelper helper, BlockPos position) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, position);
        pig.setNoGravity(true);
        setBase(pig.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        setBase(pig.getAttribute(Attributes.ARMOR), 20.0D);
        setBase(pig.getAttribute(Attributes.ARMOR_TOUGHNESS), 8.0D);
        pig.setHealth(TEST_HEALTH);
        return pig;
    }

    /** Joueur factice tenant ces objets, avec leurs bonus d'attributs appliqués (comme quand on les équipe). */
    private static Player player(GameTestHelper helper, ItemStack mainHand, ItemStack offHand) {
        Player player = holding(helper, mainHand, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, offHand);
        return player;
    }

    private static Player holding(GameTestHelper helper, ItemStack mainHand, GameType mode) {
        Player player = makeMockPlayer(helper, mode);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        mainHand.getAttributeModifiers(EquipmentSlot.MAINHAND).forEach((attribute, modifier) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null && instance.getModifier(modifier.getId()) == null) instance.addTransientModifier(modifier);
        });
        return player;
    }

    private static Player makeMockPlayer(GameTestHelper helper, GameType mode) {
        return new MockPlayer(helper, mode);
    }

    private static void setBase(AttributeInstance attribute, double value) {
        if (attribute != null) attribute.setBaseValue(value);
    }

    private static final class MockPlayer extends Player {
        private final GameType mode;

        private MockPlayer(GameTestHelper helper, GameType mode) {
            super(helper.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player"));
            this.mode = mode;
        }

        @Override
        public boolean isSpectator() {
            return mode == GameType.SPECTATOR;
        }

        @Override
        public boolean isCreative() {
            return mode != null && mode.isCreative();
        }

        @Override
        public boolean isLocalPlayer() {
            return true;
        }
    }

    private static void heal(Pig pig) {
        pig.setHealth(TEST_HEALTH);
        pig.invulnerableTime = 0;
    }
}
