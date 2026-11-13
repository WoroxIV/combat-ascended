package com.combatascended.registry;

import com.combatascended.CombatAscended;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/** Tags du mod, modifiables par datapack comme n'importe quel tag. */
public final class ModTags {
    /** Objets utilisables en dual wield sans être déclarés dans un fichier d'arme. Vide par défaut. */
    public static final TagKey<Item> DUAL_WIELDABLE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(CombatAscended.MODID, "dual_wieldable"));

    /**
     * Objets qui comptent comme des armes de mêlée (ligne de portée du tooltip, dash avec une arme dans l'autre main).
     * Par défaut : épées, haches, trident et le tag commun des armes de mêlée ({@code c:tools/melee_weapon}).
     */
    public static final TagKey<Item> MELEE_WEAPONS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(CombatAscended.MODID, "melee_weapons"));

    /** Armes qui peuvent recevoir Riposte, en plus de celles dont le profil pare. */
    public static final TagKey<Item> ENCHANTABLE_PARRY_WEAPONS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(CombatAscended.MODID, "enchantable/parry_weapons"));
    /** Ce qui peut recevoir Reflective, en plus des boucliers et des armes dont le profil pare. */
    public static final TagKey<Item> ENCHANTABLE_PARRYING = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(CombatAscended.MODID, "enchantable/parrying"));
    /** Armes qui peuvent recevoir Élan, en plus de celles dont le profil dash. */
    public static final TagKey<Item> ENCHANTABLE_DASHING = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(CombatAscended.MODID, "enchantable/dashing"));

    /** Projectiles récupérables par leur lanceur (shurikens...) : les renvoyer les lui ferait perdre. */
    public static final TagKey<EntityType<?>> NOT_REFLECTED_WHEN_PICKABLE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(CombatAscended.MODID, "not_reflected_when_pickable"));

    private ModTags() {
    }
}
