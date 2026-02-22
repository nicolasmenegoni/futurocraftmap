package com.futurocraft.mapgen;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StorageManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public StorageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "lifecycle.db");
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement st = connection.createStatement()) {
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS natural_breaks (
                            world TEXT NOT NULL,
                            x INTEGER NOT NULL,
                            y INTEGER NOT NULL,
                            z INTEGER NOT NULL,
                            original_type TEXT NOT NULL,
                            broke_at_ms INTEGER NOT NULL,
                            broke_at_tick INTEGER NOT NULL,
                            PRIMARY KEY (world, x, y, z)
                        )
                        """);

                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS player_blocks (
                            world TEXT NOT NULL,
                            x INTEGER NOT NULL,
                            y INTEGER NOT NULL,
                            z INTEGER NOT NULL,
                            block_type TEXT NOT NULL,
                            owner_uuid TEXT NOT NULL,
                            placed_at_ms INTEGER NOT NULL,
                            placed_at_tick INTEGER NOT NULL,
                            last_owner_visit_ms INTEGER NOT NULL,
                            last_owner_visit_tick INTEGER NOT NULL,
                            chunk_x INTEGER NOT NULL,
                            chunk_z INTEGER NOT NULL,
                            PRIMARY KEY (world, x, y, z)
                        )
                        """);

                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS chunk_activity (
                            world TEXT NOT NULL,
                            chunk_x INTEGER NOT NULL,
                            chunk_z INTEGER NOT NULL,
                            last_seen_ms INTEGER NOT NULL,
                            last_seen_tick INTEGER NOT NULL,
                            PRIMARY KEY (world, chunk_x, chunk_z)
                        )
                        """);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize SQLite", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

    public void upsertChunkActivity(String world, int chunkX, int chunkZ, long nowMs, long nowTick) {
        String sql = """
                INSERT INTO chunk_activity (world, chunk_x, chunk_z, last_seen_ms, last_seen_tick)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(world, chunk_x, chunk_z)
                DO UPDATE SET last_seen_ms=excluded.last_seen_ms, last_seen_tick=excluded.last_seen_tick
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.setLong(4, nowMs);
            ps.setLong(5, nowTick);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not update chunk activity: " + e.getMessage());
        }
    }

    public ChunkActivity getChunkActivity(String world, int chunkX, int chunkZ) {
        String sql = "SELECT last_seen_ms, last_seen_tick FROM chunk_activity WHERE world=? AND chunk_x=? AND chunk_z=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new ChunkActivity(rs.getLong("last_seen_ms"), rs.getLong("last_seen_tick"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not read chunk activity: " + e.getMessage());
        }
        return null;
    }

    public boolean isPlayerPlacedBlock(String world, int x, int y, int z) {
        String sql = "SELECT 1 FROM player_blocks WHERE world=? AND x=? AND y=? AND z=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not check placed block: " + e.getMessage());
            return false;
        }
    }

    public void registerNaturalBreak(String world, int x, int y, int z, Material original, long nowMs, long nowTick) {
        String sql = """
                INSERT INTO natural_breaks (world, x, y, z, original_type, broke_at_ms, broke_at_tick)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(world, x, y, z)
                DO UPDATE SET original_type=excluded.original_type, broke_at_ms=excluded.broke_at_ms, broke_at_tick=excluded.broke_at_tick
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.setString(5, original.name());
            ps.setLong(6, nowMs);
            ps.setLong(7, nowTick);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not register natural break: " + e.getMessage());
        }
    }

    public void registerPlacement(String world, int x, int y, int z, Material type, UUID owner, long nowMs, long nowTick, int chunkX, int chunkZ) {
        String sql = """
                INSERT INTO player_blocks (world, x, y, z, block_type, owner_uuid, placed_at_ms, placed_at_tick, last_owner_visit_ms, last_owner_visit_tick, chunk_x, chunk_z)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(world, x, y, z)
                DO UPDATE SET block_type=excluded.block_type, owner_uuid=excluded.owner_uuid,
                placed_at_ms=excluded.placed_at_ms, placed_at_tick=excluded.placed_at_tick,
                last_owner_visit_ms=excluded.last_owner_visit_ms, last_owner_visit_tick=excluded.last_owner_visit_tick,
                chunk_x=excluded.chunk_x, chunk_z=excluded.chunk_z
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.setString(5, type.name());
            ps.setString(6, owner.toString());
            ps.setLong(7, nowMs);
            ps.setLong(8, nowTick);
            ps.setLong(9, nowMs);
            ps.setLong(10, nowTick);
            ps.setInt(11, chunkX);
            ps.setInt(12, chunkZ);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not register placement: " + e.getMessage());
        }
    }

    public void deleteNaturalBreak(String world, int x, int y, int z) {
        delete("DELETE FROM natural_breaks WHERE world=? AND x=? AND y=? AND z=?", world, x, y, z);
    }

    public void deletePlayerBlock(String world, int x, int y, int z) {
        delete("DELETE FROM player_blocks WHERE world=? AND x=? AND y=? AND z=?", world, x, y, z);
    }

    private void delete(String sql, String world, int x, int y, int z) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not delete record: " + e.getMessage());
        }
    }

    public void touchOwnerVisitsForChunk(String world, int chunkX, int chunkZ, UUID owner, long nowMs, long nowTick) {
        String sql = """
                UPDATE player_blocks
                SET last_owner_visit_ms=?, last_owner_visit_tick=?
                WHERE world=? AND chunk_x=? AND chunk_z=? AND owner_uuid=?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, nowMs);
            ps.setLong(2, nowTick);
            ps.setString(3, world);
            ps.setInt(4, chunkX);
            ps.setInt(5, chunkZ);
            ps.setString(6, owner.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not update owner visits: " + e.getMessage());
        }
    }

    public List<NaturalBreakRecord> getNaturalBreaksInChunk(String world, int chunkX, int chunkZ) {
        String sql = "SELECT x, y, z, original_type FROM natural_breaks WHERE world=? AND (x >> 4)=? AND (z >> 4)=?";
        List<NaturalBreakRecord> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new NaturalBreakRecord(
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        Material.valueOf(rs.getString("original_type"))
                ));
            }
        } catch (SQLException | IllegalArgumentException e) {
            plugin.getLogger().warning("Could not read natural breaks in chunk: " + e.getMessage());
        }
        return result;
    }

    public List<PlayerBlockRecord> getPlayerBlocksInChunk(String world, int chunkX, int chunkZ) {
        String sql = "SELECT x, y, z, block_type, owner_uuid, last_owner_visit_ms, last_owner_visit_tick FROM player_blocks WHERE world=? AND chunk_x=? AND chunk_z=?";
        List<PlayerBlockRecord> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new PlayerBlockRecord(
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        Material.valueOf(rs.getString("block_type")),
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getLong("last_owner_visit_ms"),
                        rs.getLong("last_owner_visit_tick")
                ));
            }
        } catch (SQLException | IllegalArgumentException e) {
            plugin.getLogger().warning("Could not read player blocks in chunk: " + e.getMessage());
        }
        return result;
    }

    public long getWorldTick(World world) {
        return world.getFullTime();
    }

    public record NaturalBreakRecord(int x, int y, int z, Material originalType) {}

    public record PlayerBlockRecord(int x, int y, int z, Material blockType, UUID owner, long lastOwnerVisitMs, long lastOwnerVisitTick) {}

    public record ChunkActivity(long lastSeenMs, long lastSeenTick) {}
}
