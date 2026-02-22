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
                    Material material = layerMaterial(y, groundY, edgeDistance, random);
                    chunkData.setBlock(x, y, z, material);
                }

                if (groundY < settings.waterLevel()) {
                    for (int y = groundY + 1; y <= settings.waterLevel(); y++) {
                        chunkData.setBlock(x, y, z, Material.WATER);
                    }
                }

                if (isWallEdge(worldX, worldZ, minX, maxX, minZ, maxZ)) {
                    int wallTop = settings.waterLevel() + settings.glassWallHeight();
                    for (int y = settings.bedrockY(); y <= wallTop; y++) {
                        chunkData.setBlock(x, y, z, Material.GLASS);
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

        if (edgeDistance < settings.seaWidth()) {
            return settings.waterLevel() - 4;
        }

        if (edgeDistance < settings.borderSize()) {
            int beachWidth = settings.borderSize() - settings.seaWidth();
            double ratio = (double) (edgeDistance - settings.seaWidth()) / (double) beachWidth;
            int beachStart = settings.waterLevel() - 1;
            return (int) Math.round(beachStart + (surfaceY - beachStart) * ratio);
        }

        return surfaceY;
    }

    private Material layerMaterial(int y, int groundY, int edgeDistance, Random random) {
        boolean beachZone = edgeDistance < settings.borderSize();

        if (y == groundY) {
            return beachZone ? Material.SAND : Material.GRASS_BLOCK;
        }

        int dirtStart = groundY - settings.dirtLayers();
        if (y > dirtStart) {
            return beachZone ? Material.SAND : Material.DIRT;
        }

        if (random.nextDouble() < oreChance(y)) {
            return randomOre(random);
        }
        return Material.STONE;
    }

    private boolean isWallEdge(int worldX, int worldZ, int minX, int maxX, int minZ, int maxZ) {
        return worldX == minX || worldX == maxX || worldZ == minZ || worldZ == maxZ;
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
