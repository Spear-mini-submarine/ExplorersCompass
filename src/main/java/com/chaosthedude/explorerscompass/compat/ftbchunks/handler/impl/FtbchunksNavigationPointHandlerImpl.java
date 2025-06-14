package com.chaosthedude.explorerscompass.compat.ftbchunks.handler.impl;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.compat.ftbchunks.handler.IFtbchunksNavigationPointHandler;
import com.chaosthedude.explorerscompass.compat.ftbchunks.network.FtbchunksNavigationPointPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

public class FtbchunksNavigationPointHandlerImpl implements IFtbchunksNavigationPointHandler {
    @Override
    public void addNavigationPoint(ServerPlayer serverPlayer, BlockPos pos, String name, ResourceKey<Level> dimension) {
        ExplorersCompass.network.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new FtbchunksNavigationPointPacket(findSafeLanding(serverPlayer.serverLevel(),pos), name, serverPlayer.level().dimension())
        );
    }
    private static BlockPos findSafeLanding(ServerLevel world, BlockPos start) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(start.getX(), start.getY(), start.getZ());
        // 向下查找
        for (int y = start.getY(); y > world.getMinBuildHeight(); y--) {
            pos.setY(y);
            BlockState state = world.getBlockState(pos);
            boolean sturdy = state.isFaceSturdy(world, pos, Direction.UP);
            BlockState above = world.getBlockState(pos.above());
            if (sturdy && above.isAir()) {
                return pos;
            }
        }
        return start; // 没有找到安全位置
    }
}
