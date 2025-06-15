package com.chaosthedude.explorerscompass.compat.journeymap.handler.impl;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.compat.journeymap.handler.IJourneymapHandler;
import com.chaosthedude.explorerscompass.compat.journeymap.network.JourneymapPacket;
import com.chaosthedude.explorerscompass.util.BlockPosUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

public class JourneymapHandlerImpl implements IJourneymapHandler {
    @Override
    public void addNavigationPoint(ServerPlayer serverPlayer, BlockPos pos, String name, ResourceKey<Level> dimension) {
        ExplorersCompass.network.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new JourneymapPacket(BlockPosUtils.findSafeLanding(serverPlayer.serverLevel(),pos), name, serverPlayer.level().dimension())
        );
    }
}
