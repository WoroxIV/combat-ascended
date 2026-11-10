package com.combatreforged.fabric;

import com.combatreforged.CombatReforged;
import com.combatreforged.client.AttackZoneRenderer;
import com.combatreforged.client.CombatHud;
import com.combatreforged.client.ModKeyMappings;
import com.combatreforged.client.editor.ClientEditorHooks;
import com.combatreforged.client.editor.WeaponEditorScreen;
import com.combatreforged.client.tutorial.TutorialHintTracker;
import com.combatreforged.data.WeaponData;
import com.combatreforged.editor.EditorHooks;
import com.combatreforged.event.TooltipEventHandler;
import com.combatreforged.network.ComboSyncPayload;
import com.combatreforged.network.DualSwingPayload;
import com.combatreforged.network.EditorResultPayload;
import com.combatreforged.network.OpenEditorPayload;
import com.combatreforged.network.WeaponDataSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import com.combatreforged.client.tutorial.TutorialRendering;
import com.combatreforged.tutorial.DemoWeapon;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.List;

/** Point d'entrée client Fabric (jamais chargé sur un serveur dédié). */
public final class CombatReforgedFabricClient implements ClientModInitializer {
    /** Largeur et écart des boutons du menu Échap du jeu. */
    private static final int PAUSE_BUTTON_WIDTH = 204;
    private static final int BUTTON_GAP = 4;

    @Override
    public void onInitializeClient() {
        // Paquets du serveur pour l'éditeur (ouverture par /combatreforged, résultat d'un enregistrement).
        EditorHooks.setClient(new ClientEditorHooks());
        // Le README et les schémas du dossier de config sont écrits dans la langue du jeu.
        CombatReforged.setLanguageSupplier(() -> Minecraft.getInstance().options.languageCode);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> CombatReforged.prepareConfigFolder());

        registerNetwork();
        registerRendering();
        registerEvents();
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.WEAPON_DETAILS);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.OPEN_TUTORIAL);
    }

    private static void registerNetwork() {
        FabricNetwork.toClient(WeaponDataSyncPayload.class, WeaponDataSyncPayload::read, (payload, player) -> WeaponDataSyncPayload.handle(payload));
        FabricNetwork.toClient(ComboSyncPayload.class, ComboSyncPayload::read, ComboSyncPayload::handle);
        FabricNetwork.toClient(DualSwingPayload.class, DualSwingPayload::read, DualSwingPayload::handle);
        FabricNetwork.toClient(OpenEditorPayload.class, OpenEditorPayload::read, (payload, player) -> OpenEditorPayload.handle(payload));
        FabricNetwork.toClient(EditorResultPayload.class, EditorResultPayload::read, (payload, player) -> EditorResultPayload.handle(payload));
    }

    private void registerRendering() {
        // Les modèles des armes de démonstration des tutoriels, chargés sans objet associé.
        ModelLoadingPlugin.register(context -> context.addModels(DemoWeapon.allModels()));
        TutorialRendering.setModelLookup(id -> BakedModelManagerHelper.getModel(Minecraft.getInstance().getModelManager(), id));

        HudRenderCallback.EVENT.register((graphics, partialTick) -> CombatHud.render(graphics));
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> AttackZoneRenderer.render(context.matrixStack(), context.camera().getPosition(), context.tickDelta()));
    }

    private static void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> TutorialHintTracker.onClientTick());
        ItemTooltipCallback.EVENT.register((stack, flag, lines) -> TooltipEventHandler.onTooltip(stack, lines, Minecraft.getInstance().player));
        // En quittant un serveur, on oublie ses stats : on revient aux valeurs par défaut du mod.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WeaponData.resetToBuiltin());
        // Une connexion qui échoue pendant le chargement ne passe pas par DISCONNECT, alors que les stats du serveur sont déjà reçues.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen && client.level == null) WeaponData.resetToBuiltin();
            if (screen instanceof PauseScreen pause && WeaponEditorScreen.playerCanEdit()) addEditorButton(pause, width, height);
        });
    }

    /** Menu Échap : ouvre l'éditeur sans passer par la commande. Sous les boutons du jeu, pour ne pas les recouvrir. */
    private static void addEditorButton(Screen pauseScreen, int width, int height) {
        List<AbstractWidget> buttons = Screens.getButtons(pauseScreen);
        // F3+Échap met le jeu en pause sans afficher de menu.
        if (buttons.isEmpty()) return;
        int bottom = buttons.stream().mapToInt(button -> button.getY() + button.getHeight()).max().orElse(height / 2);
        int y = Math.min(bottom + BUTTON_GAP, height - Button.DEFAULT_HEIGHT - BUTTON_GAP);
        buttons.add(Button.builder(WeaponEditorScreen.editorTitle(), pressed -> Minecraft.getInstance().setScreen(WeaponEditorScreen.fromModList(pauseScreen)))
                .bounds((width - PAUSE_BUTTON_WIDTH) / 2, y, PAUSE_BUTTON_WIDTH, Button.DEFAULT_HEIGHT).build());
    }
}
