package com.chaosthedude.explorerscompass.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class BlockPosUtils {
    public static BlockPos findSafeLanding(ServerLevel world, BlockPos start) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(start.getX(), 255, start.getZ());
        for (int y = 255; y > world.getMinBuildHeight(); y--) {
            pos.setY(y);
            BlockState state = world.getBlockState(pos);
            boolean sturdy = state.isFaceSturdy(world, pos, Direction.UP);
            BlockState above = world.getBlockState(pos.above());
            if (sturdy && above.isAir()) {
                pos.setY(y+1);
                return pos;
            }
        }
        return start; // 没有找到安全位置
    }
}
