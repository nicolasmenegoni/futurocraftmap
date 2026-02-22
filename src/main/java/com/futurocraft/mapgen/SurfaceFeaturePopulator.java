package com.futurocraft.mapgen;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

import java.util.Random;

public class SurfaceFeaturePopulator extends BlockPopulator {

    private static final TreeType[] TREE_TYPES = new TreeType[]{
            TreeType.TREE,
            TreeType.BIG_TREE,
            TreeType.BIRCH,
            TreeType.REDWOOD,
            TreeType.TALL_REDWOOD,
            TreeType.JUNGLE,
            TreeType.SMALL_JUNGLE,
            TreeType.JUNGLE_BUSH,
            TreeType.ACACIA,
            TreeType.DARK_OAK,
            TreeType.MANGROVE,
            TreeType.CHERRY
    };

    private final MapSettings settings;

    public SurfaceFeaturePopulator(MapSettings settings) {
        this.settings = settings;
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        if (!isInsideMap(chunk)) {
            return;
        }

        if (random.nextDouble() < settings.lakeChance()) {
            generateLake(world, random, chunk);
        }

        if (random.nextDouble() < settings.pastureChance()) {
            generatePasture(world, random, chunk);
        }

        if (random.nextDouble() < settings.treeChance()) {
            int attempts = 2 + random.nextInt(4);
            for (int i = 0; i < attempts; i++) {
                generateTree(world, random, chunk);
            }
        }
    }

    private boolean isInsideMap(Chunk chunk) {
        int chunkCenterX = (chunk.getX() << 4) + 8;
        int chunkCenterZ = (chunk.getZ() << 4) + 8;
        int half = settings.mapSize() / 2;
        return chunkCenterX >= -half && chunkCenterX <= half && chunkCenterZ >= -half && chunkCenterZ <= half;
    }

    private void generateTree(World world, Random random, Chunk chunk) {
        int localX = random.nextInt(16);
        int localZ = random.nextInt(16);
        int worldX = (chunk.getX() << 4) + localX;
        int worldZ = (chunk.getZ() << 4) + localZ;
        int y = world.getHighestBlockYAt(worldX, worldZ);

        Block ground = world.getBlockAt(worldX, y - 1, worldZ);
        Block base = world.getBlockAt(worldX, y, worldZ);

        if (ground.getType() != Material.GRASS_BLOCK || !base.isEmpty()) {
            return;
        }

        TreeType type = TREE_TYPES[random.nextInt(TREE_TYPES.length)];
        world.generateTree(new Location(world, worldX, y, worldZ), type);
    }

    private void generateLake(World world, Random random, Chunk chunk) {
        int centerX = (chunk.getX() << 4) + random.nextInt(16);
        int centerZ = (chunk.getZ() << 4) + random.nextInt(16);
        int centerY = world.getHighestBlockYAt(centerX, centerZ) - 1;
        if (world.getBlockAt(centerX, centerY, centerZ).getType() != Material.GRASS_BLOCK) {
            return;
        }

        int radius = 2 + random.nextInt(3);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radius * radius) {
                    continue;
                }

                int x = centerX + dx;
                int z = centerZ + dz;
                int y = world.getHighestBlockYAt(x, z) - 1;

                world.getBlockAt(x, y, z).setType(Material.WATER, false);
                world.getBlockAt(x, y + 1, z).setType(Material.AIR, false);
            }
        }
    }

    private void generatePasture(World world, Random random, Chunk chunk) {
        int startX = (chunk.getX() << 4) + random.nextInt(8);
        int startZ = (chunk.getZ() << 4) + random.nextInt(8);

        for (int i = 0; i < 24; i++) {
            int x = startX + random.nextInt(8);
            int z = startZ + random.nextInt(8);
            int y = world.getHighestBlockYAt(x, z);

            Block ground = world.getBlockAt(x, y - 1, z);
            Block plant = world.getBlockAt(x, y, z);
            if (ground.getType() != Material.GRASS_BLOCK || !plant.isEmpty()) {
                continue;
            }

            double roll = random.nextDouble();
            if (roll < 0.65) {
                plant.setType(Material.SHORT_GRASS, false);
            } else if (roll < 0.9) {
                plant.setType(Material.TALL_GRASS, false);
            } else {
                plant.setType(randomFlower(random), false);
            }
        }
    }

    private Material randomFlower(Random random) {
        Material[] flowers = {
                Material.DANDELION,
                Material.POPPY,
                Material.ALLIUM,
                Material.AZURE_BLUET,
                Material.RED_TULIP,
                Material.ORANGE_TULIP,
                Material.WHITE_TULIP,
                Material.PINK_TULIP,
                Material.OXEYE_DAISY,
                Material.CORNFLOWER
        };
        return flowers[random.nextInt(flowers.length)];
    }
}
