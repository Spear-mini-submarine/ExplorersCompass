package com.chaosthedude.explorerscompass.compat.xaerominimap.handler.impl;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.compat.xaerominimap.handler.IXaerominimapHandler;
import com.chaosthedude.explorerscompass.compat.xaerominimap.network.XaerominimapPacket;
import com.chaosthedude.explorerscompass.util.BlockPosUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

public class XaerominimapHandlerImpl implements IXaerominimapHandler {
    @Override
    public void addNavigationPoint(ServerPlayer serverPlayer, BlockPos pos, String name, ResourceKey<Level> dimension) {
        ExplorersCompass.network.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new XaerominimapPacket(BlockPosUtils.findSafeLanding(serverPlayer.serverLevel(),pos), name, serverPlayer.level().dimension())
        );
    }
}
