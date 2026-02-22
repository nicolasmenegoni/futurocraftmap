package com.futurocraft.mapgen;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;

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
        int maxY = Math.min(world.getMaxHeight() - 1, settings.waterLevel() + settings.glassWallHeight() + 8);

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

                Biome biome = biomeFor(worldX, worldZ, edgeDistance);
                world.setBiome(worldX, groundY, worldZ, biome);

                if (worldX == minX || worldX == maxX || worldZ == minZ || worldZ == maxZ) {
                    int wallTop = settings.waterLevel() + settings.glassWallHeight();
                    for (int y = settings.bedrockY(); y <= wallTop; y++) {
                        world.getBlockAt(worldX, y, worldZ).setType(Material.GLASS, false);
                    }
                }
            }
        }

        placeChunkFeatures(world, chunk, random);
    }

    private void placeChunkFeatures(World world, Chunk chunk, Random random) {
        if (!isInteriorChunk(chunk)) {
            return;
        }

        int attempts = 7;
        for (int i = 0; i < attempts; i++) {
            if (random.nextDouble() < settings.treeChance()) {
                placeBiomeTree(world, chunk, random);
            }
        }

        if (random.nextDouble() < settings.lakeChance()) {
            placeLake(world, chunk, random);
        }

        if (random.nextDouble() < settings.houseChance()) {
            placeHouse(world, chunk, random);
        }

        if (random.nextDouble() < 0.04) {
            spawnRareAnimal(world, chunk, random);
        }
    }

    private boolean isInteriorChunk(Chunk chunk) {
        int chunkCenterX = (chunk.getX() << 4) + 8;
        int chunkCenterZ = (chunk.getZ() << 4) + 8;
        int half = settings.mapSize() / 2;
        int margin = settings.borderSize() + 24;
        return chunkCenterX > (-half + margin)
                && chunkCenterX < (half - margin)
                && chunkCenterZ > (-half + margin)
                && chunkCenterZ < (half - margin);
    }

    private void placeBiomeTree(World world, Chunk chunk, Random random) {
        int x = (chunk.getX() << 4) + random.nextInt(16);
        int z = (chunk.getZ() << 4) + random.nextInt(16);
        int y = topSolidY(world, x, z);
        if (world.getBlockAt(x, y, z).getType() != Material.GRASS_BLOCK) return;

        Biome biome = world.getBiome(x, y, z);
        Material log = biomeLog(biome, random);
        Material leaves = biomeLeaves(biome, random);

        int trunkBase = y + 1;
        int height = 4 + random.nextInt(3);

        for (int i = 0; i < height; i++) {
            world.getBlockAt(x, trunkBase + i, z).setType(log, false);
        }

        int canopyY = trunkBase + height - 1;
        int radius = biome == Biome.JUNGLE ? 3 : 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if ((dx * dx) + (dz * dz) > radius * radius + 1) continue;
                    Material current = world.getBlockAt(x + dx, canopyY + dy, z + dz).getType();
                    if (current == Material.AIR || current == Material.SHORT_GRASS || current == Material.TALL_GRASS) {
                        world.getBlockAt(x + dx, canopyY + dy, z + dz).setType(leaves, false);
                    }
                }
            }
        }
    }

    private void placeLake(World world, Chunk chunk, Random random) {
        int cx = (chunk.getX() << 4) + 4 + random.nextInt(8);
        int cz = (chunk.getZ() << 4) + 4 + random.nextInt(8);
        int radius = 2 + random.nextInt(3);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radius * radius) continue;
                int x = cx + dx;
                int z = cz + dz;
                int y = topSolidY(world, x, z);
                Material ground = world.getBlockAt(x, y, z).getType();
                if (ground != Material.GRASS_BLOCK && ground != Material.DIRT) continue;
                world.getBlockAt(x, y, z).setType(Material.WATER, false);
                world.getBlockAt(x, y - 1, z).setType(Material.SAND, false);
                world.getBlockAt(x, y + 1, z).setType(Material.AIR, false);
            }
        }
    }

    private void placeHouse(World world, Chunk chunk, Random random) {
        int x = (chunk.getX() << 4) + 3 + random.nextInt(6);
        int z = (chunk.getZ() << 4) + 3 + random.nextInt(6);
        int y = topSolidY(world, x, z);
        if (world.getBlockAt(x, y, z).getType() != Material.GRASS_BLOCK) return;

        Material wall = random.nextBoolean() ? Material.OAK_PLANKS : Material.COBBLESTONE;
        Material roof = random.nextBoolean() ? Material.SPRUCE_PLANKS : Material.BRICKS;

        int w = 5, l = 5, h = 4;
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < l; dz++) {
                world.getBlockAt(x + dx, y, z + dz).setType(Material.OAK_PLANKS, false);
                for (int dy = 1; dy <= h; dy++) {
                    boolean edge = dx == 0 || dz == 0 || dx == w - 1 || dz == l - 1;
                    world.getBlockAt(x + dx, y + dy, z + dz).setType(edge ? wall : Material.AIR, false);
                }
                world.getBlockAt(x + dx, y + h + 1, z + dz).setType(roof, false);
            }
        }
        world.getBlockAt(x + 2, y + 1, z).setType(Material.AIR, false);
        world.getBlockAt(x + 2, y + 2, z).setType(Material.AIR, false);
    }

    private void spawnRareAnimal(World world, Chunk chunk, Random random) {
        int x = (chunk.getX() << 4) + random.nextInt(16);
        int z = (chunk.getZ() << 4) + random.nextInt(16);
        int y = topSolidY(world, x, z);
        if (world.getBlockAt(x, y, z).getType() != Material.GRASS_BLOCK) return;

        EntityType type;
        double roll = random.nextDouble();
        if (roll < 0.3) type = EntityType.SHEEP;
        else if (roll < 0.55) type = EntityType.COW;
        else if (roll < 0.75) type = EntityType.PIG;
        else if (roll < 0.9) type = EntityType.CHICKEN;
        else type = EntityType.HORSE;

        world.spawnEntity(new Location(world, x + 0.5, y + 1, z + 0.5), type);
    }

    private int topSolidY(World world, int x, int z) {
        for (int y = settings.surfaceY() + 8; y >= settings.bedrockY(); y--) {
            if (!world.getBlockAt(x, y, z).isEmpty() && !world.getBlockAt(x, y, z).isLiquid()) {
                return y;
            }
        }
        return settings.surfaceY();
    }

    private Biome biomeFor(int x, int z, int edgeDistance) {
        if (edgeDistance < settings.borderSize()) {
            return Biome.BEACH;
        }

        long mix = Math.abs((x * 73428767L) ^ (z * 912931L));
        int b = (int) (mix % 6);
        return switch (b) {
            case 0 -> Biome.FOREST;
            case 1 -> Biome.BIRCH_FOREST;
            case 2 -> Biome.TAIGA;
            case 3 -> Biome.JUNGLE;
            case 4 -> Biome.SAVANNA;
            default -> Biome.CHERRY_GROVE;
        };
    }

    private Material biomeLog(Biome biome, Random random) {
        return switch (biome) {
            case BIRCH_FOREST -> Material.BIRCH_LOG;
            case TAIGA -> Material.SPRUCE_LOG;
            case JUNGLE -> Material.JUNGLE_LOG;
            case SAVANNA -> Material.ACACIA_LOG;
            case CHERRY_GROVE -> Material.CHERRY_LOG;
            case FOREST -> random.nextBoolean() ? Material.OAK_LOG : Material.DARK_OAK_LOG;
            default -> Material.OAK_LOG;
        };
    }

    private Material biomeLeaves(Biome biome, Random random) {
        return switch (biome) {
            case BIRCH_FOREST -> Material.BIRCH_LEAVES;
            case TAIGA -> Material.SPRUCE_LEAVES;
            case JUNGLE -> Material.JUNGLE_LEAVES;
            case SAVANNA -> Material.ACACIA_LEAVES;
            case CHERRY_GROVE -> Material.CHERRY_LEAVES;
            case FOREST -> random.nextBoolean() ? Material.OAK_LEAVES : Material.DARK_OAK_LEAVES;
            default -> Material.OAK_LEAVES;
        };
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
