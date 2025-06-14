package com.chaosthedude.explorerscompass.events;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.cache.PlayerStructureCache;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExplorersCompass.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {
    @SubscribeEvent
    public static void onServerStartedEvent(ServerStartedEvent event) {
        PlayerStructureCache.init(event.getServer());
    }
}
