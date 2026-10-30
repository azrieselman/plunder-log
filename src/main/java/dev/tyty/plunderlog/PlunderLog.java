package dev.tyty.plunderlog;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.tyty.plunderlog.net.PlunderNetworking;
import dev.tyty.plunderlog.world.ClearTracker;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(PlunderLog.MOD_ID)
public class PlunderLog {
    public static final String MOD_ID = "plunderlog";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlunderLog(IEventBus modBus, ModContainer container) {
        PlunderItems.ITEMS.register(modBus);
        modBus.addListener(PlunderItems::addToTabs);
        modBus.addListener(PlunderNetworking::register);
        container.registerConfig(ModConfig.Type.COMMON, PlunderConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(ClearTracker::onContainerClose);
        NeoForge.EVENT_BUS.addListener(ClearTracker::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(ClearTracker::onPlayerLogin);
    }
}
