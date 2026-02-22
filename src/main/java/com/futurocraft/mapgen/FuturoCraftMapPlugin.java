package com.futurocraft.mapgen;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class FuturoCraftMapPlugin extends JavaPlugin {

    private MapSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = MapSettings.fromConfig(getConfig());

        if (settings.autoCreateWorld()) {
            Bukkit.getScheduler().runTask(this, this::createOrLoadWorld);
        }

        getServer().getPluginManager().registerEvents(new PlayerSpawnListener(this, settings), this);

        getLogger().info("FuturoCraftMapGen habilitado.");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (settings == null) {
            saveDefaultConfig();
            settings = MapSettings.fromConfig(getConfig());
        }
        return new FuturoFlatGenerator(this, settings);
    }

    public World createOrLoadWorld() {
        String worldName = settings.worldName();
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            existing.setSpawnLocation(new Location(existing, 0.5, settings.surfaceY() + 1.0, 0.5));
            return existing;
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new FuturoFlatGenerator(this, settings));
        World world = creator.createWorld();
        if (world != null) {
            world.setSpawnLocation(new Location(world, 0.5, settings.surfaceY() + 1.0, 0.5));
        }
        return world;
    }
}
