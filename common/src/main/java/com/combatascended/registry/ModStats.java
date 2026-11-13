package com.combatascended.registry;

import com.combatascended.platform.Platform;
import com.combatascended.CombatAscended;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

import java.util.function.Supplier;

/** Statistiques du joueur, affichées dans l'écran vanilla (onglet Général). */
public final class ModStats {
    public static final Supplier<ResourceLocation> PERFECT_PARRIES = register("perfect_parries");

    private ModStats() {
    }

    /** Force la déclaration des statistiques. */
    public static void init() {
    }

    private static Supplier<ResourceLocation> register(String name) {
        ResourceLocation id = new ResourceLocation(CombatAscended.MODID, name);
        return Platform.get().register(Registries.CUSTOM_STAT, name, () -> id);
    }

    /** À appeler une fois les registres figés : donne à chaque statistique sa façon de s'afficher. */
    public static void registerFormats() {
        Stats.CUSTOM.get(PERFECT_PARRIES.get(), StatFormatter.DEFAULT);
    }
}
