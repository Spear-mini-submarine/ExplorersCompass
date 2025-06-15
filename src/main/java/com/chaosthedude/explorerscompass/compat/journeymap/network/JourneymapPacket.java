package com.chaosthedude.explorerscompass.compat.journeymap.network;

import journeymap.client.waypoint.Waypoint;
import journeymap.client.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.awt.*;
import java.util.function.Supplier;

public class JourneymapPacket {
    private final BlockPos blockPos;
    private final String name;
    private final ResourceKey<Level> dimension;

    public JourneymapPacket(BlockPos blockPos, String name, ResourceKey<Level> dimension) {
        this.blockPos = blockPos;
        this.name = name;
        this.dimension = dimension;
    }

    public JourneymapPacket(FriendlyByteBuf buf) {
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
            Waypoint waypoint = new Waypoint(name, blockPos, Color.RED, Waypoint.Type.Normal, dimension.toString(), true);
            WaypointStore.INSTANCE.add(waypoint);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty("已将路径点添加到JourneyMap"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}