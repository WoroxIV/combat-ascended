package com.combatreforged.fabric;

import com.combatreforged.CombatReforged;
import com.combatreforged.client.AttackZoneRenderer;
import com.combatreforged.client.CombatHud;
import com.combatreforged.client.ModKeyMappings;
import com.combatreforged.client.RangedWeaponClient;
import com.combatreforged.client.ShieldItemRenderer;
import com.combatreforged.client.ShurikenRenderer;
import com.combatreforged.client.WeaponPaletteSource;
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
import com.combatreforged.registry.ModEntities;
import com.combatreforged.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Point d'entrée client Fabric (jamais chargé sur un serveur dédié). */
public final class CombatReforgedFabricClient implements ClientModInitializer {
    /** Largeur et écart des boutons du menu Échap du jeu. */
    private static final int PAUSE_BUTTON_WIDTH = 204;
    private static final int BUTTON_GAP = 4;

    private ShieldItemRenderer shieldRenderer;

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
        ClientConfigurationNetworking.registerGlobalReceiver(WeaponDataSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> WeaponDataSyncPayload.handle(payload)));
        ClientPlayNetworking.registerGlobalReceiver(WeaponDataSyncPayload.TYPE, (payload, context) -> WeaponDataSyncPayload.handle(payload));
        ClientPlayNetworking.registerGlobalReceiver(ComboSyncPayload.TYPE, (payload, context) -> ComboSyncPayload.handle(payload, context.player()));
        ClientPlayNetworking.registerGlobalReceiver(DualSwingPayload.TYPE, (payload, context) -> DualSwingPayload.handle(payload, context.player()));
        ClientPlayNetworking.registerGlobalReceiver(OpenEditorPayload.TYPE, (payload, context) -> OpenEditorPayload.handle(payload));
        ClientPlayNetworking.registerGlobalReceiver(EditorResultPayload.TYPE, (payload, context) -> EditorResultPayload.handle(payload));
    }

    private void registerRendering() {
        SpriteSources.TYPES.put(WeaponPaletteSource.ID, WeaponPaletteSource.TYPE);
        EntityRendererRegistry.register(ModEntities.SHURIKEN.get(), ShurikenRenderer::new);

        ModItems.allShields().forEach(shield -> {
            BuiltinItemRendererRegistry.INSTANCE.register(shield.get(), (stack, mode, pose, buffers, light, overlay) -> {
                // Créé au premier affichage : le jeu n'est pas encore prêt au démarrage.
                if (shieldRenderer == null) shieldRenderer = new ShieldItemRenderer();
                shieldRenderer.renderByItem(stack, mode, pose, buffers, light, overlay);
            });
            // La pose "bouclier levé" est un modèle différent, choisi par cette propriété (comme pour le bouclier vanilla).
            ItemProperties.register(shield.get(), ResourceLocation.withDefaultNamespace("blocking"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
        RangedWeaponClient.registerItemProperties();

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
