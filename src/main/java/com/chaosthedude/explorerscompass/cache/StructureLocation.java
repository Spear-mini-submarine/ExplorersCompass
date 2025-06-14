package com.chaosthedude.explorerscompass.cache;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 结构位置类，用于表示和存储结构的位置信息。
 */
public class StructureLocation {
    public final ResourceLocation structureKey;
    public final String dimension;
    public final int x, z;

    public StructureLocation(ResourceLocation key, String dim, int x, int z) {
        this.structureKey = key;
        this.dimension = dim;
        this.x = x;
        this.z = z;
    }

    public static StructureLocation fromNBT(CompoundTag tag) {
        return new StructureLocation(
                new ResourceLocation(tag.getString("structure")),
                tag.getString("dimension"),
                tag.getInt("x"),
                tag.getInt("z")
        );
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("structure", structureKey.toString());
        tag.putString("dimension", dimension);
        tag.putInt("x", x);
        tag.putInt("z", z);
        return tag;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StructureLocation loc)) return false;
        return structureKey.equals(loc.structureKey)
                && dimension.equals(loc.dimension)
                && x == loc.x && z == loc.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(structureKey, dimension, x, z);
    }
}
