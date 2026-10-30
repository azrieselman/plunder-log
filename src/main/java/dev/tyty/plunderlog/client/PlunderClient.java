package dev.tyty.plunderlog.client;

import dev.tyty.plunderlog.PlunderLog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = PlunderLog.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = PlunderLog.MOD_ID, value = Dist.CLIENT)
public class PlunderClient {
    public PlunderClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(XaeroWaypoints::onClientTick);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        PlunderLog.LOGGER.info("Plunder Log client ready");
    }
}
