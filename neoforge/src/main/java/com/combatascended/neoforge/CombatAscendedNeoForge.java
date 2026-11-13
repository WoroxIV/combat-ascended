package com.combatascended.neoforge;

import com.combatascended.CombatAscended;
import com.combatascended.datagen.lang.ModLanguageProvider;
import com.combatascended.platform.Platform;
import com.combatascended.registry.ModStats;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Point d'entrée NeoForge (chargé côté client et serveur). */
@Mod(CombatAscended.MODID)
public final class CombatAscendedNeoForge {

    public CombatAscendedNeoForge(IEventBus modEventBus) {
        Platform.set(new NeoForgePlatform(modEventBus));
        CombatAscended.init();

        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModStats::registerFormats);
        CombatAscended.prepareConfigFolder();
    }

    /** Lancé par "gradlew :neoforge:runData" : génère les traductions. */
    private void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(event.includeClient(), new ModLanguageProvider(output));
    }
}
