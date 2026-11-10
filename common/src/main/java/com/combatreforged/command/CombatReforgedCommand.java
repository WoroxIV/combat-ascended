package com.combatreforged.command;

import com.combatreforged.CombatReforged;
import com.combatreforged.combat.WeaponAudit;
import com.combatreforged.data.WeaponDataLoader;
import com.combatreforged.editor.EditorService;
import com.combatreforged.network.OpenEditorPayload;
import com.combatreforged.platform.Platform;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Commande /combatreforged :
 * <ul>
 *   <li>{@code /combatreforged} : l'éditeur pour les opérateurs (et l'hôte d'une partie solo), le Codex pour les autres ;</li>
 *   <li>{@code editor} / {@code codex} : l'un ou l'autre explicitement ;</li>
 *   <li>{@code scan} : liste chaque arme, son archétype et la raison de ce choix (opérateurs) ;</li>
 *   <li>{@code reload} : recharge seulement les données de combat (opérateurs).</li>
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
                .then(Commands.literal("scan")
                        .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                        .executes(CombatReforgedCommand::scan))
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

    private static int scan(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WeaponAudit.Report report = WeaponAudit.run();
        try {
            String file = WeaponAudit.export(report);
            source.sendSuccess(() -> Component.translatable(KEY + "scan.result", report.entries().size(), report.ignored().size(), file), false);
        } catch (IOException e) {
            CombatReforged.LOGGER.error("[Combat Reforged] Could not write the scan report", e);
            source.sendFailure(Component.translatable(KEY + "scan.failed", e.getMessage()));
            return 0;
        }
        report.bySource().forEach((kind, count) -> source.sendSuccess(() -> Component.translatable(KEY + "scan.line", kind, count), false));
        return report.entries().size();
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        WeaponDataLoader.Result result = EditorService.reloadWeaponData(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "reloaded", result.problems().size()), true);
        return 1;
    }
}
