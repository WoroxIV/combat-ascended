package com.combatascended.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Données de combat actuellement en vigueur (archétypes, fichiers d'armes, règles).
 * <p>
 * Côté serveur, elles viennent du chargement des fichiers ({@link WeaponDataLoader}) ; côté client, de la
 * synchronisation envoyée par le serveur. En solo, client et serveur partagent la même instance, ce qui est
 * sans danger : les deux reçoivent les mêmes valeurs. Avant tout chargement (menu principal), ce sont les
 * valeurs par défaut du jar.
 */
public final class WeaponData {
    private static final AtomicInteger GENERATION = new AtomicInteger();

    /** Données en vigueur, null tant qu'on n'a pas eu besoin des valeurs par défaut. */
    private static volatile Versioned current;
    private static volatile Snapshot builtin;

    private WeaponData() {
    }

    /** D'où vient le profil d'un objet, du plus explicite au plus deviné. */
    public enum Source implements net.minecraft.util.StringRepresentable {
        FILE("file"), TAG("tag"), BETTER_COMBAT("better_combat"), EPIC_FIGHT("epic_fight"), NAME("name"), FALLBACK("fallback");

        public static final Codec<Source> CODEC = net.minecraft.util.StringRepresentable.fromEnum(Source::values);

        private final String id;

        Source(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        /** Vrai si le profil a été deviné : aucun fichier ne l'a demandé pour cet objet. */
        public boolean detected() {
            return this == BETTER_COMBAT || this == EPIC_FIGHT || this == NAME || this == FALLBACK;
        }
    }

    /**
     * Le profil attribué à un objet, calculé par le serveur.
     *
     * @param keepsOwnRightClick l'objet a son propre clic droit : il garde le sien
     */
    public record ItemProfile(ResourceLocation profile, Source source, boolean keepsOwnRightClick) {
        public static final Codec<ItemProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("profile").forGetter(ItemProfile::profile),
                Source.CODEC.fieldOf("source").forGetter(ItemProfile::source),
                Codec.BOOL.optionalFieldOf("keeps_own_right_click", false).forGetter(ItemProfile::keepsOwnRightClick)
        ).apply(instance, ItemProfile::new));
    }

    /**
     * Un jeu de données complet, immuable : on le remplace en bloc, jamais champ par champ.
     *
     * @param archetypes comportements de référence, par identifiant
     * @param weapons    fichiers d'armes, par identifiant
     * @param items      profil de chaque objet : vide côté serveur jusqu'au premier calcul, reçu tout prêt par le client
     */
    public record Snapshot(Map<ResourceLocation, WeaponDefinition> archetypes, Map<ResourceLocation, WeaponDefinition> weapons,
                           DetectionRules detection, RulesDefinition rules, GearDefinition gear, Map<ResourceLocation, ItemProfile> items) {
        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(ResourceLocation.CODEC, WeaponDefinition.CODEC).fieldOf("archetypes").forGetter(Snapshot::archetypes),
                Codec.unboundedMap(ResourceLocation.CODEC, WeaponDefinition.CODEC).fieldOf("weapons").forGetter(Snapshot::weapons),
                DetectionRules.CODEC.fieldOf("detection").forGetter(Snapshot::detection),
                RulesDefinition.CODEC.fieldOf("rules").forGetter(Snapshot::rules),
                GearDefinition.CODEC.fieldOf("gear").forGetter(Snapshot::gear),
                Codec.unboundedMap(ResourceLocation.CODEC, ItemProfile.CODEC).fieldOf("items").forGetter(Snapshot::items)
        ).apply(instance, Snapshot::new));

        public Snapshot {
            archetypes = Map.copyOf(archetypes);
            weapons = Map.copyOf(weapons);
            items = Map.copyOf(items);
        }

        /** Le comportement de cet identifiant : un fichier d'arme, sinon un archétype. */
        public WeaponDefinition definition(ResourceLocation id) {
            WeaponDefinition weapon = weapons.get(id);
            return weapon != null ? weapon : archetypes.getOrDefault(id, WeaponDefinition.FALLBACK);
        }

        @Nullable
        public WeaponDefinition definitionOrNull(ResourceLocation id) {
            WeaponDefinition weapon = weapons.get(id);
            return weapon != null ? weapon : archetypes.get(id);
        }

        /** Mêmes données, avec le profil de chaque objet (avant de les envoyer aux clients). */
        public Snapshot withItems(Map<ResourceLocation, ItemProfile> resolved) {
            return new Snapshot(archetypes, weapons, detection, rules, gear, resolved);
        }
    }

    /**
     * Données + numéro de version, qui augmente à chaque remplacement.
     * Les caches comparent ce numéro pour savoir s'ils sont à jour.
     */
    public record Versioned(Snapshot snapshot, int generation) {
    }

    public static Snapshot current() {
        return versioned().snapshot();
    }

    public static Versioned versioned() {
        Versioned value = current;
        if (value == null) {
            synchronized (WeaponData.class) {
                if (current == null) current = new Versioned(builtin(), GENERATION.incrementAndGet());
                value = current;
            }
        }
        return value;
    }

    public static WeaponDefinition definition(ResourceLocation id) {
        return current().definition(id);
    }

    public static RulesDefinition rules() {
        return current().rules();
    }

    public static GearDefinition gear() {
        return current().gear();
    }

    /** Remplace les données en vigueur (chargement serveur ou synchronisation reçue). */
    public static void install(Snapshot snapshot) {
        current = new Versioned(snapshot, GENERATION.incrementAndGet());
    }

    /** Revient aux valeurs par défaut du jar (ex : le client quitte un serveur). */
    public static void resetToBuiltin() {
        install(builtin());
    }

    /** Valeurs par défaut du jar, lues une seule fois. */
    public static Snapshot builtin() {
        Snapshot value = builtin;
        if (value == null) {
            synchronized (WeaponData.class) {
                if (builtin == null) builtin = WeaponDataLoader.loadBuiltin();
                value = builtin;
            }
        }
        return value;
    }
}
