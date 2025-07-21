package com.chaosthedude.explorerscompass.cache;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存玩家搜索过的结构数据的类。
 */
public class PlayerStructureCache {

    public static final String FOLDER = "explorerscompass_player_cache";
    private static final Map<UUID, String> uuidToName = new ConcurrentHashMap<>();
    /**
     * 获取玩家名称，优先从缓存中获取，如果不存在则返回"Unknown"。
     */
    public static String getName(UUID uuid) {
        return uuidToName.getOrDefault(uuid, "Unknown");
    }

    public static Map<UUID, String> getUuidToName() {
        return uuidToName;
    }

    /**
     * 初始化玩家缓存，加载已存在的玩家数据。
     */
    public static void init(MinecraftServer server){
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path cacheDir = worldDir.resolve(FOLDER);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDir, "*.nbt")) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                if (fileName.endsWith("shared_structures_cache.nbt")) continue;
                String uuidString = fileName.substring(0, fileName.length() - 4);
                UUID uuid = UUID.fromString(uuidString);

                // 读取名称
                CompoundTag tag = readCacheTag(server.getLevel(Level.OVERWORLD), uuid);
                if (tag.contains("name", Tag.TAG_STRING)) {
                    uuidToName.put(uuid, tag.getString("name"));
                }
            }
        } catch (IOException e) {
            ExplorersCompass.LOGGER.error(e);
        }
    }
    /**
     * 获取玩家缓存文件路径
     */
    public static Path getCacheFileForPlayer(ServerLevel level, UUID uuid) throws IOException {
        Path worldDir = level.getServer().getWorldPath(LevelResource.ROOT);
        Path cacheDir = worldDir.resolve(FOLDER);
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }
        return cacheDir.resolve(uuid.toString() + ".nbt");
    }

    /**
     * 读取玩家缓存NBT
     */
    public static CompoundTag readCacheTag(ServerLevel level, UUID uuid) {
        try {
            Path file = getCacheFileForPlayer(level, uuid);
            if (!Files.exists(file)) return new CompoundTag();
            try (InputStream in = Files.newInputStream(file)) {
                CompoundTag tag = NbtIo.readCompressed(in);
                if (tag.contains("name", Tag.TAG_STRING)) {
                    uuidToName.put(uuid, tag.getString("name"));
                }
                return tag;
            }
        } catch (Exception e) {
            ExplorersCompass.LOGGER.error(e);
            return new CompoundTag();
        }
    }

    /**
     * 保存玩家缓存NBT
     */
    public static void writeCacheTag(ServerLevel level, UUID uuid, CompoundTag tag) {
        try {
            Path file = getCacheFileForPlayer(level, uuid);
            try (OutputStream out = Files.newOutputStream(file)) {
                NbtIo.writeCompressed(tag, out);
            }
        } catch (Exception e) {
            ExplorersCompass.LOGGER.error(e);
        }
    }

    /**
     * 获取玩家结构缓存
     */
    public static Set<StructureLocation> getCache(ServerLevel serverLevel,UUID uuid) {
        Set<StructureLocation> cache = new HashSet<>();
        CompoundTag tag = readCacheTag(serverLevel, uuid);
        ListTag list = tag.getList("explored", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag ct = (CompoundTag) t;
            cache.add(StructureLocation.fromNBT(ct));
        }
        return cache;
    }

    /**
     * 添加结构到缓存
     */
    public static void addToCache(ServerPlayer player, StructureLocation loc) {
        Set<StructureLocation> cache = getCache(player.serverLevel(), player.getUUID());
        cache.add(loc);
        saveCache(player, cache);
    }

    /**
     * 批量添加结构到缓存
     */
    public static void addAllToCache(ServerPlayer player, Iterable<StructureLocation> locs) {
        Set<StructureLocation> cache = getCache(player.serverLevel(), player.getUUID());
        for (StructureLocation loc : locs) {
            cache.add(loc);
        }
        saveCache(player, cache);
    }

    /**
     * 判断是否已探索
     */
    public static boolean hasExplored(ServerPlayer player, StructureLocation loc) {
        return getCache(player.serverLevel(),player.getUUID()).contains(loc);
    }

    /**
     * 保存缓存
     */
    public static void saveCache(ServerPlayer player, Set<StructureLocation> cache) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (StructureLocation loc : cache) {
            list.add(loc.toNBT());
        }
        tag.put("explored", list);
        tag.putString("name", player.getName().getString()); // 新增保存玩家名称
        writeCacheTag(player.serverLevel(), player.getUUID(), tag);

        // 同步到内存缓存
        uuidToName.put(player.getUUID(), player.getName().getString());
    }

    /**
     * 清除缓存
     */
    public static void clearCache(ServerPlayer player) {
        try {
            Path file = getCacheFileForPlayer(player.serverLevel(), player.getUUID());
            Files.deleteIfExists(file);
        } catch (Exception e) {
            ExplorersCompass.LOGGER.error(e);
        }
    }
}
