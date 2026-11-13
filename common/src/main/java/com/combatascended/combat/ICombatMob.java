package com.combatascended.combat;

/** État de combat ajouté à chaque mob par MobMixin. Rien n'est sauvegardé : ces valeurs ne durent que quelques secondes. */
public interface ICombatMob {

    boolean combatascended$offhandTurn();

    void combatascended$setOffhandTurn(boolean offhand);

    long combatascended$guardBrokenUntil();

    void combatascended$setGuardBrokenUntil(long gameTime);
}
