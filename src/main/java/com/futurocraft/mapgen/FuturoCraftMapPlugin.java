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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = MapSettings.fromConfig(getConfig());

        getServer().getPluginManager().registerEvents(new PlayerSpawnListener(this), this);

        Bukkit.getScheduler().runTask(this, () -> {
            World world = getTargetWorld();
            if (world != null) {
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

    public Location getSafeSpawnLocation(World world) {
        int x = 0;
        int z = 0;

        world.getChunkAt(x >> 4, z >> 4).load();
        int y = Math.max(world.getMinHeight() + 1, world.getHighestBlockYAt(x, z));

        Block ground = world.getBlockAt(x, y - 1, z);
        if (ground.getType() == Material.AIR || ground.isLiquid()) {
            y = settings.surfaceY() + 1;
        }

        return new Location(world, x + 0.5, y, z + 0.5);
    }
}
