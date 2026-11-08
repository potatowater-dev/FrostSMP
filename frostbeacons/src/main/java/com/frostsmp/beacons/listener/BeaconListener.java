package com.frostsmp.beacons.listener;

import com.frostsmp.beacons.config.PluginConfig;
import com.frostsmp.beacons.service.BeaconRegistry;
import com.frostsmp.beacons.service.WandService;
import com.frostsmp.beacons.storage.BeaconEffectStorage;
import com.frostsmp.beacons.storage.BeamLinkStorage;
import com.frostsmp.beacons.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BeaconListener implements Listener {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final BeaconRegistry registry;
    private final WandService wandService;
    private final BeamLinkStorage beamLinks;
    private final BeaconEffectStorage effectStorage;
    private final Map<UUID, Location> selectedStarts = new ConcurrentHashMap<>();

    public BeaconListener(
            JavaPlugin plugin,
            PluginConfig config,
            BeaconRegistry registry,
            WandService wandService,
            BeamLinkStorage beamLinks,
            BeaconEffectStorage effectStorage
    ) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.wandService = wandService;
        this.beamLinks = beamLinks;
        this.effectStorage = effectStorage;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.BEACON) {
            registry.register(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.BEACON) {
            Location location = event.getBlock().getLocation();
            registry.unregister(location);
            beamLinks.remove(location);
            effectStorage.remove(location);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        registry.scanChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        registry.removeChunk(event.getChunk());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selectedStarts.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBeaconClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() != InventoryType.BEACON) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        Location location = null;
        if (holder instanceof Beacon beaconHolder) {
            location = beaconHolder.getLocation();
        } else if (holder instanceof BlockInventoryHolder blockHolder) {
            location = blockHolder.getBlock().getLocation();
        }
        if (location == null) {
            return;
        }
        Location finalLocation = location.clone();
        registry.register(finalLocation);
        Bukkit.getScheduler().runTask(plugin, () -> saveBeaconEffects(finalLocation));
    }

    private void saveBeaconEffects(Location location) {
        BlockState state = location.getBlock().getState();
        if (!(state instanceof Beacon beacon)) {
            return;
        }
        PotionEffectType primary = beacon.getPrimaryEffect() != null ? beacon.getPrimaryEffect().getType() : null;
        PotionEffectType secondary = beacon.getSecondaryEffect() != null ? beacon.getSecondaryEffect().getType() : null;
        if (primary != null || secondary != null) {
            effectStorage.put(location, primary, secondary);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWandUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!wandService.isWand(player.getInventory().getItemInMainHand())) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        event.setCancelled(true);
        if (player.getGameMode() == GameMode.CREATIVE) {
            player.setCooldown(player.getInventory().getItemInMainHand().getType(), 5);
        }
        if (!player.hasPermission("frostsmp.beacons.admin")) {
            MessageUtil.send(player, config.getNoPermission());
            return;
        }

        if (action == Action.LEFT_CLICK_BLOCK) {
            if (block.getType() != Material.BEACON) {
                MessageUtil.send(player, config.getStartMustBeBeacon());
                return;
            }
            Location start = block.getLocation();
            registry.register(start);
            selectedStarts.put(player.getUniqueId(), start.clone());
            MessageUtil.send(player, MessageUtil.format(
                    config.getStartSet(),
                    "location", BeamLinkStorage.format(start)
            ));
            return;
        }

        Location start = selectedStarts.get(player.getUniqueId());
        if (start == null) {
            MessageUtil.send(player, config.getNeedStartFirst());
            return;
        }
        if (start.getWorld() == null || !start.getWorld().equals(block.getWorld())) {
            MessageUtil.send(player, config.getNeedStartFirst());
            return;
        }
        if (block.getX() != start.getBlockX() || block.getZ() != start.getBlockZ()) {
            MessageUtil.send(player, config.getEndSameColumn());
            return;
        }
        if (block.getY() <= start.getBlockY()) {
            MessageUtil.send(player, config.getEndAboveStart());
            return;
        }

        Location end = block.getLocation();
        beamLinks.setEnd(start, end);
        MessageUtil.send(player, MessageUtil.format(
                config.getEndSet(),
                "location", BeamLinkStorage.format(end)
        ));
    }
}
