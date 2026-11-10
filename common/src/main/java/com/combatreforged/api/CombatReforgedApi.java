package com.combatreforged.api;

import com.combatreforged.combat.WeaponProfiles;
import com.combatreforged.data.ComboStep;
import com.combatreforged.data.WeaponDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * API publique de Combat Reforged : la seule partie du code garantie stable d'une version à l'autre.
 * Pour ajouter des armes, un fichier JSON suffit (voir le README) : cette classe sert aux mods qui veulent réagir au combat.
 */
public final class CombatReforgedApi {
    private static final List<BiConsumer<Player, ItemStack>> PARRY_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<ComboListener> COMBO_LISTENERS = new CopyOnWriteArrayList<>();

    private CombatReforgedApi() {
    }

    /** Appelé après chaque coup de combo qui a touché. */
    @FunctionalInterface
    public interface ComboListener {
        void onComboHit(Player player, ItemStack weapon, ComboStep step);
    }

    /** Le comportement de cet objet, s'il en a un. */
    public static Optional<WeaponDefinition> behaviorOf(ItemStack stack) {
        return WeaponProfiles.definition(stack);
    }

    /** L'identifiant du fichier d'arme ou de l'archétype de cet objet, s'il en a un. */
    public static Optional<ResourceLocation> profileOf(ItemStack stack) {
        return WeaponProfiles.id(stack);
    }

    /** Appelé après chaque parade parfaite. */
    public static void onPerfectParry(BiConsumer<Player, ItemStack> listener) {
        PARRY_LISTENERS.add(listener);
    }

    public static void onComboHit(ComboListener listener) {
        COMBO_LISTENERS.add(listener);
    }

    /** Interne : prévient les mods à l'écoute. */
    public static void firePerfectParry(Player player, ItemStack guard) {
        PARRY_LISTENERS.forEach(listener -> listener.accept(player, guard));
    }

    /** Interne : prévient les mods à l'écoute. */
    public static void fireComboHit(Player player, ItemStack weapon, ComboStep step) {
        COMBO_LISTENERS.forEach(listener -> listener.onComboHit(player, weapon, step));
    }
}
