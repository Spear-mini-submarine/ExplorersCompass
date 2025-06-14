package com.chaosthedude.explorerscompass.compat.xaerominimap;

import com.chaosthedude.explorerscompass.compat.xaerominimap.handler.IXaerominimapNavigationPointHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class XaerominimapNavigationPointApi {
    private static IXaerominimapNavigationPointHandler handler = null;

    public static void registerNavigationPointHandler(IXaerominimapNavigationPointHandler h) {
        handler = h;
    }

    public static void addNavigationPoint(ServerPlayer serverPlayer, BlockPos pos, String name, ResourceKey<Level> dimension) {
        if (handler != null) {
            handler.addNavigationPoint(serverPlayer,pos, name, dimension);
        }
    }
}
