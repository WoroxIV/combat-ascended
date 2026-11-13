package com.combatascended.combat;

import net.minecraft.world.entity.LivingEntity;
import com.combatascended.compat.CombatCompat;
import com.combatascended.data.RulesDefinition;
import com.combatascended.data.WeaponData;
import com.combatascended.data.WeaponDefinition;
import com.combatascended.platform.Platform;
import com.combatascended.registry.ModAttributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Le comportement des objets qui ont un profil d'arme (voir {@link WeaponProfiles}), branché sur les événements du jeu :
 * un mod d'armes n'a rien à appeler. Les effets à l'impact, la pose et la zone de balayage
 * sont dans ItemStackMixin et PlayerCombatMixin.
 */
public final class AdoptedItemEvents {
    private static final double PERCENT = 100.0D;

    private AdoptedItemEvents() {
    }

    /**
     * Clic droit : lance la capacité de l'arme si ses conditions sont remplies. Appelé côté client puis côté serveur.
     *
     * @return vrai si la capacité de l'arme a été utilisée
     */
    public static boolean onRightClickItem(Player player, InteractionHand hand, ItemStack stack, Level level) {
        if (!WeaponProfiles.isAdopted(stack)) return false;
        if (ParrySystem.canParry(player, hand, stack)) {
            ParrySystem.beginParry(player, stack);
            player.startUsingItem(hand);
        } else if (DashAbility.canDash(player, hand, stack)) {
            DashAbility.dash(level, player, stack, DashAbility.dashOf(stack).orElseThrow());
        } else {
            return false;
        }
        return true;
    }

    /** Portée, pénétration d'armure et balayage de l'arme, dans la main principale ; les dégâts et la vitesse restent ceux de l'objet. */
    public static void collectModifiers(ItemStack stack, BiConsumer<Attribute, AttributeModifier> output) {
        Optional<WeaponProfiles.Profile> profile = WeaponProfiles.of(stack);
        if (profile.isEmpty()) return;

        RulesDefinition rules = WeaponData.rules();
        WeaponDefinition definition = profile.get().definition();
        if (rules.reachActive() && definition.reachBonus() != 0.0D) {
            output.accept(Platform.get().entityReachAttribute(), new AttributeModifier(WeaponActions.BASE_REACH_ID, "Weapon modifier", definition.reachBonus(), AttributeModifier.Operation.ADDITION));
        }
        if (rules.armorPenetrationActive() && definition.armorPenetration() > 0.0D) {
            output.accept(ModAttributes.armorPenetration(), new AttributeModifier(ModAttributes.BASE_ARMOR_PENETRATION_ID, "Weapon modifier",
                    definition.armorPenetration() / PERCENT, AttributeModifier.Operation.ADDITION));
        }
        if (rules.sweepActive() && definition.sweep().enabled() && definition.sweep().damageRatio() > 0.0D) {
            output.accept(ModAttributes.SWEEPING_DAMAGE_RATIO.get(), new AttributeModifier(WeaponActions.BASE_SWEEPING_RATIO_ID, "Weapon modifier",
                    definition.sweep().damageRatio() / PERCENT, AttributeModifier.Operation.ADDITION));
        }
    }

    /** Objet adopté par une arme qui désactive les boucliers (champ "items"). */
    public static boolean disablesShield(ItemStack weapon, LivingEntity attacker) {
        return WeaponData.rules().shieldDisablingActive() && WeaponProfiles.isAdopted(weapon) && !CombatCompat.isFakePlayer(attacker)
                && WeaponProfiles.definition(weapon).orElseThrow().disablesShield();
    }
}
