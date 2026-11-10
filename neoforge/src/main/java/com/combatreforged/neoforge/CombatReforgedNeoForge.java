package com.combatreforged.neoforge;

import com.combatreforged.CombatReforged;
import com.combatreforged.datagen.lang.ModLanguageProvider;
import com.combatreforged.platform.Platform;
import com.combatreforged.registry.ModStats;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Point d'entrée NeoForge (chargé côté client et serveur). */
@Mod(CombatReforged.MODID)
public final class CombatReforgedNeoForge {

    public CombatReforgedNeoForge(IEventBus modEventBus) {
        Platform.set(new NeoForgePlatform(modEventBus));
        CombatReforged.init();

        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModStats::registerFormats);
        CombatReforged.prepareConfigFolder();
    }

    /** Lancé par "gradlew :neoforge:runData" : génère les traductions. */
    private void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(event.includeClient(), new ModLanguageProvider(output));
    }
}
