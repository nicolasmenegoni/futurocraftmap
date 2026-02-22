package com.futurocraft.mapgen;

import org.bukkit.configuration.file.FileConfiguration;

public record MapSettings(
        String worldName,
        int mapSize,
        int waterLevel,
        int borderSize,
        int seaWidth,
        int glassWallHeight,
        int bedrockY,
        int stoneLayers,
        int dirtLayers,
        int surfaceY,
        double treeChance,
        double lakeChance,
        double pastureChance,
        double houseChance
) {

    public static MapSettings fromConfig(FileConfiguration config) {
        String worldName = config.getString("world.name", "world");
        int mapSize = Math.max(128, config.getInt("map.size", 1000));
        int waterLevel = Math.max(6, config.getInt("map.water-level", 58));
        int borderSize = Math.max(24, config.getInt("map.border-size", 140));
        int seaWidth = Math.max(8, config.getInt("map.sea-width", 80));
        if (seaWidth >= borderSize) {
            seaWidth = borderSize - 8;
        }
        int glassWallHeight = Math.max(6, config.getInt("map.glass-wall-height", 24));

        int requestedBedrockY = Math.max(0, config.getInt("layers.bedrock-y", 48));
        int stoneLayers = Math.max(1, config.getInt("layers.stone", 10));
        int dirtLayers = Math.max(1, config.getInt("layers.dirt", 4));

        int minBedrockForDrySurface = Math.max(0, (waterLevel + 2) - (stoneLayers + dirtLayers));
        int bedrockY = Math.max(requestedBedrockY, minBedrockForDrySurface);

        int surfaceY = bedrockY + stoneLayers + dirtLayers;

        double treeChance = clamp(config.getDouble("features.tree-chance-per-chunk", 0.95), 0.0, 1.0);
        double lakeChance = clamp(config.getDouble("features.lake-chance-per-chunk", 0.25), 0.0, 1.0);
        double pastureChance = clamp(config.getDouble("features.pasture-chance-per-chunk", 0.25), 0.0, 1.0);
        double houseChance = clamp(config.getDouble("features.house-chance-per-chunk", 0.07), 0.0, 1.0);

        return new MapSettings(worldName, mapSize, waterLevel, borderSize, seaWidth, glassWallHeight, bedrockY,
                stoneLayers, dirtLayers, surfaceY, treeChance, lakeChance, pastureChance, houseChance);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
