package com.chaosthedude.explorerscompass.compat.xaerominimap.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public interface IXaerominimapHandler {
    void addNavigationPoint(ServerPlayer serverPlayer, BlockPos pos, String name, ResourceKey<Level> dimension);
}
