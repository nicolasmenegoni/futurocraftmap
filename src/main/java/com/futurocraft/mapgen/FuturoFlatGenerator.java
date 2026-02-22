package com.futurocraft.mapgen;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FuturoFlatGenerator extends ChunkGenerator {

    private final MapSettings settings;
    private final List<BlockPopulator> populators;

    public FuturoFlatGenerator(FuturoCraftMapPlugin plugin, MapSettings settings) {
        this.settings = settings;
        this.populators = Collections.singletonList(new SurfaceFeaturePopulator(settings));
    }

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        int minX = -settings.mapSize() / 2;
        int maxX = settings.mapSize() / 2;
        int minZ = -settings.mapSize() / 2;
        int maxZ = settings.mapSize() / 2;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (chunkX << 4) + x;
                int worldZ = (chunkZ << 4) + z;

                boolean inside = worldX >= minX && worldX <= maxX && worldZ >= minZ && worldZ <= maxZ;
                if (!inside) {
                    continue;
                }

                int edgeDistance = Math.min(
                        Math.min(worldX - minX, maxX - worldX),
                        Math.min(worldZ - minZ, maxZ - worldZ)
                );

                int groundY = computeGroundY(edgeDistance);

                chunkData.setBlock(x, settings.bedrockY(), z, Material.BEDROCK);

                for (int y = settings.bedrockY() + 1; y <= groundY; y++) {
                    Material material = layerMaterial(y, groundY, random);
                    chunkData.setBlock(x, y, z, material);
                }

                if (groundY < settings.waterLevel()) {
                    for (int y = groundY + 1; y <= settings.waterLevel(); y++) {
                        chunkData.setBlock(x, y, z, Material.WATER);
                    }
                }
            }
        }
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return populators;
    }

    private int computeGroundY(int edgeDistance) {
        int surfaceY = settings.surfaceY();
        if (edgeDistance >= settings.borderSize()) {
            return surfaceY;
        }

        double ratio = (double) edgeDistance / (double) settings.borderSize();
        int targetBeachHeight = settings.waterLevel() - 2;
        return (int) Math.round(targetBeachHeight + (surfaceY - targetBeachHeight) * ratio);
    }

    private Material layerMaterial(int y, int groundY, Random random) {
        if (y == groundY) {
            return groundY >= settings.waterLevel() ? Material.GRASS_BLOCK : Material.SAND;
        }

        int dirtStart = groundY - settings.dirtLayers();
        if (y > dirtStart) {
            return groundY >= settings.waterLevel() ? Material.DIRT : Material.SAND;
        }

        if (random.nextDouble() < oreChance(y)) {
            return randomOre(random);
        }
        return Material.STONE;
    }

    private double oreChance(int y) {
        if (y < 16) return 0.05;
        if (y < 32) return 0.035;
        if (y < 48) return 0.02;
        return 0.01;
    }

    private Material randomOre(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.35) return Material.COAL_ORE;
        if (roll < 0.55) return Material.IRON_ORE;
        if (roll < 0.72) return Material.COPPER_ORE;
        if (roll < 0.84) return Material.REDSTONE_ORE;
        if (roll < 0.93) return Material.LAPIS_ORE;
        if (roll < 0.98) return Material.GOLD_ORE;
        return Material.DIAMOND_ORE;
    }
}
