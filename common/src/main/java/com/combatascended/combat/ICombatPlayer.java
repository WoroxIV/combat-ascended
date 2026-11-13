package com.combatascended.combat;

/**
 * Ajouté à chaque joueur par {@link com.combatascended.mixin.PlayerCombatMixin} pour lui donner un
 * {@link PlayerCombatState}. Le préfixe "combatascended$" évite les collisions avec les méthodes d'autres mods.
 * Utiliser plutôt {@link PlayerCombatState#of}.
 */
public interface ICombatPlayer {
    PlayerCombatState combatascended$combat();
}
