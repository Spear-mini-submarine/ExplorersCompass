package com.chaosthedude.explorerscompass.worker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;

@FunctionalInterface
public interface StructureFoundCallback {
    /**
     * @param foundPos 结构坐标
     * @param foundStructure 结构实例
     * @return true 继续正常流程，false 阻止默认 succeed 行为
     */
    boolean onStructureFound(BlockPos foundPos, Structure foundStructure);
}
