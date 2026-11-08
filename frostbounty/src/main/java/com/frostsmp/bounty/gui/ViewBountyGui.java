package com.frostsmp.bounty.gui;

import com.frostsmp.bounty.config.PluginConfig;
import com.frostsmp.bounty.model.Bounty;
import com.frostsmp.bounty.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ViewBountyGui implements BountyGuiHolder {

    private final Inventory inventory;

    public ViewBountyGui(PluginConfig config, Bounty bounty) {
        this.inventory = Bukkit.createInventory(
                this,
                54,
                MessageUtil.parse(config.getViewTitle().replace("{player}", bounty.getTargetName()))
        );
        List<ItemStack> rewards = bounty.getRewards();
        int slot = 0;
        for (ItemStack reward : rewards) {
            if (slot >= 54) {
                break;
            }
            inventory.setItem(slot++, reward);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
