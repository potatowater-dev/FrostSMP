package com.frost.plots.listener;

import com.frost.plots.config.PluginConfig;
import com.frost.plots.hook.WorldGuardHook;
import com.frost.plots.manager.PlotManager;
import com.frost.plots.model.Plot;
import com.frost.plots.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class PlotProtectionListener implements Listener {

    private final PluginConfig config;
    private final PlotManager plots;
    private final WorldGuardHook worldGuard;

    public PlotProtectionListener(PluginConfig config, PlotManager plots, WorldGuardHook worldGuard) {
        this.config = config;
        this.plots = plots;
        this.worldGuard = worldGuard;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Plot plot = plots.at(block.getLocation());
        if (plot == null) {
            return;
        }
        if (isBaseLayer(plot, block)) {
            event.setCancelled(true);
            MessageUtil.send(player, config.prefixed("base-protected"));
            return;
        }
        if (bypass(player) || worldGuard.hasBuildAccess(player, plot)) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlace(BlockPlaceEvent event) {
        applyPlace(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMultiPlace(BlockMultiPlaceEvent event) {
        applyPlace(event);
    }

    private void applyPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Plot plot = plots.at(block.getLocation());
        if (plot == null) {
            return;
        }
        if (isBaseLayer(plot, block)) {
            event.setCancelled(true);
            return;
        }
        if (bypass(player) || worldGuard.hasBuildAccess(player, plot)) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Plot plot = plots.at(block.getLocation());
        if (plot == null) {
            return;
        }
        Player player = event.getPlayer();
        if (isBaseLayer(plot, block) && modifiesBase(event.getItem())) {
            event.setCancelled(true);
            MessageUtil.send(player, config.prefixed("base-protected"));
            return;
        }
        if (isContainer(block)) {
            if (bypass(player) || worldGuard.hasContainerAccess(player, plot)) {
                event.setCancelled(false);
                return;
            }
            event.setCancelled(true);
            return;
        }
        if (bypass(player) || worldGuard.hasBuildAccess(player, plot)) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);
    }

    /**
     * WorldGuard often cancels chest/shulker opens on InventoryOpenEvent after interact.
     * Re-allow for plot owners/members.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Location location = inventoryBlockLocation(event.getInventory().getHolder());
        if (location == null) {
            return;
        }
        Plot plot = plots.at(location);
        if (plot == null) {
            return;
        }
        if (bypass(player) || worldGuard.hasContainerAccess(player, plot)) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);
    }

    private Location inventoryBlockLocation(InventoryHolder holder) {
        if (holder instanceof BlockInventoryHolder blockHolder) {
            return blockHolder.getBlock().getLocation();
        }
        if (holder instanceof BlockState state) {
            return state.getLocation();
        }
        if (holder instanceof DoubleChest doubleChest) {
            return doubleChest.getLocation();
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Plot plot = plots.at(block.getLocation());
        if (plot == null) {
            return;
        }
        if (isBaseLayer(plot, block)) {
            event.setCancelled(true);
            if (player != null) {
                MessageUtil.send(player, config.prefixed("base-protected"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Plot plot = plots.at(event.getBlock().getLocation());
        if (plot != null && isBaseLayer(plot, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlock();
        Plot plot = plots.at(block.getLocation());
        if (plot == null) {
            return;
        }
        if (isBaseLayer(plot, block)) {
            event.setCancelled(true);
            return;
        }
        if (bypass(event.getPlayer()) || worldGuard.hasBuildAccess(event.getPlayer(), plot)) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block block = event.getBlock();
        Plot plot = plots.at(block.getLocation());
        if (plot == null) {
            return;
        }
        if (isBaseLayer(plot, block)) {
            event.setCancelled(true);
            return;
        }
        if (bypass(event.getPlayer()) || worldGuard.hasBuildAccess(event.getPlayer(), plot)) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Plot plot = plots.at(event.getRightClicked().getLocation());
        if (plot == null) {
            return;
        }
        if (bypass(event.getPlayer()) || worldGuard.hasBuildAccess(event.getPlayer(), plot)) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) {
            return;
        }
        Plot plot = plots.at(event.getEntity().getLocation());
        if (plot == null || bypass(player)) {
            return;
        }
        if (!worldGuard.hasBuildAccess(player, plot)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = playerDamager(event.getDamager());
        if (attacker == null) {
            return;
        }
        Plot victimPlot = plots.at(event.getEntity().getLocation());
        Plot attackerPlot = plots.at(attacker.getLocation());
        Plot plot = victimPlot != null ? victimPlot : attackerPlot;
        if (plot == null) {
            return;
        }
        // No PVP inside plots (either combatant on a plot).
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
            return;
        }
        if (bypass(attacker)) {
            return;
        }
        if (!worldGuard.hasBuildAccess(attacker, plot)) {
            event.setCancelled(true);
        }
    }

    private Player playerDamager(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Plot plot = plots.at(block.getLocation());
            return plot != null && isBaseLayer(plot, block);
        });
    }

    private boolean modifiesBase(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Material type = item.getType();
        String name = type.name();
        return name.endsWith("_HOE")
                || name.endsWith("_SHOVEL")
                || type == Material.BONE_MEAL
                || type == Material.FLINT_AND_STEEL
                || type == Material.FIRE_CHARGE;
    }

    private boolean isContainer(Block block) {
        Material type = block.getType();
        String name = type.name();
        return name.contains("SHULKER_BOX")
                || name.contains("CHEST")
                || type == Material.BARREL
                || type == Material.HOPPER
                || type == Material.DROPPER
                || type == Material.DISPENSER
                || type == Material.FURNACE
                || type == Material.BLAST_FURNACE
                || type == Material.SMOKER
                || type == Material.BREWING_STAND
                || type == Material.CRAFTER
                || name.contains("SHULKER");
    }

    private boolean isBaseLayer(Plot plot, Block block) {
        return block.getY() == plot.getBaseY();
    }

    private boolean bypass(Player player) {
        return player.hasPermission("frostplots.admin");
    }
}
