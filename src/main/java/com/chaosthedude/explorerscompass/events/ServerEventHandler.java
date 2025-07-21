package com.chaosthedude.explorerscompass.events;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.cache.PlayerStructureCache;
import com.chaosthedude.explorerscompass.cache.SharedStructureCache;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExplorersCompass.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {
    private static int tickCounter = 0;
    private static final int AUTO_SAVE_INTERVAL = 10 * 60 * 20;
    @SubscribeEvent
    public static void onServerStartedEvent(ServerStartedEvent event) {
        PlayerStructureCache.init(event.getServer());
    }
    @SubscribeEvent
    public static void onServerStoppingEvent(ServerStoppingEvent event) {
        SharedStructureCache.saveToDisk();
    }
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER) {
            tickCounter++;

            // 每10分钟检查一次是否需要保存
            if (tickCounter >= AUTO_SAVE_INTERVAL) {
                tickCounter = 0;
                ExplorersCompass.LOGGER.debug("Auto-saving shared structure cache...");
                SharedStructureCache.saveToDisk();
            }
        }
    }
}
