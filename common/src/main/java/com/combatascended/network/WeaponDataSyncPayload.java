package com.combatascended.network;

import com.combatascended.data.DataResults;
import com.combatascended.combat.WeaponProfiles;
import com.combatascended.data.WeaponData;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Envoie au client les données des armes du serveur (stats et règles), pour que les tooltips,
 * la recharge affichée et la prédiction des coups correspondent au serveur.
 * Envoyé à la connexion puis après chaque {@code /reload}.
 * <p>
 * Les données voyagent en JSON : le même format que les fichiers, simple à déboguer.
 */
public record WeaponDataSyncPayload(WeaponData.Snapshot snapshot) implements Payload {
    /** Taille maximale du JSON (1 million de caractères) : très au-dessus des besoins réels. */
    private static final int MAX_LENGTH = 1 << 20;

    /** Les données en vigueur, avec le profil de chaque objet : le client n'a plus rien à calculer. */
    public static WeaponDataSyncPayload current() {
        return new WeaponDataSyncPayload(WeaponData.current().withItems(WeaponProfiles.table()));
    }

    public static WeaponDataSyncPayload read(FriendlyByteBuf buffer) {
        return fromJson(buffer.readUtf(MAX_LENGTH));
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(toJson(), MAX_LENGTH);
    }

    /** Côté client : remplace les données en vigueur par celles du serveur. */
    public static void handle(WeaponDataSyncPayload payload) {
        WeaponData.install(payload.snapshot());
    }

    private static WeaponDataSyncPayload fromJson(String json) {
        return new WeaponDataSyncPayload(DataResults.orThrow(WeaponData.Snapshot.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))));
    }

    private String toJson() {
        return DataResults.orThrow(WeaponData.Snapshot.CODEC.encodeStart(JsonOps.INSTANCE, snapshot)).toString();
    }
}
