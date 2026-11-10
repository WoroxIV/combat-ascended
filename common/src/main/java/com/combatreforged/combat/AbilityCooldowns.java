package com.combatreforged.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Recharge d'une capacité (dash, garde brisée), affichée par l'icône vanilla dans la barre d'objets. */
public final class AbilityCooldowns {
    private static final double TICKS_PER_SECOND = 20.0D;

    private AbilityCooldowns() {
    }

    /** Durée en ticks (au moins 1). */
    public static int ticks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * TICKS_PER_SECOND));
    }

    /** La recharge touche tous les objets du même profil : changer de matériau ne permet pas d'y échapper. */
    public static void start(Player player, ItemStack stack, double seconds) {
        int ticks = ticks(seconds);
        List<Item> shared = WeaponProfiles.id(stack).map(WeaponProfiles::itemsOf).orElse(List.of());
        if (shared.isEmpty()) {
            player.getCooldowns().addCooldown(stack.getItem(), ticks);
        } else {
            shared.forEach(item -> player.getCooldowns().addCooldown(item, ticks));
        }
    }
}
