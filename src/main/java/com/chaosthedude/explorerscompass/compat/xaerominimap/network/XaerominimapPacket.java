package com.chaosthedude.explorerscompass.compat.xaerominimap.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.HudSession;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.*;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;

import java.util.function.Supplier;

/**
 * Xaero's Minimap的联动网络包，用于处理导航点的添加。
 */
public class XaerominimapPacket {
    private final BlockPos blockPos;
    private final String name;
    private final ResourceKey<Level> dimension;

    public XaerominimapPacket(BlockPos blockPos, String name, ResourceKey<Level> dimension) {
        this.blockPos = blockPos;
        this.name = name;
        this.dimension = dimension;
    }

    public XaerominimapPacket(FriendlyByteBuf buf) {
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
            HudSession forPlayer = HudSession.getCurrentSession();

            MinimapSession session = forPlayer.getSession(BuiltInHudModules.MINIMAP);
            MinimapWorldManager worldManager = session.getWorldManager();
            MinimapWorld currentWorld = worldManager.getCurrentWorld();
            String setId = currentWorld.getCurrentWaypointSetId();
            WaypointSet waypointSet = currentWorld.getWaypointSet(setId);

            //构造目标点
            Waypoint waypoint = new Waypoint(blockPos.getX(), blockPos.getY(), blockPos.getZ(), name, "QVQ", WaypointColor.RED,WaypointPurpose.DESTINATION);

            waypointSet.add(waypoint);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty("已将路径点添加到Xaero"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
