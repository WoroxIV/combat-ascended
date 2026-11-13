package com.combatascended.platform;

import com.combatascended.network.Payload;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Ce qui diffère d'un chargeur de mods à l'autre (NeoForge, Fabric...). Le code commun ne connaît que cette interface ;
 * chaque chargeur en fournit une version et l'installe au tout début de son point d'entrée.
 */
public interface Platform {

    /** La version du chargeur en cours d'utilisation. */
    static Platform get() {
        return PlatformHolder.instance();
    }

    /** À appeler en premier dans le point d'entrée du chargeur. */
    static void set(Platform platform) {
        PlatformHolder.install(platform);
    }

    boolean isModLoaded(String modId);

    Path configDir();

    Path gameDir();

    /** Vrai pendant la génération des fichiers de données. */
    boolean isDataGen();

    /** Joueur simulé par un mod (déployeur de Create...). */
    boolean isFakePlayer(Entity entity);

    /** Cet objet bloque comme un bouclier (celui du jeu, du mod, ou d'un autre mod). */
    boolean isShield(ItemStack stack);

    /** L'attribut de portée d'attaque du chargeur (la 1.20.1 n'en a pas dans le jeu de base). */
    Attribute entityReachAttribute();

    /** Portée d'attaque actuelle de ce joueur. */
    double entityReach(Player player);

    /** Nom d'un tag commun, écrit "c:ingots/steel" dans le code : chaque chargeur a sa propre convention. */
    String commonTag(String id);

    /** Partie d'une créature à plusieurs corps (dragon, boss d'un mod). */
    boolean isBossPart(Entity entity);

    /** Contenu texte d'un fichier du jar du mod, ex : {@code readModFile("data", "combat_ascended", "x.json")}. */
    Optional<String> readModFile(String... path);

    /** Enregistre un objet du jeu (item, son, effet...). Il n'est utilisable qu'une fois les registres remplis. */
    <R, T extends R> Registered<R, T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory);

    /** Début de la construction d'un onglet créatif, placé après l'onglet Combat. */
    CreativeModeTab.Builder creativeTabBuilder();

    void sendToPlayer(ServerPlayer player, Payload payload);

    /** Envoie aux joueurs qui voient cette entité, sans elle-même. */
    void sendToPlayersTracking(Entity entity, Payload payload);

    /** Côté client uniquement. */
    void sendToServer(Payload payload);

    /** Cet objet accepte cet enchantement sur une table ou une enclume. */
    boolean supportsEnchantment(ItemStack stack, Enchantment enchantment);



    /** Joueur simulé, pour les tests automatiques. */
    ServerPlayer createFakePlayer(ServerLevel level);

    /** Reprend les anciennes configs du mod, s'il y en a pour ce chargeur. */
    default void migrateLegacyConfig() {
    }
}
