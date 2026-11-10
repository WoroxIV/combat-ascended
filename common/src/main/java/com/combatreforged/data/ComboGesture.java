package com.combatreforged.data;

import net.minecraft.util.StringRepresentable;

/** Mouvement du bras pendant une étape de combo, joué sur les deux vues. Better Combat garde ses propres animations. */
public enum ComboGesture implements StringRepresentable {
    /** Coup de bras vanilla. */
    DEFAULT("default"),
    /** Taille horizontale, de côté. */
    SLASH("slash"),
    /** Estoc, droit devant. */
    THRUST("thrust"),
    /** Coup vertical, de haut en bas. */
    OVERHEAD("overhead");

    private final String id;

    ComboGesture(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
