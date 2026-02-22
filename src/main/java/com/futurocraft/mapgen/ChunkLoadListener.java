package com.futurocraft.mapgen;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkLoadListener implements Listener {
    private final BlockLifecyclePlugin plugin;
    private final StorageManager storage;

    public ChunkLoadListener(BlockLifecyclePlugin plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        StorageManager.ChunkActivity activity = storage.getChunkActivity(worldName, chunkX, chunkZ);
        long nowMs = System.currentTimeMillis();
        long nowTick = storage.getWorldTick(chunk.getWorld());

        if (activity != null && hasExpired(activity.lastSeenMs(), activity.lastSeenTick(), nowMs, nowTick, plugin.getRegenThreshold())) {
            for (StorageManager.NaturalBreakRecord record : storage.getNaturalBreaksInChunk(worldName, chunkX, chunkZ)) {
                Block target = chunk.getWorld().getBlockAt(record.x(), record.y(), record.z());
                if (target.getType() == Material.AIR) {
                    target.setType(record.originalType(), false);
                }
                storage.deleteNaturalBreak(worldName, record.x(), record.y(), record.z());
            }
        }

        for (StorageManager.PlayerBlockRecord record : storage.getPlayerBlocksInChunk(worldName, chunkX, chunkZ)) {
            if (!hasExpired(record.lastOwnerVisitMs(), record.lastOwnerVisitTick(), nowMs, nowTick, plugin.getRemovalThreshold())) {
                continue;
            }
            Block target = chunk.getWorld().getBlockAt(record.x(), record.y(), record.z());
            if (target.getType() == record.blockType()) {
                target.setType(Material.AIR, false);
            }
            storage.deletePlayerBlock(worldName, record.x(), record.y(), record.z());
        }
    }

    private boolean hasExpired(long savedMs, long savedTick, long nowMs, long nowTick, long threshold) {
        if (plugin.useRealTime()) {
            long unitMs = plugin.useRealMinutes() ? 60L * 1000L : 60L * 60L * 1000L;
            return (nowMs - savedMs) >= threshold * unitMs;
        }
        return (nowTick - savedTick) >= threshold;
    }
}
