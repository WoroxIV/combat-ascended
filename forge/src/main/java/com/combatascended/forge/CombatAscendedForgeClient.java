package com.combatascended.forge;

import com.combatascended.CombatAscended;
import com.combatascended.client.tutorial.TutorialRendering;
import com.combatascended.tutorial.DemoWeapon;
import com.combatascended.client.CombatHud;
import com.combatascended.client.ModKeyMappings;
import com.combatascended.client.editor.ClientEditorHooks;
import com.combatascended.client.editor.WeaponEditorScreen;
import com.combatascended.editor.EditorHooks;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;

/** Partie cliente du point d'entrée Forge (jamais chargée sur un serveur dédié). */
final class CombatAscendedForgeClient {
    private CombatAscendedForgeClient() {
    }

    static void init(IEventBus modEventBus) {
        // Bouton "Config" dans la liste des mods : l'éditeur des armes (avec une page pour les préférences d'affichage).
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> WeaponEditorScreen.fromModList(parent)));
        // Paquets du serveur pour l'éditeur (ouverture par /combatascended, résultat d'un enregistrement).
        EditorHooks.setClient(new ClientEditorHooks());
        // Le README et les schémas du dossier de config sont écrits dans la langue du jeu.
        CombatAscended.setLanguageSupplier(CombatAscendedForgeClient::gameLanguage);
        // Les modèles des armes de démonstration des tutoriels, chargés sans objet associé.
        TutorialRendering.setModelLookup(id -> Minecraft.getInstance().getModelManager().getModel(id));

        modEventBus.addListener(CombatAscendedForgeClient::registerGuiOverlays);
        modEventBus.addListener(CombatAscendedForgeClient::registerModels);
        modEventBus.addListener(CombatAscendedForgeClient::registerKeyMappings);
        MinecraftForge.EVENT_BUS.register(ForgeClientEvents.class);
    }

    private static void registerModels(ModelEvent.RegisterAdditional event) {
        DemoWeapon.allModels().forEach(event::register);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // "Interface" : pas de conflit avec Maj = s'accroupir, qui ne compte qu'en jeu.
        ModKeyMappings.WEAPON_DETAILS.setKeyConflictContext(KeyConflictContext.GUI);
        ModKeyMappings.OPEN_TUTORIAL.setKeyConflictContext(KeyConflictContext.IN_GAME);
        event.register(ModKeyMappings.WEAPON_DETAILS);
        event.register(ModKeyMappings.OPEN_TUTORIAL);
    }

    /** Éléments de combat à l'écran, dessinés juste au-dessus du viseur vanilla. */
    private static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "combat_hud", (gui, graphics, partialTick, width, height) -> CombatHud.render(graphics));
    }

    /** Langue choisie dans les options du jeu (ex : "fr_fr"), ou l'anglais si les options ne sont pas encore chargées. */
    private static String gameLanguage() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.options != null ? minecraft.options.languageCode : "en_us";
    }
}
