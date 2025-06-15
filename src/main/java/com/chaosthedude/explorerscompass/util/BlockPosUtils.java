package com.chaosthedude.explorerscompass.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockPosUtils {
    public static BlockPos findSafeLanding(ServerLevel world, BlockPos start) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(start.getX(), 255, start.getZ());
        for (int y = 255; y > world.getMinBuildHeight(); y--) {
            pos.setY(y);

            BlockState below = world.getBlockState(pos.below());
            VoxelShape shape = below.getCollisionShape(world, pos.below());
            boolean hasFloor = !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 1.0 / 16.0;

            BlockState at = world.getBlockState(pos);
            BlockState above = world.getBlockState(pos.above());
            boolean spaceClear = at.getCollisionShape(world, pos).isEmpty() && above.getCollisionShape(world, pos.above()).isEmpty();
            if (hasFloor && spaceClear) {
                return pos.immutable();
            }
        }
        return pos.set(128); // 没有找到安全位置
    }
}
