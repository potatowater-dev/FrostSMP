package com.frostsmp.crates.service;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.config.CrateDefinitionStorage;
import com.frostsmp.crates.config.PlacedCrateStorage;
import com.frostsmp.crates.config.PluginConfig;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.PlacedCrate;
import com.frostsmp.crates.util.GradientUtil;
import com.frostsmp.crates.util.MessageUtil;
import com.frostsmp.crates.util.PluginKeys;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class HologramService implements Listener {

    private static final String LINE_TITLE = "title";
    private static final String LINE_SUBTITLE = "subtitle";
    private static final String LINE_KEYS = "keys";

    private final FrostCratesPlugin plugin;
    private final PluginConfig config;
    private final CrateDefinitionStorage definitions;
    private final PlacedCrateStorage placedCrates;
    private final KeyService keyService;
    private final PluginKeys keys;

    private final Map<String, SharedHologram> sharedByPlacedId = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, UUID>> keyDisplaysByPlacedId = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public HologramService(
            FrostCratesPlugin plugin,
            PluginConfig config,
            CrateDefinitionStorage definitions,
            PlacedCrateStorage placedCrates,
            KeyService keyService,
            PluginKeys keys
    ) {
        this.plugin = plugin;
        this.config = config;
        this.definitions = definitions;
        this.placedCrates = placedCrates;
        this.keyService = keyService;
        this.keys = keys;
    }

    public void start() {
        stop();
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateKeyDisplays, 20L, 20L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void respawnAll() {
        cleanupOrphanHolograms();
        removeAll();
        for (PlacedCrate placed : placedCrates.getAll()) {
            CrateDefinition definition = definitions.get(placed.getDefinitionId());
            if (definition != null) {
                spawn(placed, definition);
            }
        }
    }

    public void cleanupOrphanHolograms() {
        Set<String> validIds = placedCrates.getAll().stream()
                .map(PlacedCrate::getId)
                .collect(Collectors.toSet());
        sharedByPlacedId.keySet().removeIf(id -> !validIds.contains(id));
        keyDisplaysByPlacedId.keySet().removeIf(id -> !validIds.contains(id));

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(TextDisplay.class)) {
                String placedId = entity.getPersistentDataContainer().get(keys.hologramPlacedId, PersistentDataType.STRING);
                if (placedId == null) {
                    continue;
                }
                if (!validIds.contains(placedId)) {
                    entity.remove();
                }
            }
        }
    }

    public void spawn(PlacedCrate placed, CrateDefinition definition) {
        Location base = placed.getLocation();
        World world = base.getWorld();
        if (world == null) {
            return;
        }

        ensureChunkLoaded(base);

        remove(placed);
        purgeTaggedAt(base, placed.getId());

        UUID titleId = spawnSharedLine(base, placed.getId(), LINE_TITLE, config.getHologramLine1Height(), buildTitle(definition));
        UUID subtitleId = spawnSharedLine(base, placed.getId(), LINE_SUBTITLE, config.getHologramLine2Height(), config.getHologramSubtitle());
        sharedByPlacedId.put(placed.getId(), new SharedHologram(titleId, subtitleId));
        keyDisplaysByPlacedId.putIfAbsent(placed.getId(), new ConcurrentHashMap<>());
    }

    public void remove(Location location) {
        PlacedCrate placed = placedCrates.getAt(location);
        if (placed != null) {
            remove(placed);
        }
    }

    public void remove(PlacedCrate placed) {
        removeShared(placed.getId());
        removeAllKeyDisplays(placed.getId());
        Location base = placed.getLocation();
        if (base.getWorld() != null) {
            purgeTaggedAt(base, placed.getId());
        }
    }

    public void removeAll() {
        for (String placedId : new HashSet<>(sharedByPlacedId.keySet())) {
            removeShared(placedId);
        }
        for (String placedId : new HashSet<>(keyDisplaysByPlacedId.keySet())) {
            removeAllKeyDisplays(placedId);
        }
        sharedByPlacedId.clear();
        keyDisplaysByPlacedId.clear();
    }

    public void refreshKeysFor(Player player) {
        for (PlacedCrate placed : placedCrates.getAll()) {
            if (!isNear(player, placed.getLocation())) {
                removeKeyDisplay(placed.getId(), player.getUniqueId());
                continue;
            }
            CrateDefinition definition = definitions.get(placed.getDefinitionId());
            if (definition != null) {
                ensureKeyDisplay(player, placed, definition);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        for (PlacedCrate placed : placedCrates.getAll()) {
            Location location = placed.getLocation();
            if (location.getWorld() == null || !location.getWorld().equals(chunk.getWorld())) {
                continue;
            }
            if (location.getBlockX() >> 4 != chunk.getX() || location.getBlockZ() >> 4 != chunk.getZ()) {
                continue;
            }
            if (!hasSharedHologram(placed.getId())) {
                CrateDefinition definition = definitions.get(placed.getDefinitionId());
                if (definition != null) {
                    spawn(placed, definition);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        for (String placedId : keyDisplaysByPlacedId.keySet()) {
            removeKeyDisplay(placedId, playerId);
        }
    }

    private void updateKeyDisplays() {
        int viewDistance = config.getHologramViewDistance();
        long viewDistanceSquared = (long) viewDistance * viewDistance;

        for (PlacedCrate placed : placedCrates.getAll()) {
            CrateDefinition definition = definitions.get(placed.getDefinitionId());
            if (definition == null) {
                continue;
            }
            Location location = placed.getLocation();
            World world = location.getWorld();
            if (world == null) {
                continue;
            }

            Set<UUID> nearbyPlayers = new HashSet<>();
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(location) > viewDistanceSquared) {
                    removeKeyDisplay(placed.getId(), player.getUniqueId());
                    continue;
                }
                nearbyPlayers.add(player.getUniqueId());
                ensureKeyDisplay(player, placed, definition);
            }

            Map<UUID, UUID> displays = keyDisplaysByPlacedId.get(placed.getId());
            if (displays == null) {
                continue;
            }
            for (UUID playerId : new HashSet<>(displays.keySet())) {
                if (!nearbyPlayers.contains(playerId)) {
                    removeKeyDisplay(placed.getId(), playerId);
                }
            }
        }
    }

    private void ensureKeyDisplay(Player player, PlacedCrate placed, CrateDefinition definition) {
        String placedId = placed.getId();
        Map<UUID, UUID> displays = keyDisplaysByPlacedId.computeIfAbsent(placedId, ignored -> new ConcurrentHashMap<>());
        UUID existingId = displays.get(player.getUniqueId());

        int keyCount = keyService.get(player, definition.getId());
        String text = config.getHologramKeysFormat().replace("{keys}", String.valueOf(keyCount));

        TextDisplay display;
        if (existingId != null) {
            Entity entity = placed.getLocation().getWorld().getEntity(existingId);
            if (entity instanceof TextDisplay existing) {
                existing.text(MessageUtil.parse(text));
                player.showEntity(plugin, existing);
                return;
            }
            displays.remove(player.getUniqueId());
        }

        display = spawnKeyLine(placed.getLocation(), placedId, player, text);
        displays.put(player.getUniqueId(), display.getUniqueId());
    }

    private TextDisplay spawnKeyLine(Location base, String placedId, Player viewer, String miniMessageText) {
        Location spawnAt = base.clone().add(0.5, config.getHologramLine3Height(), 0.5);
        World world = spawnAt.getWorld();
        TextDisplay display = world.spawn(spawnAt, TextDisplay.class, entity -> configureDisplay(entity, placedId, LINE_KEYS, miniMessageText, false));
        viewer.showEntity(plugin, display);
        return display;
    }

    private UUID spawnSharedLine(Location base, String placedId, String lineType, double heightOffset, String miniMessageText) {
        Location spawnAt = base.clone().add(0.5, heightOffset, 0.5);
        World world = spawnAt.getWorld();
        TextDisplay display = world.spawn(spawnAt, TextDisplay.class, entity -> configureDisplay(entity, placedId, lineType, miniMessageText, true));
        return display.getUniqueId();
    }

    private void configureDisplay(TextDisplay display, String placedId, String lineType, String miniMessageText, boolean persistent) {
        display.text(MessageUtil.parse(miniMessageText));
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(true);
        display.setShadowed(true);
        display.setDefaultBackground(false);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setPersistent(persistent);
        display.setInvulnerable(true);
        display.setGravity(false);
        display.getPersistentDataContainer().set(keys.hologramPlacedId, PersistentDataType.STRING, placedId);
        display.getPersistentDataContainer().set(keys.hologramLine, PersistentDataType.STRING, lineType);
        if (!persistent) {
            display.setVisibleByDefault(false);
        }
    }

    private void removeShared(String placedId) {
        SharedHologram hologram = sharedByPlacedId.remove(placedId);
        if (hologram == null) {
            return;
        }
        removeEntity(hologram.titleId());
        removeEntity(hologram.subtitleId());
    }

    private void removeAllKeyDisplays(String placedId) {
        Map<UUID, UUID> displays = keyDisplaysByPlacedId.remove(placedId);
        if (displays == null) {
            return;
        }
        for (UUID entityId : displays.values()) {
            removeEntity(entityId);
        }
    }

    private void removeKeyDisplay(String placedId, UUID playerId) {
        Map<UUID, UUID> displays = keyDisplaysByPlacedId.get(placedId);
        if (displays == null) {
            return;
        }
        UUID entityId = displays.remove(playerId);
        if (entityId != null) {
            removeEntity(entityId);
        }
    }

    private void removeEntity(UUID entityId) {
        for (World world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                entity.remove();
                return;
            }
        }
    }

    private void purgeTaggedAt(Location base, String placedId) {
        World world = base.getWorld();
        if (world == null) {
            return;
        }
        ensureChunkLoaded(base);
        int chunkX = base.getBlockX() >> 4;
        int chunkZ = base.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof TextDisplay display)) {
                continue;
            }
            String taggedPlacedId = display.getPersistentDataContainer().get(keys.hologramPlacedId, PersistentDataType.STRING);
            if (!placedId.equals(taggedPlacedId)) {
                continue;
            }
            Location entityLoc = entity.getLocation();
            if (entityLoc.getBlockX() == base.getBlockX()
                    && entityLoc.getBlockY() >= base.getBlockY()
                    && entityLoc.getBlockY() <= base.getBlockY() + 3
                    && entityLoc.getBlockZ() == base.getBlockZ()) {
                entity.remove();
            }
        }
    }

    private boolean hasSharedHologram(String placedId) {
        SharedHologram hologram = sharedByPlacedId.get(placedId);
        if (hologram == null) {
            return false;
        }
        World world = null;
        for (PlacedCrate placed : placedCrates.getAll()) {
            if (placed.getId().equals(placedId) && placed.getLocation().getWorld() != null) {
                world = placed.getLocation().getWorld();
                break;
            }
        }
        if (world == null) {
            return false;
        }
        Entity title = world.getEntity(hologram.titleId());
        return title != null && title.isValid();
    }

    private boolean isNear(Player player, Location location) {
        if (location.getWorld() == null || !player.getWorld().equals(location.getWorld())) {
            return false;
        }
        int viewDistance = config.getHologramViewDistance();
        return player.getLocation().distanceSquared(location) <= (long) viewDistance * viewDistance;
    }

    private void ensureChunkLoaded(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.getChunkAt(chunkX, chunkZ).load();
        }
    }

    private String buildTitle(CrateDefinition definition) {
        String plainName = PlainTextComponentSerializer.plainText().serialize(MessageUtil.parse(definition.getDisplayName()));
        if (plainName.isBlank()) {
            plainName = definition.getId();
        }
        return GradientUtil.letterGradient(
                plainName,
                config.getHologramGradientStart(),
                config.getHologramGradientEnd()
        );
    }

    private record SharedHologram(UUID titleId, UUID subtitleId) {
    }
}
