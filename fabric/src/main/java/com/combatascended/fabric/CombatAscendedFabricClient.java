package com.combatascended.fabric;

import com.combatascended.CombatAscended;
import com.combatascended.client.AttackZoneRenderer;
import com.combatascended.client.CombatHud;
import com.combatascended.client.ModKeyMappings;
import com.combatascended.client.editor.ClientEditorHooks;
import com.combatascended.client.editor.WeaponEditorScreen;
import com.combatascended.client.tutorial.TutorialHintTracker;
import com.combatascended.data.WeaponData;
import com.combatascended.editor.EditorHooks;
import com.combatascended.event.TooltipEventHandler;
import com.combatascended.network.ComboSyncPayload;
import com.combatascended.network.DualSwingPayload;
import com.combatascended.network.EditorResultPayload;
import com.combatascended.network.OpenEditorPayload;
import com.combatascended.network.WeaponDataSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import com.combatascended.client.tutorial.TutorialRendering;
import com.combatascended.tutorial.DemoWeapon;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Point d'entrée client Fabric (jamais chargé sur un serveur dédié). */
public final class CombatAscendedFabricClient implements ClientModInitializer {
    /** Largeur et écart des boutons du menu Échap du jeu. */
    private static final int PAUSE_BUTTON_WIDTH = 204;
    private static final int BUTTON_GAP = 4;

    @Override
    public void onInitializeClient() {
        // Paquets du serveur pour l'éditeur (ouverture par /combatascended, résultat d'un enregistrement).
        EditorHooks.setClient(new ClientEditorHooks());
        // Le README et les schémas du dossier de config sont écrits dans la langue du jeu.
        CombatAscended.setLanguageSupplier(() -> Minecraft.getInstance().options.languageCode);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> CombatAscended.prepareConfigFolder());

        registerNetwork();
        registerRendering();
        registerEvents();
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.WEAPON_DETAILS);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.OPEN_TUTORIAL);
    }

    private static void registerNetwork() {
        ClientConfigurationNetworking.registerGlobalReceiver(WeaponDataSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> WeaponDataSyncPayload.handle(payload)));
        ClientPlayNetworking.registerGlobalReceiver(WeaponDataSyncPayload.TYPE, (payload, context) -> WeaponDataSyncPayload.handle(payload));
        ClientPlayNetworking.registerGlobalReceiver(ComboSyncPayload.TYPE, (payload, context) -> ComboSyncPayload.handle(payload, context.player()));
        ClientPlayNetworking.registerGlobalReceiver(DualSwingPayload.TYPE, (payload, context) -> DualSwingPayload.handle(payload, context.player()));
        ClientPlayNetworking.registerGlobalReceiver(OpenEditorPayload.TYPE, (payload, context) -> OpenEditorPayload.handle(payload));
        ClientPlayNetworking.registerGlobalReceiver(EditorResultPayload.TYPE, (payload, context) -> EditorResultPayload.handle(payload));
    }

    private void registerRendering() {
        // Les modèles des armes de démonstration des tutoriels, chargés sans objet associé.
        ModelLoadingPlugin.register(context -> context.addModels(DemoWeapon.allModels()));
        TutorialRendering.setModelLookup(id -> Minecraft.getInstance().getModelManager().getModel(id));

        HudRenderCallback.EVENT.register(CombatHud::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> AttackZoneRenderer.render(context.matrixStack(), context.camera().getPosition(),
                context.tickCounter().getGameTimeDeltaPartialTick(false)));
    }

    private static void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> TutorialHintTracker.onClientTick());
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> TooltipEventHandler.onTooltip(stack, lines, Minecraft.getInstance().player));
        // En quittant un serveur, on oublie ses stats : on revient aux valeurs par défaut du mod.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WeaponData.resetToBuiltin());
        // Une connexion qui échoue pendant le chargement ne passe pas par DISCONNECT, alors que les stats du serveur sont déjà reçues.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen && client.level == null) WeaponData.resetToBuiltin();
            if (screen instanceof PauseScreen pause && pause.showsPauseMenu() && WeaponEditorScreen.playerCanEdit()) addEditorButton(pause, width, height);
        });
    }

    /** Menu Échap : ouvre l'éditeur sans passer par la commande. Sous les boutons du jeu, pour ne pas les recouvrir. */
    private static void addEditorButton(Screen pauseScreen, int width, int height) {
        List<AbstractWidget> buttons = Screens.getButtons(pauseScreen);
        int bottom = buttons.stream().mapToInt(button -> button.getY() + button.getHeight()).max().orElse(height / 2);
        int y = Math.min(bottom + BUTTON_GAP, height - Button.DEFAULT_HEIGHT - BUTTON_GAP);
        buttons.add(Button.builder(WeaponEditorScreen.editorTitle(), pressed -> Minecraft.getInstance().setScreen(WeaponEditorScreen.fromModList(pauseScreen)))
                .bounds((width - PAUSE_BUTTON_WIDTH) / 2, y, PAUSE_BUTTON_WIDTH, Button.DEFAULT_HEIGHT).build());
    }
}
