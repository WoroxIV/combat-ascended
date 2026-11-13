package com.combatascended.compat;

import com.combatascended.platform.Platform;
import net.minecraft.world.entity.Entity;

/**
 * Better Combat remplace notre portée, notre balayage, nos deux mains et notre dual wield : quand il est là, ces
 * systèmes se coupent et tout le reste (combos, parades, effets, enchantements, pénétration d'armure) continue.
 * Epic Fight, lui, ne peut pas être chargé en même temps : le jeu refuse de démarrer.
 */
public final class CombatCompat {
    public static final String BETTER_COMBAT = "bettercombat";

    // Calculé une seule fois, au premier appel (la liste des mods ne change pas en cours de partie).
    private static final boolean BETTER_COMBAT_LOADED = Platform.get().isModLoaded(BETTER_COMBAT);

    /** Tests automatiques uniquement : fait comme si Better Combat était installé. */
    private static volatile boolean simulated;

    private CombatCompat() {
    }

    /** Vrai si Better Combat gère les systèmes que nous avons en commun. */
    public static boolean betterCombat() {
        return simulated || BETTER_COMBAT_LOADED;
    }

    /**
     * Joueur simulé par un mod (déployeur de Create...) : il tient une arme simple, qui applique ses dégâts et rien d'autre.
     * Dual wield, combos, parade, dash et effets des armes ne servent qu'aux vrais joueurs et aux mobs.
     */
    public static boolean isFakePlayer(Entity entity) {
        return Platform.get().isFakePlayer(entity);
    }

    public static void simulateForTests(boolean value) {
        simulated = value;
    }
}
