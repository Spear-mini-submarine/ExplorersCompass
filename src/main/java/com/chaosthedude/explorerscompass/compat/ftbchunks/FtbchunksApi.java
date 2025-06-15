package com.chaosthedude.explorerscompass.compat.ftbchunks;

import com.chaosthedude.explorerscompass.compat.ftbchunks.handler.IFtbchunksHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class FtbchunksApi {
    private static IFtbchunksHandler handler = null;

    public static void registerNavigationPointHandler(IFtbchunksHandler h) {
        handler = h;
    }

    public static void addNavigationPoint(ServerPlayer serverPlayer, BlockPos pos, String name, ResourceKey<Level> dimension) {
        if (handler != null) {
            handler.addNavigationPoint(serverPlayer,pos, name, dimension);
        }
    }
}
