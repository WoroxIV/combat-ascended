package com.combatreforged.command;

import com.mojang.brigadier.CommandDispatcher;
import com.combatreforged.platform.Platform;
import com.combatreforged.CombatReforged;
import com.combatreforged.config.Docs;
import com.combatreforged.data.WeaponData;
import com.combatreforged.data.WeaponDataLoader;
import com.combatreforged.data.WeaponDefinition;
import com.combatreforged.editor.EditorService;
import com.combatreforged.item.WeaponMaterials;
import com.combatreforged.item.WeaponType;
import com.combatreforged.network.OpenEditorPayload;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Commande /combatreforged :
 * <ul>
 *   <li>{@code /combatreforged} : l'éditeur pour les opérateurs (et l'hôte d'une partie solo), le Codex pour les autres ;</li>
 *   <li>{@code editor} / {@code codex} : l'un ou l'autre explicitement ;</li>
 *   <li>{@code dps} : dégâts par seconde de chaque arme, comparés à l'épée en fer vanilla ;</li>
 *   <li>{@code reload} : recharge seulement les données des armes (opérateurs).</li>
 * </ul>
 */
public final class CombatReforgedCommand {
    private static final String KEY = CombatReforged.MODID + ".command.";
    private static final int OPERATOR_LEVEL = 2;

    private CombatReforgedCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("combatreforged")
                .executes(context -> open(context, null))
                .then(Commands.literal("editor").executes(context -> open(context, true)))
                .then(Commands.literal("codex").executes(context -> open(context, false)))
                .then(Commands.literal("dps").executes(CombatReforgedCommand::dps))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                        .executes(CombatReforgedCommand::reload)));
    }

    /** @param editor true = éditeur, false = Codex, null = selon les droits du joueur */
    private static int open(CommandContext<CommandSourceStack> context, @Nullable Boolean editor) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean canEdit = EditorService.canEdit(player);
        if (Boolean.TRUE.equals(editor) && !canEdit) {
            context.getSource().sendFailure(Component.translatable(KEY + "no_permission"));
            return 0;
        }
        Platform.get().sendToPlayer(player, new OpenEditorPayload(editor == null ? canEdit : editor));
        return 1;
    }

    private static int dps(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable(KEY + "dps.header", Docs.number(WeaponDefinition.VANILLA_IRON_SWORD_DPS))
                .withStyle(ChatFormatting.GOLD), false);
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            double iron = definition.dps(WeaponMaterials.IRON);
            double netherite = definition.dps(WeaponMaterials.NETHERITE);
            Component line = Component.translatable(KEY + "dps.line",
                    Component.translatable(CombatReforged.MODID + ".configuration." + type.id()),
                    Docs.number(iron), Docs.number(netherite), Docs.number(iron / WeaponDefinition.VANILLA_IRON_SWORD_DPS));
            source.sendSuccess(() -> line, false);
        }
        return WeaponType.values().length;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        WeaponDataLoader.Result result = EditorService.reloadWeaponData(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "reloaded", result.problems().size()), true);
        return 1;
    }
}
