package com.futurocraft.mapgen;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    private final BlockLifecyclePlugin plugin;
    private final StorageManager storage;

    public BlockBreakListener(BlockLifecyclePlugin plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String world = block.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        long nowMs = System.currentTimeMillis();
        long nowTick = storage.getWorldTick(block.getWorld());

        storage.upsertChunkActivity(world, block.getChunk().getX(), block.getChunk().getZ(), nowMs, nowTick);

        if (storage.isPlayerPlacedBlock(world, x, y, z)) {
            storage.deletePlayerBlock(world, x, y, z);
            return;
        }

        Material original = block.getType();
        if (original != Material.AIR && original.isBlock()) {
            storage.registerNaturalBreak(world, x, y, z, original, nowMs, nowTick);
        }
    }
}
