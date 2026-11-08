package com.frostsmp.bounty.gui;

import com.frostsmp.bounty.config.PluginConfig;
import com.frostsmp.bounty.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlaceBountyGui implements BountyGuiHolder {

    public static final int SIZE = 54;
    public static final int SLOT_CONFIRM = 49;
    public static final int SLOT_CANCEL = 53;
    public static final int REWARD_SLOTS = 45;

    private final Inventory inventory;
    private final UUID targetId;
    private final String targetName;
    private boolean completed;

    public PlaceBountyGui(PluginConfig config, UUID targetId, String targetName) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.inventory = Bukkit.createInventory(
                this,
                SIZE,
                MessageUtil.parse(config.getPlaceTitle().replace("{player}", targetName))
        );
        drawControls(config);
    }

    private void drawControls(PluginConfig config) {
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(MessageUtil.parse("<green>Confirm bounty</green>"));
            confirmMeta.lore(List.of(MessageUtil.parse("<gray>Items stay taken until claimed.</gray>")));
            confirm.setItemMeta(confirmMeta);
        }
        inventory.setItem(SLOT_CONFIRM, confirm);

        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(MessageUtil.parse("<red>Cancel</red>"));
            cancelMeta.lore(List.of(MessageUtil.parse("<gray>Return items to you.</gray>")));
            cancel.setItemMeta(cancelMeta);
        }
        inventory.setItem(SLOT_CANCEL, cancel);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(MessageUtil.parse(" "));
            filler.setItemMeta(fillerMeta);
        }
        for (int i = REWARD_SLOTS; i < SIZE; i++) {
            if (i == SLOT_CONFIRM || i == SLOT_CANCEL) {
                continue;
            }
            inventory.setItem(i, filler);
        }
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isRewardSlot(int slot) {
        return slot >= 0 && slot < REWARD_SLOTS;
    }

    public boolean isControlSlot(int slot) {
        return slot >= REWARD_SLOTS;
    }

    public List<ItemStack> collectRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        for (int i = 0; i < REWARD_SLOTS; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                rewards.add(item.clone());
            }
        }
        return rewards;
    }

    public void clearRewards() {
        for (int i = 0; i < REWARD_SLOTS; i++) {
            inventory.setItem(i, null);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
