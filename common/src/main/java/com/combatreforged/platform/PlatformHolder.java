package com.combatreforged.platform;

/** Garde la version du chargeur en cours (les champs d'une interface ne peuvent pas changer de valeur). */
final class PlatformHolder {
    private static volatile Platform instance;

    private PlatformHolder() {
    }

    static Platform instance() {
        Platform platform = instance;
        if (platform == null) throw new IllegalStateException("Combat Reforged: the loader entry point has not run yet");
        return platform;
    }

    static void install(Platform platform) {
        instance = platform;
    }
}
