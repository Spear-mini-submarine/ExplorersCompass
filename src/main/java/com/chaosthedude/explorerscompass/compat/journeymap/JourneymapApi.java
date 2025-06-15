package com.chaosthedude.explorerscompass.compat.journeymap;

import com.chaosthedude.explorerscompass.compat.journeymap.handler.IJourneymapHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class JourneymapApi {
    private static IJourneymapHandler handler = null;

    public static void registerNavigationPointHandler(IJourneymapHandler h) {
        handler = h;
    }

    public static void addNavigationPoint(ServerPlayer serverPlayer, BlockPos pos, String name, ResourceKey<Level> dimension) {
        if (handler != null) {
            handler.addNavigationPoint(serverPlayer,pos, name, dimension);
        }
    }
}
