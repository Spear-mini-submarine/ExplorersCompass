package com.chaosthedude.explorerscompass.compat.ftbchunks.network;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class FtbchunksNavigationPointPacket {
    private final BlockPos blockPos;
    private final String name;
    private final ResourceKey<Level> dimension;

    public FtbchunksNavigationPointPacket(BlockPos blockPos, String name, ResourceKey<Level> dimension) {
        this.blockPos = blockPos;
        this.name = name;
        this.dimension = dimension;
    }

    public FtbchunksNavigationPointPacket(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
        this.name = buf.readUtf(32767);
        this.dimension = buf.readResourceKey(Registries.DIMENSION);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeUtf(name);
        buf.writeResourceKey(dimension);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 获取对应维度的 WaypointManager
            Optional<WaypointManager> optManager = FTBChunksAPI.clientApi().getWaypointManager(dimension);
            if (optManager.isPresent()) {
                WaypointManager waypointManager = optManager.get();
                Waypoint waypoint = waypointManager.addWaypointAt(blockPos, this.name);
                waypoint.setHidden(false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
