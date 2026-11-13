package com.combatascended;

import com.combatascended.combat.ModEnchantments;
import com.combatascended.config.ConfigFolder;
import com.combatascended.platform.Platform;
import com.combatascended.registry.ModAttributes;
import com.combatascended.registry.ModSounds;
import com.combatascended.registry.ModStats;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Le cœur du mod, commun à tous les chargeurs. Chaque chargeur a son propre point d'entrée, qui installe sa
 * {@link Platform} puis appelle {@link #init()}.
 * <p>
 * Les stats des armes et les règles sont des fichiers JSON (voir {@link ConfigFolder} et
 * {@link com.combatascended.data.WeaponDataLoader}).
 */
public final class CombatAscended {
    public static final String MODID = "combat_ascended";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Langue du README et des schémas du dossier de config : celle du jeu côté client, l'anglais sur un serveur dédié. */
    private static volatile Supplier<String> languageSupplier = () -> "en_us";

    private CombatAscended() {
    }

    /** Déclare tout le contenu du mod (objets, sons, effets...). */
    public static void init() {
        ModStats.init();
        ModAttributes.init();
        ModSounds.init();
        ModEnchantments.init();
    }

    /** Appelé par le point d'entrée client pour écrire la documentation dans la langue du joueur. */
    public static void setLanguageSupplier(Supplier<String> supplier) {
        languageSupplier = supplier;
    }

    /** Prépare config/combat_ascended/ (et convertit les anciennes configs) avant le chargement de tout monde. */
    public static void prepareConfigFolder() {
        if (Platform.get().isDataGen()) return;
        ConfigFolder.prepare(languageSupplier.get());
    }
}
