package com.chaosthedude.explorerscompass.cache;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 缓存所有玩家探索过的结构数据的类，使用单一公共NBT文件存储。
 */
public class SharedStructureCache {

    public static final String FILE_NAME = "shared_structures_cache.nbt";
    private static final Path CACHE_FILE = getSharedCacheFile();

    // 内存缓存
    private static Map<StructureLocation, Set<UUID>> memoryCache = null;
    private static boolean cacheDirty = false;

    /**
     * 获取共享缓存文件路径
     */
    private static Path getSharedCacheFile() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path cacheDir = worldDir.resolve(PlayerStructureCache.FOLDER);
        try {
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }
            return cacheDir.resolve(FILE_NAME);
        } catch (IOException e) {
            ExplorersCompass.LOGGER.error("Failed to create shared cache directory", e);
            return null;
        }
    }

    /**
     * 初始化内存缓存（如果尚未初始化）
     */
    private static void initializeCache() {
        if (memoryCache == null) {
            memoryCache = new HashMap<>();
            CompoundTag tag = readSharedCache();
            ListTag list = tag.getList("explored", Tag.TAG_COMPOUND);

            for (Tag t : list) {
                CompoundTag ct = (CompoundTag) t;
                StructureLocation loc = StructureLocation.fromNBT(ct);

                Set<UUID> uuids = new HashSet<>();
                ListTag uuidList = ct.getList("uuids", Tag.TAG_STRING);
                for (Tag uuidTag : uuidList) {
                    uuids.add(UUID.fromString(uuidTag.getAsString()));
                }

                memoryCache.put(loc, uuids);
            }
            cacheDirty = false;
        }
    }

    /**
     * 将内存缓存写入磁盘（如果缓存有变化）
     */
    private static void flushCache() {
        if (cacheDirty && memoryCache != null) {
            saveSharedCache(memoryCache);
            cacheDirty = false;
        }
    }

    /**
     * 读取共享缓存NBT
     */
    private static CompoundTag readSharedCache() {
        if (CACHE_FILE == null || !Files.exists(CACHE_FILE)) {
            return new CompoundTag();
        }

        try (InputStream in = Files.newInputStream(CACHE_FILE)) {
            return NbtIo.readCompressed(in);
        } catch (Exception e) {
            ExplorersCompass.LOGGER.error("Failed to read shared structure cache", e);
            return new CompoundTag();
        }
    }

    /**
     * 写入共享缓存NBT
     */
    private static void writeSharedCache(CompoundTag tag) {
        if (CACHE_FILE == null) return;

        try (OutputStream out = Files.newOutputStream(CACHE_FILE)) {
            NbtIo.writeCompressed(tag, out);
        } catch (Exception e) {
            ExplorersCompass.LOGGER.error("Failed to write shared structure cache", e);
        }
    }

    /**
     * 获取所有已探索结构位置及其探索者
     */
    public static Map<StructureLocation, Set<UUID>> getAllExploredStructures() {
        initializeCache();
        return new HashMap<>(memoryCache);
    }

    /**
     * 添加结构到共享缓存
     */
    public static void addToSharedCache(ServerPlayer player, StructureLocation loc) {
        initializeCache();

        // 如果结构已存在，只添加玩家UUID
        if (memoryCache.containsKey(loc)) {
            memoryCache.get(loc).add(player.getUUID());
        } else {
            // 否则创建新条目
            Set<UUID> uuids = new HashSet<>();
            uuids.add(player.getUUID());
            memoryCache.put(loc, uuids);
        }

        cacheDirty = true;
    }

    /**
     * 从共享缓存中移除玩家对结构的探索记录
     */
    public static void removeFromSharedCache(UUID playerUuid, StructureLocation loc) {
        initializeCache();

        if (memoryCache.containsKey(loc)) {
            memoryCache.get(loc).remove(playerUuid);

            // 如果没有玩家探索过这个结构，移除整个条目
            if (memoryCache.get(loc).isEmpty()) {
                memoryCache.remove(loc);
            }

            cacheDirty = true;
        }
    }

    /**
     * 保存共享缓存
     */
    private static void saveSharedCache(Map<StructureLocation, Set<UUID>> cache) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();

        for (Map.Entry<StructureLocation, Set<UUID>> entry : cache.entrySet()) {
            CompoundTag entryTag = entry.getKey().toNBT();

            ListTag uuidList = new ListTag();
            for (UUID uuid : entry.getValue()) {
                uuidList.add(StringTag.valueOf(uuid.toString()));
            }

            entryTag.put("uuids", uuidList);
            list.add(entryTag);
        }

        tag.put("explored", list);
        writeSharedCache(tag);
    }

    /**
     * 检查玩家是否已探索过某个结构
     */
    public static boolean hasExplored(UUID playerUuid, StructureLocation loc) {
        initializeCache();
        return memoryCache.containsKey(loc) && memoryCache.get(loc).contains(playerUuid);
    }

    /**
     * 获取探索过特定结构的所有玩家
     */
    public static Set<UUID> getExplorers(StructureLocation loc) {
        initializeCache();
        return memoryCache.getOrDefault(loc, Collections.emptySet());
    }

    /**
     * 清除所有共享缓存
     */
    public static void clearSharedCache() {
        memoryCache = new HashMap<>();
        cacheDirty = true;

        if (CACHE_FILE != null && Files.exists(CACHE_FILE)) {
            try {
                Files.delete(CACHE_FILE);
            } catch (IOException e) {
                ExplorersCompass.LOGGER.error("Failed to clear shared structure cache", e);
            }
        }
    }

    /**
     * 强制将缓存写入磁盘
     */
    public static void saveToDisk() {
        if (memoryCache != null) {
            flushCache();
        }
    }
}
