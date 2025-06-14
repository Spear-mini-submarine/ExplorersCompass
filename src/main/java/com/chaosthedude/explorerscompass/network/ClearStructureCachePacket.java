package com.chaosthedude.explorerscompass.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.chaosthedude.explorerscompass.cache.PlayerStructureCache;

import java.util.function.Supplier;

/**
 * 清除玩家结构缓存的网络数据包。
 */
public class ClearStructureCachePacket {

    // 无需成员变量

    public ClearStructureCachePacket() {}

    public ClearStructureCachePacket(FriendlyByteBuf buf) {
        // 无数据
    }

    public void toBytes(FriendlyByteBuf buf) {
        // 无数据，无需写
    }

    public static void handle(ClearStructureCachePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PlayerStructureCache.clearCache(player);
                player.sendSystemMessage(Component.nullToEmpty("成功清除结构缓存。"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
