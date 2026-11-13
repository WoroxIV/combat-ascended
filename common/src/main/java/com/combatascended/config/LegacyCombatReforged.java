package com.combatascended.config;

import com.combatascended.CombatAscended;
import com.combatascended.platform.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reprise des réglages de Combat Reforged, l'ancien nom de ce mod.
 * Le dossier {@code config/combat_reforged/} est copié une fois, au premier démarrage sous le nouveau nom ; l'original n'est pas modifié.
 */
final class LegacyCombatReforged {
    private static final String OLD_NAME = "combat_reforged";

    private LegacyCombatReforged() {
    }

    /** À appeler avant la création du nouveau dossier : la copie n'a lieu que s'il n'existe pas encore. */
    static void importConfig() throws IOException {
        Path configDir = Platform.get().configDir();
        Path oldClient = configDir.resolve(OLD_NAME + "-client.json");
        Path newClient = configDir.resolve(ClientConfig.FILE_NAME);
        if (Files.isRegularFile(oldClient) && !Files.exists(newClient)) Files.copy(oldClient, newClient);

        Path oldRoot = configDir.resolve(OLD_NAME);
        Path newRoot = ConfigFolder.root();
        if (!Files.isDirectory(oldRoot) || Files.exists(newRoot)) return;
        List<Path> files;
        try (Stream<Path> walk = Files.walk(oldRoot)) {
            files = walk.toList();
        }
        for (Path file : files) {
            Path target = newRoot.resolve(oldRoot.relativize(file).toString());
            if (Files.isDirectory(file)) Files.createDirectories(target);
            else if (!Files.exists(target)) Files.copy(file, target);
        }
        CombatAscended.LOGGER.info("[Combat Ascended] Copied the settings of Combat Reforged from {}.", ConfigFolder.displayPath(oldRoot));
    }
}
