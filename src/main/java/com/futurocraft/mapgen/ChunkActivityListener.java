package com.futurocraft.mapgen;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ChunkActivityListener implements Listener {
    private final StorageManager storage;

    public ChunkActivityListener(StorageManager storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        touch(event.getPlayer(), event.getPlayer().getLocation(), null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        touch(event.getPlayer(), event.getTo(), event.getFrom());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        touch(event.getPlayer(), event.getTo(), event.getFrom());
    }

    private void touch(Player player, Location to, Location from) {
        if (to == null) {
            return;
        }
        if (from != null && from.getBlockX() >> 4 == to.getBlockX() >> 4 && from.getBlockZ() >> 4 == to.getBlockZ() >> 4
                && from.getWorld() != null && to.getWorld() != null && from.getWorld().equals(to.getWorld())) {
            return;
        }

        String world = to.getWorld().getName();
        int chunkX = to.getBlockX() >> 4;
        int chunkZ = to.getBlockZ() >> 4;
        long nowMs = System.currentTimeMillis();
        long nowTick = storage.getWorldTick(to.getWorld());

        storage.upsertChunkActivity(world, chunkX, chunkZ, nowMs, nowTick);
        storage.touchOwnerVisitsForChunk(world, chunkX, chunkZ, player.getUniqueId(), nowMs, nowTick);
    }
}
