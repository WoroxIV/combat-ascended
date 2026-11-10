package com.combatreforged.forge;

import com.combatreforged.CombatReforged;
import com.combatreforged.platform.Platform;
import com.combatreforged.registry.ModAttributes;
import com.combatreforged.registry.ModStats;
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
@Mod(CombatReforged.MODID)
public final class CombatReforgedForge {

    public CombatReforgedForge() {
        if (ModList.get().isLoaded("epicfight")) {
            throw new IllegalStateException("Combat Reforged and Epic Fight cannot be installed together: both replace the combat system. Remove one of the two.");
        }
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Platform.set(new ForgePlatform(modEventBus));
        CombatReforged.init();
        ForgeNetwork.register();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addAttributes);
        MinecraftForge.EVENT_BUS.register(ForgeGameEvents.class);
        if (FMLEnvironment.dist == Dist.CLIENT) CombatReforgedForgeClient.init(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModStats::registerFormats);
        CombatReforged.prepareConfigFolder();
    }

    /** Toutes les créatures reçoivent la pénétration d'armure (valeur 0) : joueurs, mais aussi mobs qui tiendraient une arme. */
    private void addAttributes(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            if (!event.has(type, ModAttributes.ARMOR_PENETRATION.get())) event.add(type, ModAttributes.ARMOR_PENETRATION.get());
        }
        event.add(EntityType.PLAYER, ModAttributes.SWEEPING_DAMAGE_RATIO.get());
    }
}
