package com.combatascended.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Comment reconnaître une arme à son nom. Fichiers {@code data/<mod>/combat_ascended/detection/*.json}, tous fusionnés,
 * plus {@code config/combat_ascended/detection.json} : un mod ou un modpack ajoute ses mots sans toucher au reste.
 *
 * @param keywords           mots du nom et archétype correspondant ; le mot le plus long l'emporte
 * @param excludedWords      un objet dont le nom contient l'un de ces mots n'est jamais détecté (outils...)
 * @param excludedNamespaces mods dont les objets ne sont jamais détectés
 * @param excludedItems      objets jamais détectés
 */
public record DetectionRules(List<Keyword> keywords, List<String> excludedWords, List<String> excludedNamespaces, List<ResourceLocation> excludedItems) {
    public static final DetectionRules EMPTY = new DetectionRules(List.of(), List.of(), List.of(), List.of());

    /**
     * @param words     mots reconnus ; un mot avec "_" doit apparaître tel quel, ex : "long_sword"
     * @param archetype archétype donné à l'objet
     * @param suffix    le mot peut aussi terminer un mot plus long, ex : "blade" dans "flameblade"
     */
    public record Keyword(List<String> words, ResourceLocation archetype, boolean suffix) {
        public static final Codec<Keyword> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("words").forGetter(Keyword::words),
                ResourceLocation.CODEC.fieldOf("archetype").forGetter(Keyword::archetype),
                Codec.BOOL.optionalFieldOf("suffix", false).forGetter(Keyword::suffix)
        ).apply(instance, Keyword::new));
    }

    public static final Codec<DetectionRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Keyword.CODEC.listOf().optionalFieldOf("keywords", List.of()).forGetter(DetectionRules::keywords),
            Codec.STRING.listOf().optionalFieldOf("excluded_words", List.of()).forGetter(DetectionRules::excludedWords),
            Codec.STRING.listOf().optionalFieldOf("excluded_namespaces", List.of()).forGetter(DetectionRules::excludedNamespaces),
            ResourceLocation.CODEC.listOf().optionalFieldOf("excluded_items", List.of()).forGetter(DetectionRules::excludedItems)
    ).apply(instance, DetectionRules::new));

    public DetectionRules plus(DetectionRules other) {
        List<Keyword> allKeywords = new ArrayList<>(keywords);
        allKeywords.addAll(other.keywords);
        List<String> words = new ArrayList<>(excludedWords);
        words.addAll(other.excludedWords);
        List<String> namespaces = new ArrayList<>(excludedNamespaces);
        namespaces.addAll(other.excludedNamespaces);
        List<ResourceLocation> items = new ArrayList<>(excludedItems);
        items.addAll(other.excludedItems);
        return new DetectionRules(List.copyOf(allKeywords), List.copyOf(words), List.copyOf(namespaces), List.copyOf(items));
    }
}
