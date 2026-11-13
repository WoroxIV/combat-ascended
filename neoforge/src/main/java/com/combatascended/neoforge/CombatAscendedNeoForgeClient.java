package com.combatascended.neoforge;

import com.combatascended.CombatAscended;
import com.combatascended.client.CombatHud;
import com.combatascended.client.ModKeyMappings;
import com.combatascended.client.editor.ClientEditorHooks;
import com.combatascended.client.editor.WeaponEditorScreen;
import com.combatascended.client.tutorial.TutorialRendering;
import com.combatascended.editor.EditorHooks;
import com.combatascended.tutorial.DemoWeapon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Point d'entrée client NeoForge (jamais chargé sur un serveur dédié). */
@Mod(value = CombatAscended.MODID, dist = Dist.CLIENT)
public final class CombatAscendedNeoForgeClient {

    public CombatAscendedNeoForgeClient(IEventBus modEventBus, ModContainer container) {
        // Bouton "Config" dans la liste des mods : l'éditeur (avec une page pour les préférences d'affichage).
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> WeaponEditorScreen.fromModList(parent));
        // Paquets du serveur pour l'éditeur (ouverture par /combatascended, résultat d'un enregistrement).
        EditorHooks.setClient(new ClientEditorHooks());

        // Le README et les schémas du dossier de config sont écrits dans la langue du jeu.
        CombatAscended.setLanguageSupplier(CombatAscendedNeoForgeClient::gameLanguage);
        // Les modèles des armes de démonstration des tutoriels, chargés sans objet associé.
        TutorialRendering.setModelLookup(id -> Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(id)));

        modEventBus.addListener(CombatAscendedNeoForgeClient::registerGuiLayers);
        modEventBus.addListener(CombatAscendedNeoForgeClient::registerKeyMappings);
        modEventBus.addListener(CombatAscendedNeoForgeClient::registerModels);
    }

    private static void registerModels(ModelEvent.RegisterAdditional event) {
        DemoWeapon.allModels().forEach(id -> event.register(ModelResourceLocation.standalone(id)));
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // "Interface" : pas de conflit avec Maj = s'accroupir, qui ne compte qu'en jeu.
        ModKeyMappings.WEAPON_DETAILS.setKeyConflictContext(KeyConflictContext.GUI);
        ModKeyMappings.OPEN_TUTORIAL.setKeyConflictContext(KeyConflictContext.IN_GAME);
        event.register(ModKeyMappings.WEAPON_DETAILS);
        event.register(ModKeyMappings.OPEN_TUTORIAL);
    }

    /** Éléments de combat à l'écran, dessinés juste au-dessus du viseur vanilla. */
    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(CombatAscended.MODID, "combat_hud"), CombatHud::render);
    }

    /** Langue choisie dans les options du jeu (ex : "fr_fr"), ou l'anglais si les options ne sont pas encore chargées. */
    private static String gameLanguage() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.options != null ? minecraft.options.languageCode : "en_us";
    }
}
