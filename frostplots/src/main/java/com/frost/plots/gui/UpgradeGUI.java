package com.frost.plots.gui;

import com.frost.plots.config.PluginConfig;
import com.frost.plots.hook.ItemCostHook;
import com.frost.plots.manager.PlotManager;
import com.frost.plots.model.Plot;
import com.frost.plots.model.UpgradeTier;
import com.frost.plots.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class UpgradeGUI implements Listener {

    public static final int SLOT_CURRENT = 11;
    public static final int SLOT_NEXT = 13;
    public static final int SLOT_CONFIRM = 15;

    private final PluginConfig config;
    private final PlotManager plots;
    private final ItemCostHook costs;

    public UpgradeGUI(PluginConfig config, PlotManager plots, ItemCostHook costs) {
        this.config = config;
        this.plots = plots;
        this.costs = costs;
    }

    public void open(Player player) {
        Plot plot = plots.requireOwnerForManagement(player);
        if (plot == null) {
            return;
        }
        View view = new View(plot.getId());
        view.inventory = Bukkit.createInventory(view, 27, Component.text("Plot Upgrade").color(NamedTextColor.AQUA));
        populate(view.inventory, plot);
        player.openInventory(view.inventory);
    }

    private void populate(Inventory inventory, Plot plot) {
        inventory.clear();
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, Component.text(" ").decoration(TextDecoration.ITALIC, false));
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        UpgradeTier current = config.tier(plot.getTierIndex());
        inventory.setItem(SLOT_CURRENT, named(
                Material.GRASS_BLOCK,
                Component.text("Current tier " + (plot.getTierIndex() + 1)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                Component.text("Size: " + current.size() + "x" + current.size()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));

        if (!config.hasNextTier(plot.getTierIndex())) {
            inventory.setItem(SLOT_NEXT, named(
                    Material.BARRIER,
                    Component.text("Maximum tier").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                    Component.text("This plot cannot be expanded further.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            inventory.setItem(SLOT_CONFIRM, named(
                    Material.GRAY_DYE,
                    Component.text("No upgrade available").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            return;
        }

        UpgradeTier next = config.tier(plot.getTierIndex() + 1);
        String costText = costs.format(next);
        inventory.setItem(SLOT_NEXT, named(
                next.costTemplate() == null ? Material.GOLD_INGOT : next.costTemplate().getType(),
                Component.text("Next tier " + (plot.getTierIndex() + 2)).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false),
                Component.text("Size: " + next.size() + "x" + next.size()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Cost: " + costText).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));
        inventory.setItem(SLOT_CONFIRM, named(
                Material.LIME_CONCRETE,
                Component.text("Confirm upgrade").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                Component.text("Pays " + costText + " from your inventory.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof View view)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (event.getSlot() != SLOT_CONFIRM) {
            return;
        }
        confirm(player, view.plotId);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof View) {
            event.setCancelled(true);
        }
    }

    private void confirm(Player player, int plotId) {
        Plot plot = plots.byId(plotId);
        if (plot == null) {
            player.closeInventory();
            MessageUtil.send(player, config.prefixed("plot-missing"));
            return;
        }
        if (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("frostplots.admin")) {
            player.closeInventory();
            MessageUtil.send(player, config.prefixed("not-owner"));
            return;
        }
        if (!config.hasNextTier(plot.getTierIndex())) {
            MessageUtil.send(player, config.prefixed("max-tier"));
            return;
        }
        UpgradeTier next = config.tier(plot.getTierIndex() + 1);
        if (!next.isFree()) {
            if (next.costTemplate() == null) {
                MessageUtil.send(player, config.prefixed("cost-item-missing"));
                return;
            }
            if (!costs.has(player, next)) {
                MessageUtil.send(player, config.prefixed("cannot-afford", "cost", costs.format(next)));
                return;
            }
            if (!costs.take(player, next)) {
                MessageUtil.send(player, config.prefixed("upgrade-failed"));
                return;
            }
        }
        plot.setTierIndex(plot.getTierIndex() + 1);
        plots.expandPlatform(plot, next);
        plots.persist(plot);
        player.closeInventory();
        if (plot.getCenterLocation() != null) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.getWorld().spawnParticle(
                    Particle.TOTEM_OF_UNDYING,
                    plot.getSpawnLocation() == null ? player.getLocation() : plot.getSpawnLocation(),
                    50,
                    0.6,
                    0.8,
                    0.6,
                    0.2
            );
        }
        MessageUtil.send(player, config.prefixed(
                "upgraded",
                "tier", String.valueOf(plot.getTierIndex() + 1),
                "size", String.valueOf(next.size())
        ));
    }

    private ItemStack named(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (lore.length > 0) {
                meta.lore(List.of(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static final class View implements InventoryHolder {
        private final int plotId;
        private Inventory inventory;

        private View(int plotId) {
            this.plotId = plotId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
