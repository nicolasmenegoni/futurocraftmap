package com.futurocraft.mapgen;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {
    private final BlockLifecyclePlugin plugin;
    private final StorageManager storage;

    public BlockPlaceListener(BlockLifecyclePlugin plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        String world = block.getWorld().getName();
        long nowMs = System.currentTimeMillis();
        long nowTick = storage.getWorldTick(block.getWorld());

        storage.registerPlacement(
                world,
                block.getX(),
                block.getY(),
                block.getZ(),
                block.getType(),
                event.getPlayer().getUniqueId(),
                nowMs,
                nowTick,
                block.getChunk().getX(),
                block.getChunk().getZ()
        );

        storage.deleteNaturalBreak(world, block.getX(), block.getY(), block.getZ());
        storage.upsertChunkActivity(world, block.getChunk().getX(), block.getChunk().getZ(), nowMs, nowTick);
    }
}
