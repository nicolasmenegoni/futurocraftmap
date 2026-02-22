package com.futurocraft.mapgen;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class FuturoCraftMapPlugin extends JavaPlugin {

    private MapSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = MapSettings.fromConfig(getConfig());

        getServer().getPluginManager().registerEvents(new PlayerSpawnListener(this, settings), this);

        Bukkit.getScheduler().runTask(this, () -> {
            World world = getTargetWorld();
            if (world != null) {
                world.setSpawnLocation(new Location(world, 0.5, settings.surfaceY() + 1.0, 0.5));
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
}
