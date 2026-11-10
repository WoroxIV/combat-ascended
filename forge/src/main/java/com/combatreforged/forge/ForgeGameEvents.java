package com.combatreforged.forge;

import com.combatreforged.combat.AdoptedItemEvents;
import com.combatreforged.combat.CombatEvents;
import com.combatreforged.combat.MobCombat;
import com.combatreforged.data.WeaponData;
import com.combatreforged.combat.ShieldSystem;
import com.combatreforged.combat.TwoHandedSystem;
import com.combatreforged.command.CombatReforgedCommand;
import com.combatreforged.data.WeaponDataEvents;
import com.combatreforged.data.WeaponDataLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.stream.Stream;

/** Relie les événements du jeu de Forge au code commun. Les dégâts sont dans LivingEntityDamageMixin : Forge n'a pas d'événement équivalent. */
public final class ForgeGameEvents {
    private ForgeGameEvents() {
    }

    // --- Combat ---

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CombatEvents.onChangedDimension(player);
    }

    @SubscribeEvent
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        ShieldSystem.onStartUsingItem(event.getEntity(), event.getItem());
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (ShieldSystem.onProjectileImpact(event.getProjectile(), event.getRayTraceResult())) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        }
    }

    // --- Interactions ---

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (cancelIfLocked(event)) return;
        if (AdoptedItemEvents.onRightClickItem(event.getEntity(), event.getHand(), event.getItemStack(), event.getLevel())) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelIfLocked(event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfLocked(event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelIfLocked(event);
    }

    private static boolean cancelIfLocked(PlayerInteractEvent event) {
        if (!TwoHandedSystem.isLockedHand(event.getEntity(), event.getHand())) return false;
        event.setCanceled(true);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) TwoHandedSystem.stopLockedOffhandUse(event.player);
    }

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() == EquipmentSlot.MAINHAND) AdoptedItemEvents.collectModifiers(event.getItemStack(), event::addModifier);
    }

    // --- Monde ---

    /** Les mobs reçoivent leurs comportements de garde à chaque arrivée dans le monde. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && WeaponData.rules().mobBehaviorsActive()) MobCombat.addGoals(mob);
    }

    // --- Données et commandes ---

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new WeaponDataLoader());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        WeaponDataEvents.onDatapackSync(player == null, player != null ? Stream.of(player) : event.getPlayerList().getPlayers().stream());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CombatReforgedCommand.register(event.getDispatcher());
    }
}
