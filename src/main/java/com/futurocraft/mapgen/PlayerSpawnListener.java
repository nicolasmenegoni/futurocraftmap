package com.futurocraft.mapgen;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerSpawnListener implements Listener {

    private final FuturoCraftMapPlugin plugin;
    private final MapSettings settings;

    public PlayerSpawnListener(FuturoCraftMapPlugin plugin, MapSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            World target = Bukkit.getWorld(settings.worldName());
            if (target == null) {
                target = plugin.createOrLoadWorld();
            }
            if (target == null) {
                return;
            }

            if (!player.getWorld().getName().equalsIgnoreCase(settings.worldName())) {
                player.teleport(spawnLocation(target));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        World target = Bukkit.getWorld(settings.worldName());
        if (target == null) {
            target = plugin.createOrLoadWorld();
        }
        if (target != null) {
            event.setRespawnLocation(spawnLocation(target));
        }
    }

    private Location spawnLocation(World world) {
        return new Location(world, 0.5, settings.surfaceY() + 1.0, 0.5);
    }
}
