package com.combatreforged.combat;

/**
 * Ajouté à chaque joueur par {@link com.combatreforged.mixin.PlayerCombatMixin} pour lui donner un
 * {@link PlayerCombatState}. Le préfixe "combatreforged$" évite les collisions avec les méthodes d'autres mods.
 * Utiliser plutôt {@link PlayerCombatState#of}.
 */
public interface ICombatPlayer {
    PlayerCombatState combatreforged$combat();
}
