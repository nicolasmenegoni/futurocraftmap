package com.futurocraft.mapgen;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockLifecyclePlugin extends JavaPlugin {
    private StorageManager storageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.storageManager = new StorageManager(this);
        this.storageManager.initialize();

        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(this, storageManager), this);
        Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(this, storageManager), this);
        Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(this, storageManager), this);
        Bukkit.getPluginManager().registerEvents(new ChunkActivityListener(storageManager), this);

        getLogger().info("Block lifecycle plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.close();
        }
    }

    public long getRegenThreshold() {
        return getConfig().getLong("regeneration.threshold", 48L);
    }

    public long getRemovalThreshold() {
        return getConfig().getLong("removal.threshold", 48L);
    }

    public boolean useRealTime() {
        String mode = getConfig().getString("time.mode", "REAL_HOURS");
        return !"TICKS".equalsIgnoreCase(mode);
    }

    public boolean useRealMinutes() {
        String mode = getConfig().getString("time.mode", "REAL_HOURS");
        return "REAL_MINUTES".equalsIgnoreCase(mode);
    }
}
