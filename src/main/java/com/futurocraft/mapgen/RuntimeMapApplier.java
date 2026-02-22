package com.futurocraft.mapgen;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;

public class RuntimeMapApplier {

    private final MapSettings settings;

    public RuntimeMapApplier(MapSettings settings) {
        this.settings = settings;
    }

    public void applyChunk(World world, Chunk chunk) {
        int minX = -settings.mapSize() / 2;
        int maxX = settings.mapSize() / 2;
        int minZ = -settings.mapSize() / 2;
        int maxZ = settings.mapSize() / 2;

        int minY = world.getMinHeight();
        int maxY = Math.min(world.getMaxHeight() - 1, settings.waterLevel() + settings.glassWallHeight() + 6);

        Random random = new Random((((long) chunk.getX()) << 32) ^ chunk.getZ() ^ world.getSeed());

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int worldX = (chunk.getX() << 4) + lx;
                int worldZ = (chunk.getZ() << 4) + lz;

                if (worldX < minX || worldX > maxX || worldZ < minZ || worldZ > maxZ) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    world.getBlockAt(worldX, y, worldZ).setType(Material.AIR, false);
                }

                int edgeDistance = Math.min(
                        Math.min(worldX - minX, maxX - worldX),
                        Math.min(worldZ - minZ, maxZ - worldZ)
                );

                int groundY = computeGroundY(edgeDistance);
                world.getBlockAt(worldX, settings.bedrockY(), worldZ).setType(Material.BEDROCK, false);

                for (int y = settings.bedrockY() + 1; y <= groundY; y++) {
                    world.getBlockAt(worldX, y, worldZ).setType(layerMaterial(y, groundY, edgeDistance, random), false);
                }

                if (groundY < settings.waterLevel()) {
                    for (int y = groundY + 1; y <= settings.waterLevel(); y++) {
                        world.getBlockAt(worldX, y, worldZ).setType(Material.WATER, false);
                    }
                }

                if (worldX == minX || worldX == maxX || worldZ == minZ || worldZ == maxZ) {
                    int wallTop = settings.waterLevel() + settings.glassWallHeight();
                    for (int y = settings.waterLevel() + 1; y <= wallTop; y++) {
                        world.getBlockAt(worldX, y, worldZ).setType(Material.GLASS, false);
                    }
                }
            }
        }

        new SurfaceFeaturePopulator(settings).populate(world, random, chunk);
    }

    private int computeGroundY(int edgeDistance) {
        int surfaceY = settings.surfaceY();
        if (edgeDistance < settings.seaWidth()) return settings.waterLevel() - 4;
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
        if (y == groundY) return beachZone ? Material.SAND : Material.GRASS_BLOCK;
        int dirtStart = groundY - settings.dirtLayers();
        if (y > dirtStart) return beachZone ? Material.SAND : Material.DIRT;
        if (random.nextDouble() < oreChance(y)) return randomOre(random);
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
