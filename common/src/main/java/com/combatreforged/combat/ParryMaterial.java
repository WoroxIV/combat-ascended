package com.combatreforged.combat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Famille de matériau d'une arme, qui décide du son et des particules d'une parade parfaite.
 * Les sons sont dans assets/combat_reforged/sounds.json : un pack de ressources peut les remplacer.
 */
public enum ParryMaterial {
    WOOD("wood", List.of(blocks(Blocks.OAK_PLANKS))),
    STONE("stone", List.of(blocks(Blocks.STONE))),
    METAL("metal", List.of(ParticleTypes.CRIT, ParticleTypes.ELECTRIC_SPARK)),
    GOLD("gold", List.of(blocks(Blocks.GOLD_BLOCK), ParticleTypes.ELECTRIC_SPARK)),
    DIAMOND("diamond", List.of(blocks(Blocks.DIAMOND_BLOCK), ParticleTypes.END_ROD)),
    NETHERITE("netherite", List.of(ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.LARGE_SMOKE));

    private final String id;
    private final List<ParticleOptions> particles;

    ParryMaterial(String id, List<ParticleOptions> particles) {
        this.id = id;
        this.particles = particles;
    }

    public String id() {
        return id;
    }

    public List<ParticleOptions> particles() {
        return particles;
    }

    /** Famille de cet objet. Tout ce qui n'est pas reconnu (matériaux d'autres mods, objets sans palier) est du métal. */
    public static ParryMaterial of(ItemStack stack) {
        if (stack.is(Items.SHIELD)) return WOOD;
        ParryMaterial named = fromName(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        if (named != null) return named;
        if (stack.getItem() instanceof TieredItem tiered) return of(tiered.getTier());
        return METAL;
    }

    /** Le premier mot d'un identifiant comme "golden_katana" : marche pour tous les mods qui suivent la convention vanilla. */
    private static ParryMaterial fromName(String path) {
        return switch (path.split("_")[0]) {
            case "wooden", "wood" -> WOOD;
            case "stone" -> STONE;
            case "golden", "gold" -> GOLD;
            case "diamond" -> DIAMOND;
            case "netherite" -> NETHERITE;
            default -> null;
        };
    }

    private static ParryMaterial of(Tier tier) {
        if (tier == Tiers.WOOD) return WOOD;
        if (tier == Tiers.STONE) return STONE;
        if (tier == Tiers.GOLD) return GOLD;
        if (tier == Tiers.DIAMOND) return DIAMOND;
        if (tier == Tiers.NETHERITE) return NETHERITE;
        return METAL;
    }

    private static ParticleOptions blocks(net.minecraft.world.level.block.Block block) {
        return new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState());
    }
}
