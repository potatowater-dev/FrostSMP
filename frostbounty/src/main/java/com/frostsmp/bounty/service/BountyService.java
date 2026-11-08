package com.frostsmp.bounty.service;

import com.frostsmp.bounty.FrostBountyPlugin;
import com.frostsmp.bounty.config.PluginConfig;
import com.frostsmp.bounty.gui.ListBountyGui;
import com.frostsmp.bounty.gui.PlaceBountyGui;
import com.frostsmp.bounty.gui.ViewBountyGui;
import com.frostsmp.bounty.model.Bounty;
import com.frostsmp.bounty.storage.BountyStorage;
import com.frostsmp.bounty.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BountyService {

    private final FrostBountyPlugin plugin;
    private final PluginConfig config;
    private final BountyStorage storage;

    public BountyService(FrostBountyPlugin plugin, PluginConfig config, BountyStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
    }

    public void openPlaceGui(Player placer, Player target) {
        if (placer.getUniqueId().equals(target.getUniqueId())) {
            MessageUtil.send(placer, config.getCannotBountySelf());
            return;
        }
        PlaceBountyGui gui = new PlaceBountyGui(config, target.getUniqueId(), target.getName());
        placer.openInventory(gui.getInventory());
    }

    public void openListGui(Player viewer) {
        List<Bounty> bounties = storage.snapshot();
        if (bounties.isEmpty()) {
            MessageUtil.send(viewer, config.getListEmpty());
            return;
        }
        viewer.openInventory(new ListBountyGui(config, bounties).getInventory());
    }

    public void openViewGui(Player viewer, Bounty bounty) {
        viewer.openInventory(new ViewBountyGui(config, bounty).getInventory());
    }

    public boolean confirmPlace(Player placer, PlaceBountyGui gui) {
        List<ItemStack> rewards = gui.collectRewards();
        if (rewards.isEmpty()) {
            MessageUtil.send(placer, config.getPlaceEmpty());
            return false;
        }

        int maxSlots = config.getMaxRewardSlots();
        Bounty existing = storage.findMergeable(gui.getTargetId(), rewards.size(), maxSlots);
        if (existing != null) {
            existing.setTargetName(gui.getTargetName());
            existing.addContribution(placer.getUniqueId(), placer.getName(), rewards);
            storage.touch();
        } else {
            Bounty bounty = new Bounty(
                    UUID.randomUUID(),
                    gui.getTargetId(),
                    gui.getTargetName(),
                    placer.getUniqueId(),
                    placer.getName(),
                    rewards,
                    System.currentTimeMillis()
            );
            storage.add(bounty);
        }

        gui.clearRewards();
        gui.setCompleted(true);
        MessageUtil.send(placer, config.getPlaceConfirmed().replace("{player}", gui.getTargetName()));
        Bukkit.broadcast(MessageUtil.parse(config.getPlaceBroadcast()
                .replace("{placer}", placer.getName())
                .replace("{player}", gui.getTargetName())
                .replace("{count}", String.valueOf(rewards.size()))));
        return true;
    }

    public void cancelPlace(Player placer, PlaceBountyGui gui) {
        if (gui.isCompleted()) {
            return;
        }
        returnItems(placer, gui.collectRewards());
        gui.clearRewards();
        gui.setCompleted(true);
        MessageUtil.send(placer, config.getPlaceCancelled());
    }

    public void handleClosePlace(Player placer, PlaceBountyGui gui) {
        if (gui.isCompleted()) {
            return;
        }
        returnItems(placer, gui.collectRewards());
        gui.clearRewards();
        gui.setCompleted(true);
        MessageUtil.send(placer, config.getPlaceCancelled());
    }

    public void claimOnKill(Player victim, Player killer) {
        List<Bounty> removed = storage.removeForTarget(victim.getUniqueId());
        if (removed.isEmpty()) {
            return;
        }
        if (killer == null) {
            // Return rewards to each contributor if no killer
            Map<UUID, List<ItemStack>> byPlacer = new HashMap<>();
            for (Bounty bounty : removed) {
                for (Map.Entry<UUID, List<ItemStack>> entry : bounty.rewardsByPlacer().entrySet()) {
                    byPlacer.computeIfAbsent(entry.getKey(), id -> new ArrayList<>()).addAll(entry.getValue());
                }
            }
            for (Map.Entry<UUID, List<ItemStack>> entry : byPlacer.entrySet()) {
                Player placer = Bukkit.getPlayer(entry.getKey());
                if (placer != null) {
                    returnItems(placer, entry.getValue());
                } else {
                    // Drop at victim location as last resort so items aren't deleted
                    for (ItemStack item : entry.getValue()) {
                        victim.getWorld().dropItemNaturally(victim.getLocation(), item);
                    }
                }
            }
            Bukkit.broadcast(MessageUtil.parse(config.getTargetKilledNoKiller().replace("{player}", victim.getName())));
            return;
        }

        List<ItemStack> allRewards = new ArrayList<>();
        for (Bounty bounty : removed) {
            allRewards.addAll(bounty.getRewards());
        }
        returnItems(killer, allRewards);
        MessageUtil.send(killer, config.getClaimed().replace("{player}", victim.getName()));
        Bukkit.broadcast(MessageUtil.parse(config.getClaimedBroadcast()
                .replace("{killer}", killer.getName())
                .replace("{player}", victim.getName())));
    }

    private void returnItems(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    public PluginConfig getConfig() {
        return config;
    }

    public FrostBountyPlugin getPlugin() {
        return plugin;
    }
}
