package com.frostsmp.colosseum.gui;

import com.frostsmp.colosseum.config.PluginConfig;
import com.frostsmp.colosseum.service.RewardService;
import com.frostsmp.colosseum.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RewardGui implements Listener {

    private static final int SLOT = 13;

    private final PluginConfig config;
    private final RewardService rewardService;

    public RewardGui(PluginConfig config, RewardService rewardService) {
        this.config = config;
        this.rewardService = rewardService;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(), 27, MessageUtil.parse("<gold>Set Colosseum Reward</gold>"));
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i != SLOT) {
                inventory.setItem(i, filler);
            }
        }
        ItemStack current = rewardService.getReward();
        if (current != null) {
            inventory.setItem(SLOT, current);
        }
        inventory.setItem(22, pane(Material.LIME_STAINED_GLASS_PANE, "<green>Close to save</green>"));
        inventory.setItem(16, pane(Material.BARRIER, "<red>Clear reward</red>"));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw == SLOT) {
            // Allow placing/taking the reward item
            return;
        }
        if (raw >= 0 && raw < event.getInventory().getSize()) {
            event.setCancelled(true);
            if (raw == 16) {
                event.getInventory().setItem(SLOT, null);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        ItemStack item = event.getInventory().getItem(SLOT);
        if (item == null || item.getType().isAir()) {
            rewardService.clearReward();
            MessageUtil.send(player, config.msg("reward-cleared"));
        } else {
            rewardService.setReward(item);
            MessageUtil.send(player, config.msg("reward-saved"));
        }
    }

    private static ItemStack pane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
