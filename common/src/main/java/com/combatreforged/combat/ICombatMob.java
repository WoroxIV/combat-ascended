package com.combatreforged.combat;

/** État de combat ajouté à chaque mob par MobMixin. Rien n'est sauvegardé : ces valeurs ne durent que quelques secondes. */
public interface ICombatMob {

    boolean combatreforged$offhandTurn();

    void combatreforged$setOffhandTurn(boolean offhand);

    long combatreforged$guardBrokenUntil();

    void combatreforged$setGuardBrokenUntil(long gameTime);
}
