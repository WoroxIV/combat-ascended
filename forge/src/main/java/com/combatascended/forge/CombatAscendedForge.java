package com.combatascended.forge;

import com.combatascended.CombatAscended;
import com.combatascended.platform.Platform;
import com.combatascended.registry.ModAttributes;
import com.combatascended.registry.ModStats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;


/** Point d'entrée Forge (chargé côté client et serveur). */
@Mod(CombatAscended.MODID)
public final class CombatAscendedForge {

    public CombatAscendedForge() {
        if (ModList.get().isLoaded("epicfight")) {
            throw new IllegalStateException("Combat Ascended and Epic Fight cannot be installed together: both replace the combat system. Remove one of the two.");
        }
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Platform.set(new ForgePlatform(modEventBus));
        CombatAscended.init();
        ForgeNetwork.register();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addAttributes);
        MinecraftForge.EVENT_BUS.register(ForgeGameEvents.class);
        if (FMLEnvironment.dist == Dist.CLIENT) CombatAscendedForgeClient.init(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModStats::registerFormats);
        CombatAscended.prepareConfigFolder();
    }

    /** Toutes les créatures reçoivent la pénétration d'armure (valeur 0) : joueurs, mais aussi mobs qui tiendraient une arme. */
    private void addAttributes(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            if (!event.has(type, ModAttributes.ARMOR_PENETRATION.get())) event.add(type, ModAttributes.ARMOR_PENETRATION.get());
        }
        event.add(EntityType.PLAYER, ModAttributes.SWEEPING_DAMAGE_RATIO.get());
    }
}
