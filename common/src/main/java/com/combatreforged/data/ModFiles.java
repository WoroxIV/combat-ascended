package com.combatreforged.data;

import com.combatreforged.platform.Platform;

import java.util.Optional;

/**
 * Lit des fichiers directement dans le jar du mod, sans passer par les datapacks ni les packs de ressources.
 * Sert aux valeurs par défaut (avant le chargement d'un monde) et aux traductions du README et du schéma.
 */
public final class ModFiles {

    private ModFiles() {
    }

    /** Contenu texte d'un fichier du jar, ex : {@code read("data", "combat_reforged", "...", "katana.json")}. */
    public static Optional<String> read(String... path) {
        return Platform.get().readModFile(path);
    }
}
