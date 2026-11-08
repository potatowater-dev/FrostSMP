package com.frost.plots.storage;

import com.frost.plots.FrostPlotsPlugin;
import com.frost.plots.model.Plot;
import com.frost.plots.model.PlotPrivacy;
import com.frost.plots.model.PlotRole;
import com.frost.plots.util.SchedulerUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class PlotDAO {

    private static final String CREATE_SQL = """
            CREATE TABLE IF NOT EXISTS plots (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              owner_uuid TEXT NOT NULL,
              world TEXT NOT NULL,
              center_x REAL NOT NULL,
              center_y REAL NOT NULL,
              center_z REAL NOT NULL,
              spawn_x REAL,
              spawn_y REAL,
              spawn_z REAL,
              spawn_yaw REAL NOT NULL DEFAULT 0,
              spawn_pitch REAL NOT NULL DEFAULT 0,
              tier_index INTEGER NOT NULL DEFAULT 0,
              privacy TEXT NOT NULL DEFAULT 'PUBLIC',
              members TEXT NOT NULL DEFAULT '{}'
            )
            """;

    private final FrostPlotsPlugin plugin;
    private final SchedulerUtil scheduler;
    private final ExecutorService executor;
    private Connection connection;

    public PlotDAO(FrostPlotsPlugin plugin, SchedulerUtil scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "frostplots-db");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void open() {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }
        File dbFile = new File(folder, "plots.db");
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute(CREATE_SQL);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to open FrostPlots SQLite database", exception);
        }
    }

    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to close SQLite connection", exception);
            }
        }
    }

    public List<Plot> loadAllBlocking() {
        try {
            return executor.submit(this::loadAll).get(30, TimeUnit.SECONDS);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load plots", exception);
            return List.of();
        }
    }

    public void insertAsync(Plot plot, Consumer<Plot> onSuccess, Consumer<Exception> onError) {
        executor.execute(() -> {
            try {
                Plot saved = insert(plot);
                scheduler.sync(() -> onSuccess.accept(saved));
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Failed to insert plot", exception);
                scheduler.sync(() -> onError.accept(exception));
            }
        });
    }

    public void updateAsync(Plot plot) {
        executor.execute(() -> {
            try {
                update(plot);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update plot #" + plot.getId(), exception);
            }
        });
    }

    public void deleteAsync(int plotId, Runnable onSuccess) {
        executor.execute(() -> {
            try {
                delete(plotId);
                if (onSuccess != null) {
                    scheduler.sync(onSuccess);
                }
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete plot #" + plotId, exception);
            }
        });
    }

    private List<Plot> loadAll() throws SQLException {
        List<Plot> plots = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM plots")) {
            while (resultSet.next()) {
                plots.add(fromResultSet(resultSet));
            }
        }
        return plots;
    }

    private Plot insert(Plot plot) throws SQLException {
        String sql = """
                INSERT INTO plots (owner_uuid, world, center_x, center_y, center_z, spawn_x, spawn_y, spawn_z,
                  spawn_yaw, spawn_pitch, tier_index, privacy, members)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindPlot(statement, plot);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("SQLite did not return a plot id");
                }
                plot.setId(keys.getInt(1));
            }
        }
        return plot;
    }

    private void update(Plot plot) throws SQLException {
        String sql = """
                UPDATE plots SET owner_uuid=?, world=?, center_x=?, center_y=?, center_z=?, spawn_x=?, spawn_y=?,
                  spawn_z=?, spawn_yaw=?, spawn_pitch=?, tier_index=?, privacy=?, members=? WHERE id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPlot(statement, plot);
            statement.setInt(14, plot.getId());
            statement.executeUpdate();
        }
    }

    private void delete(int plotId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM plots WHERE id=?")) {
            statement.setInt(1, plotId);
            statement.executeUpdate();
        }
    }

    private void bindPlot(PreparedStatement statement, Plot plot) throws SQLException {
        statement.setString(1, plot.getOwner().toString());
        statement.setString(2, plot.getWorldName());
        statement.setDouble(3, plot.getCenterX());
        statement.setDouble(4, plot.getCenterY());
        statement.setDouble(5, plot.getCenterZ());
        if (plot.getSpawnX() == null) {
            statement.setObject(6, null);
        } else {
            statement.setDouble(6, plot.getSpawnX());
        }
        if (plot.getSpawnY() == null) {
            statement.setObject(7, null);
        } else {
            statement.setDouble(7, plot.getSpawnY());
        }
        if (plot.getSpawnZ() == null) {
            statement.setObject(8, null);
        } else {
            statement.setDouble(8, plot.getSpawnZ());
        }
        statement.setFloat(9, plot.getSpawnYaw());
        statement.setFloat(10, plot.getSpawnPitch());
        statement.setInt(11, plot.getTierIndex());
        statement.setString(12, plot.getPrivacy() == null ? PlotPrivacy.PUBLIC.name() : plot.getPrivacy().name());
        statement.setString(13, serializeMembers(plot.getMembers()));
    }

    private Plot fromResultSet(ResultSet resultSet) throws SQLException {
        Plot plot = new Plot();
        plot.setId(resultSet.getInt("id"));
        plot.setOwner(UUID.fromString(resultSet.getString("owner_uuid")));
        plot.setWorldName(resultSet.getString("world"));
        plot.setCenterX(resultSet.getDouble("center_x"));
        plot.setCenterY(resultSet.getDouble("center_y"));
        plot.setCenterZ(resultSet.getDouble("center_z"));
        double spawnX = resultSet.getDouble("spawn_x");
        plot.setSpawnX(resultSet.wasNull() ? null : spawnX);
        double spawnY = resultSet.getDouble("spawn_y");
        plot.setSpawnY(resultSet.wasNull() ? null : spawnY);
        double spawnZ = resultSet.getDouble("spawn_z");
        plot.setSpawnZ(resultSet.wasNull() ? null : spawnZ);
        plot.setSpawnYaw(resultSet.getFloat("spawn_yaw"));
        plot.setSpawnPitch(resultSet.getFloat("spawn_pitch"));
        plot.setTierIndex(resultSet.getInt("tier_index"));
        String privacy = resultSet.getString("privacy");
        plot.setPrivacy(privacy == null ? PlotPrivacy.PUBLIC : PlotPrivacy.valueOf(privacy));
        plot.setMembers(deserializeMembers(resultSet.getString("members")));
        return plot;
    }

    private String serializeMembers(Map<UUID, PlotRole> members) {
        JsonObject object = new JsonObject();
        if (members != null) {
            members.forEach((uuid, role) -> object.addProperty(uuid.toString(), role.name()));
        }
        return object.toString();
    }

    private Map<UUID, PlotRole> deserializeMembers(String raw) {
        Map<UUID, PlotRole> members = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return members;
        }
        JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            PlotRole role = PlotRole.fromString(entry.getValue().getAsString());
            if (role == null) {
                continue;
            }
            try {
                members.put(UUID.fromString(entry.getKey()), role);
            } catch (IllegalArgumentException ignored) {
                // skip corrupt uuid
            }
        }
        return members;
    }
}
