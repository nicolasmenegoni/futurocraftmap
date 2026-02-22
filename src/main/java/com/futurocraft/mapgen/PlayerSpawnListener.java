package com.futurocraft.mapgen;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerSpawnListener implements Listener {

    private final FuturoCraftMapPlugin plugin;

    public PlayerSpawnListener(FuturoCraftMapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            World target = plugin.getTargetWorld();
            if (target == null) {
                return;
            }

            player.teleport(plugin.getSafeSpawnLocation(target));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        World target = plugin.getTargetWorld();
        if (target != null) {
            event.setRespawnLocation(plugin.getSafeSpawnLocation(target));
        }
    }
}
