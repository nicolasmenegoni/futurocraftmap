package com.futurocraft.mapgen;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

import java.util.Random;

public class SurfaceFeaturePopulator extends BlockPopulator {

    private static final Material[] LOGS = {
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG
    };

    private static final Material[] LEAVES = {
            Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.SPRUCE_LEAVES,
            Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES
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

        if (!isInteriorChunk(chunk)) {
            return;
        }

        if (random.nextDouble() < settings.lakeChance()) {
            generateLake(world, random, chunk);
        }

        if (random.nextDouble() < settings.pastureChance()) {
            generatePasture(world, random, chunk);
        }

        int treeAttempts = 4 + random.nextInt(5);
        for (int i = 0; i < treeAttempts; i++) {
            if (random.nextDouble() < settings.treeChance()) {
                generateArtificialTree(world, random, chunk);
            }
        }

        if (random.nextDouble() < settings.houseChance()) {
            generateHouse(world, random, chunk);
        }
    }

    private boolean isInsideMap(Chunk chunk) {
        int chunkCenterX = (chunk.getX() << 4) + 8;
        int chunkCenterZ = (chunk.getZ() << 4) + 8;
        int half = settings.mapSize() / 2;
        return chunkCenterX >= -half && chunkCenterX <= half && chunkCenterZ >= -half && chunkCenterZ <= half;
    }

    private boolean isInteriorChunk(Chunk chunk) {
        int chunkCenterX = (chunk.getX() << 4) + 8;
        int chunkCenterZ = (chunk.getZ() << 4) + 8;
        int half = settings.mapSize() / 2;
        int margin = settings.borderSize() + 16;
        return chunkCenterX > (-half + margin)
                && chunkCenterX < (half - margin)
                && chunkCenterZ > (-half + margin)
                && chunkCenterZ < (half - margin);
    }

    private void generateArtificialTree(World world, Random random, Chunk chunk) {
        int x = (chunk.getX() << 4) + random.nextInt(16);
        int z = (chunk.getZ() << 4) + random.nextInt(16);
        int y = world.getHighestBlockYAt(x, z);

        Block ground = world.getBlockAt(x, y - 1, z);
        if (ground.getType() != Material.GRASS_BLOCK) {
            return;
        }

        Material log = LOGS[random.nextInt(LOGS.length)];
        Material leaves = LEAVES[random.nextInt(LEAVES.length)];
        int height = 4 + random.nextInt(3);

        for (int i = 0; i < height; i++) {
            world.getBlockAt(x, y + i, z).setType(log, false);
        }

        int canopyY = y + height - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (Math.abs(dx) + Math.abs(dz) > 3 && dy == 0) continue;
                    Block b = world.getBlockAt(x + dx, canopyY + dy, z + dz);
                    if (b.isEmpty()) {
                        b.setType(leaves, false);
                    }
                }
            }
        }
    }

    private void generateLake(World world, Random random, Chunk chunk) {
        int centerX = (chunk.getX() << 4) + random.nextInt(16);
        int centerZ = (chunk.getZ() << 4) + random.nextInt(16);
        int radius = 3 + random.nextInt(3);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radius * radius) continue;

                int x = centerX + dx;
                int z = centerZ + dz;
                int y = world.getHighestBlockYAt(x, z) - 1;
                Block ground = world.getBlockAt(x, y, z);
                if (ground.getType() != Material.GRASS_BLOCK && ground.getType() != Material.DIRT) {
                    continue;
                }

                ground.setType(Material.WATER, false);
                world.getBlockAt(x, y - 1, z).setType(Material.SAND, false);
                world.getBlockAt(x, y + 1, z).setType(Material.AIR, false);
            }
        }
    }

    private void generatePasture(World world, Random random, Chunk chunk) {
        int startX = (chunk.getX() << 4) + random.nextInt(8);
        int startZ = (chunk.getZ() << 4) + random.nextInt(8);

        for (int i = 0; i < 28; i++) {
            int x = startX + random.nextInt(8);
            int z = startZ + random.nextInt(8);
            int y = world.getHighestBlockYAt(x, z);

            Block ground = world.getBlockAt(x, y - 1, z);
            Block plant = world.getBlockAt(x, y, z);
            if (ground.getType() != Material.GRASS_BLOCK || !plant.isEmpty()) {
                continue;
            }

            double roll = random.nextDouble();
            if (roll < 0.6) {
                plant.setType(Material.SHORT_GRASS, false);
            } else if (roll < 0.9) {
                plant.setType(Material.TALL_GRASS, false);
            } else {
                plant.setType(randomFlower(random), false);
            }
        }
    }

    private void generateHouse(World world, Random random, Chunk chunk) {
        int x = (chunk.getX() << 4) + 2 + random.nextInt(8);
        int z = (chunk.getZ() << 4) + 2 + random.nextInt(8);
        int y = world.getHighestBlockYAt(x, z);

        if (world.getBlockAt(x, y - 1, z).getType() != Material.GRASS_BLOCK) {
            return;
        }

        int variant = random.nextInt(3);
        Material wall;
        Material roof;
        switch (variant) {
            case 0 -> { wall = Material.OAK_PLANKS; roof = Material.SPRUCE_STAIRS; }
            case 1 -> { wall = Material.COBBLESTONE; roof = Material.BRICK_STAIRS; }
            default -> { wall = Material.BIRCH_PLANKS; roof = Material.DARK_OAK_STAIRS; }
        }

        int w = 5;
        int l = 5;
        int h = 4;

        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < l; dz++) {
                world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.OAK_PLANKS, false);
                for (int dy = 0; dy < h; dy++) {
                    Block b = world.getBlockAt(x + dx, y + dy, z + dz);
                    boolean edge = dx == 0 || dz == 0 || dx == w - 1 || dz == l - 1;
                    if (dy == h - 1) {
                        b.setType(Material.AIR, false);
                    } else if (edge) {
                        b.setType(wall, false);
                    } else {
                        b.setType(Material.AIR, false);
                    }
                }
                world.getBlockAt(x + dx, y + h, z + dz).setType(roof, false);
            }
        }

        world.getBlockAt(x + 2, y, z).setType(Material.OAK_DOOR, false);
        world.getBlockAt(x + 2, y + 1, z).setType(Material.OAK_DOOR, false);
        world.getBlockAt(x + 1, y + 1, z).setType(Material.GLASS_PANE, false);
        world.getBlockAt(x + 3, y + 1, z).setType(Material.GLASS_PANE, false);
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
