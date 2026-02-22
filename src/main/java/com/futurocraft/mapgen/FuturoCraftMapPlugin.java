package com.futurocraft.mapgen;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class FuturoCraftMapPlugin extends JavaPlugin {

    private MapSettings settings;
    private RuntimeMapApplier mapApplier;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = MapSettings.fromConfig(getConfig());
        mapApplier = new RuntimeMapApplier(settings);

        getServer().getPluginManager().registerEvents(new PlayerSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkRegeneratorListener(this), this);

        Bukkit.getScheduler().runTask(this, () -> {
            World world = getTargetWorld();
            if (world != null) {
                preloadSpawnArea(world);
                world.setSpawnLocation(getSafeSpawnLocation(world));
            } else {
                getLogger().warning("Mundo alvo não encontrado: " + settings.worldName());
            }
        });

        getLogger().info("FuturoCraftMapGen habilitado no mundo existente.");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (settings == null) {
            saveDefaultConfig();
            settings = MapSettings.fromConfig(getConfig());
        }
        return new FuturoFlatGenerator(this, settings);
    }

    public World getTargetWorld() {
        World byName = Bukkit.getWorld(settings.worldName());
        if (byName != null) {
            return byName;
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    public RuntimeMapApplier getMapApplier() {
        return mapApplier;
    }

    public Location getSafeSpawnLocation(World world) {
        int x = 0;
        int z = 0;

        world.getChunkAt(x >> 4, z >> 4).load();
        int y = Math.max(world.getMinHeight() + 1, world.getHighestBlockYAt(x, z));

        Block ground = world.getBlockAt(x, y - 1, z);
        if (ground.getType() == Material.AIR || ground.isLiquid()) {
            y = settings.surfaceY() + 1;
            ensureSpawnPlatform(world, x, y - 1, z);
        }

        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private void preloadSpawnArea(World world) {
        int radius = settings.preloadRadiusChunks();
        for (int cx = -radius; cx <= radius; cx++) {
            for (int cz = -radius; cz <= radius; cz++) {
                mapApplier.applyChunk(world, world.getChunkAt(cx, cz));
            }
        }
    }

    private void ensureSpawnPlatform(World world, int centerX, int groundY, int centerZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(centerX + dx, groundY - 1, centerZ + dz).setType(Material.DIRT, false);
                world.getBlockAt(centerX + dx, groundY, centerZ + dz).setType(Material.GRASS_BLOCK, false);
                world.getBlockAt(centerX + dx, groundY + 1, centerZ + dz).setType(Material.AIR, false);
                world.getBlockAt(centerX + dx, groundY + 2, centerZ + dz).setType(Material.AIR, false);
            }
        }
    }
}
