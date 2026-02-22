package com.futurocraft.mapgen;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkRegeneratorListener implements Listener {

    private final FuturoCraftMapPlugin plugin;

    public ChunkRegeneratorListener(FuturoCraftMapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        World target = plugin.getTargetWorld();
        if (target == null || !event.getWorld().getUID().equals(target.getUID())) {
            return;
        }
        plugin.getMapApplier().applyChunk(event.getWorld(), event.getChunk());
    }
}
