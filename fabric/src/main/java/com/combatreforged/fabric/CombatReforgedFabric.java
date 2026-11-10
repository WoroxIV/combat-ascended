package com.combatreforged.fabric;

import com.combatreforged.CombatReforged;
import com.combatreforged.combat.AdoptedItemEvents;
import com.combatreforged.combat.CombatEvents;
import com.combatreforged.combat.MobCombat;
import com.combatreforged.data.WeaponData;
import com.combatreforged.combat.TwoHandedSystem;
import com.combatreforged.command.CombatReforgedCommand;
import com.combatreforged.data.WeaponDataEvents;
import com.combatreforged.data.WeaponDataLoader;
import com.combatreforged.platform.Platform;
import com.combatreforged.registry.ModStats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Mob;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/** Point d'entrée Fabric (chargé côté client et serveur). */
public final class CombatReforgedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Platform.set(new FabricPlatform());
        CombatReforged.init();
        ModStats.registerFormats();
        // Côté client, on attend que le jeu soit prêt pour connaître sa langue (CombatReforgedFabricClient).
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) CombatReforged.prepareConfigFolder();

        FabricNetwork.register();
        registerCombat();
        registerData();
    }

    private static void registerCombat() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> CombatEvents.onChangedDimension(player));

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Mob mob && WeaponData.rules().mobBehaviorsActive()) MobCombat.addGoals(mob);
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (TwoHandedSystem.isLockedHand(player, hand)) return InteractionResultHolder.fail(player.getItemInHand(hand));
            if (AdoptedItemEvents.onRightClickItem(player, hand, player.getItemInHand(hand), level)) return InteractionResultHolder.consume(player.getItemInHand(hand));
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> TwoHandedSystem.isLockedHand(player, hand) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> TwoHandedSystem.isLockedHand(player, hand) ? InteractionResult.FAIL : InteractionResult.PASS);

    }

    private static void registerData() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new WeaponDataReloadListener());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> WeaponDataEvents.onDatapackSync(!joined, Stream.of(player)));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> CombatReforgedCommand.register(dispatcher));
    }

    /** Le chargeur de données, avec le nom que Fabric demande. */
    private static final class WeaponDataReloadListener implements IdentifiableResourceReloadListener {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CombatReforged.MODID, "weapon_data");
        private final WeaponDataLoader loader = new WeaponDataLoader();

        @Override
        public ResourceLocation getFabricId() {
            return ID;
        }

        @Override
        public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager, ProfilerFiller preparationProfiler,
                                              ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
            return loader.reload(barrier, manager, preparationProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
        }
    }
}
