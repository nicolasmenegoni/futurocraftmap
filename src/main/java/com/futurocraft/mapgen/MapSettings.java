package com.futurocraft.mapgen;

import org.bukkit.configuration.file.FileConfiguration;

public record MapSettings(
        String worldName,
        boolean autoCreateWorld,
        int mapSize,
        int waterLevel,
        int borderSize,
        int bedrockY,
        int stoneLayers,
        int dirtLayers,
        int surfaceY,
        double treeChance,
        double lakeChance,
        double pastureChance
) {

    public static MapSettings fromConfig(FileConfiguration config) {
        String worldName = config.getString("world.name", "futuro_map");
        boolean autoCreate = config.getBoolean("world.auto-create", true);
        int mapSize = Math.max(128, config.getInt("map.size", 1000));
        int waterLevel = Math.max(6, config.getInt("map.water-level", 58));
        int borderSize = Math.max(16, config.getInt("map.border-size", 80));

        int bedrockY = Math.max(0, config.getInt("layers.bedrock-y", 0));
        int stoneLayers = Math.max(1, config.getInt("layers.stone", 10));
        int dirtLayers = Math.max(1, config.getInt("layers.dirt", 4));

        int surfaceY = bedrockY + stoneLayers + dirtLayers;

        double treeChance = clamp(config.getDouble("features.tree-chance-per-chunk", 0.65), 0.0, 1.0);
        double lakeChance = clamp(config.getDouble("features.lake-chance-per-chunk", 0.08), 0.0, 1.0);
        double pastureChance = clamp(config.getDouble("features.pasture-chance-per-chunk", 0.20), 0.0, 1.0);

        return new MapSettings(worldName, autoCreate, mapSize, waterLevel, borderSize, bedrockY, stoneLayers, dirtLayers,
                surfaceY, treeChance, lakeChance, pastureChance);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
