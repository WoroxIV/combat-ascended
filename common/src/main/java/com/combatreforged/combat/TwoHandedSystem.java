package com.combatreforged.combat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * Règles des armes à deux mains : tant que le joueur en tient une en main principale,
 * la main secondaire est inutilisable et cachée (événements + mixins de rendu).
 */
public final class TwoHandedSystem {

    private TwoHandedSystem() {
    }

    /** Toujours faux si les armes à deux mains sont coupées (rules.json, Better Combat). */
    public static boolean isOffhandLocked(Player player) {
        return WeaponActions.isTwoHanded(player.getMainHandItem());
    }

    /** Cette main ne peut rien faire : c'est la main secondaire, et le joueur tient une arme à deux mains. */
    public static boolean isLockedHand(Player player, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND && isOffhandLocked(player);
    }

    /** Un bouclier levé avant de passer à une arme à deux mains ne doit pas continuer à bloquer, caché. */
    public static void stopLockedOffhandUse(Player player) {
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND && isOffhandLocked(player)) {
            player.stopUsingItem();
        }
    }
}
