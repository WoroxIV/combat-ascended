package com.combatreforged.network;

import com.combatreforged.CombatReforged;
import com.combatreforged.combat.ComboSystem;
import com.combatreforged.combat.PlayerCombatState;
import com.combatreforged.compat.CombatCompat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Serveur → joueur, après chacun de ses coups : où en est son combo. Le client suit déjà son combo tout seul
 * (pour l'animation et l'indicateur), ce paquet corrige seulement un éventuel désaccord (coup chargé à 89 %
 * côté client et 90 % côté serveur, par exemple).
 *
 * @param step      coup du combo qui va être porté
 * @param ticksLeft temps avant que le combo reparte du début
 * @param profile   profil de l'arme du combo (vide = aucun combo)
 * @param handTurn  main qui frappe ensuite : 0 = principale, 1 = secondaire, -1 = ne pas y toucher (après un coup,
 *                  le client a pu frapper de nouveau entre-temps et a déjà la bonne main)
 */
public record ComboSyncPayload(int step, int ticksLeft, String profile, int handTurn) implements CustomPacketPayload {
    public static final Type<ComboSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CombatReforged.MODID, "combo_sync"));
    private static final int KEEP_HAND = -1;

    public static final StreamCodec<ByteBuf, ComboSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ComboSyncPayload::step,
            ByteBufCodecs.VAR_INT, ComboSyncPayload::ticksLeft,
            ByteBufCodecs.STRING_UTF8, ComboSyncPayload::profile,
            ByteBufCodecs.VAR_INT, ComboSyncPayload::handTurn,
            ComboSyncPayload::new);

    public static ComboSyncPayload of(Player player, PlayerCombatState state) {
        return create(player, state, KEEP_HAND);
    }

    /** Changement de dimension : le joueur côté client est recréé et repart de zéro, pas celui du serveur. */
    public static ComboSyncPayload withHand(Player player, PlayerCombatState state) {
        return create(player, state, state.offhandTurn ? 1 : 0);
    }

    private static ComboSyncPayload create(Player player, PlayerCombatState state, int handTurn) {
        long ticksLeft = Math.max(0L, state.comboExpiresAt - player.level().getGameTime());
        String profile = state.comboType == null ? "" : state.comboType.toString();
        return new ComboSyncPayload(state.comboStep, (int) Math.min(Integer.MAX_VALUE, ticksLeft), profile, handTurn);
    }

    @Override
    public Type<ComboSyncPayload> type() {
        return TYPE;
    }

    /** Côté client : remplace l'état du combo du joueur par celui du serveur. */
    public static void handle(ComboSyncPayload payload, Player player) {
        PlayerCombatState state = PlayerCombatState.of(player);
        int previousStep = state.comboStep;
        state.comboType = payload.profile().isEmpty() ? null : ResourceLocation.parse(payload.profile());
        state.comboStep = payload.step();
        state.comboExpiresAt = player.level().getGameTime() + payload.ticksLeft();
        if (payload.handTurn() != KEEP_HAND) state.offhandTurn = payload.handTurn() == 1;
        // Avec Better Combat, le client ne prédit rien : le signal "prochain coup spécial" vient d'ici.
        if (CombatCompat.betterCombat() && payload.step() != previousStep) ComboSystem.onServerAdvance(player, state);
    }
}
