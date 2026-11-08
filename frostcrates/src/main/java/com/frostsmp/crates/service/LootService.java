package com.frostsmp.crates.service;

import com.frostsmp.crates.config.PluginConfig;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.LootEntry;
import com.frostsmp.crates.util.MessageUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class LootService {

    private final PluginConfig config;

    public LootService(PluginConfig config) {
        this.config = config;
    }

    public LootEntry roll(CrateDefinition definition) {
        int total = definition.getTotalWeight();
        if (total <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (LootEntry entry : definition.getLoot()) {
            cumulative += entry.getWeight();
            if (roll < cumulative) {
                return entry;
            }
        }
        return definition.getLoot().get(definition.getLoot().size() - 1);
    }

    public boolean wouldOverflow(Player player, ItemStack item) {
        return !canFullyFit(player, item);
    }

    /**
     * True if the full stack can go into storage (or equip into an empty armor slot).
     * Accounts for stacking into existing similar stacks.
     */
    public boolean canFullyFit(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return true;
        }
        ItemStack stack = item.clone();
        if (isArmor(stack.getType()) && canEquipArmor(player.getInventory(), stack)) {
            return true;
        }

        int remaining = stack.getAmount();
        int maxStack = Math.max(1, stack.getMaxStackSize());
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (isEmpty(slot)) {
                remaining -= maxStack;
            } else if (slot.isSimilar(stack)) {
                remaining -= Math.max(0, slot.getMaxStackSize() - slot.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    public void giveReward(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (config.isWarnOnFullInventory() && wouldOverflow(player, item)) {
            MessageUtil.send(player, "<yellow>⚠ Your inventory is full — overflow will drop on the ground.");
        }

        ItemStack stack = item.clone();
        if (isArmor(stack.getType())) {
            if (tryEquipArmor(player.getInventory(), stack)) {
                return;
            }
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        dropOverflow(player, leftover);
    }

    private boolean canEquipArmor(PlayerInventory inventory, ItemStack stack) {
        String name = stack.getType().name();
        if (name.endsWith("_HELMET")) {
            return isEmpty(inventory.getHelmet());
        }
        if (name.endsWith("_CHESTPLATE")) {
            return isEmpty(inventory.getChestplate());
        }
        if (name.endsWith("_LEGGINGS")) {
            return isEmpty(inventory.getLeggings());
        }
        if (name.endsWith("_BOOTS")) {
            return isEmpty(inventory.getBoots());
        }
        return false;
    }

    private boolean tryEquipArmor(PlayerInventory inventory, ItemStack stack) {
        if (!canEquipArmor(inventory, stack)) {
            return false;
        }
        String name = stack.getType().name();
        if (name.endsWith("_HELMET")) {
            inventory.setHelmet(stack);
            return true;
        }
        if (name.endsWith("_CHESTPLATE")) {
            inventory.setChestplate(stack);
            return true;
        }
        if (name.endsWith("_LEGGINGS")) {
            inventory.setLeggings(stack);
            return true;
        }
        if (name.endsWith("_BOOTS")) {
            inventory.setBoots(stack);
            return true;
        }
        return false;
    }

    private boolean isArmor(org.bukkit.Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private void dropOverflow(Player player, Map<Integer, ItemStack> leftover) {
        if (leftover.isEmpty()) {
            return;
        }
        Location location = player.getLocation();
        for (ItemStack dropped : leftover.values()) {
            var entity = player.getWorld().dropItem(location.clone().add(0, 0.5, 0), dropped);
            entity.setVelocity(entity.getVelocity().zero());
        }
    }

    public void maybeBroadcast(Player player, CrateDefinition definition, LootEntry entry) {
        if (!definition.isBroadcastEnabled()) {
            return;
        }
        if (entry.getWeight() > definition.getBroadcastThresholdWeight()) {
            return;
        }
        String itemName = PlainTextComponentSerializer.plainText().serialize(entry.getItem().displayName());
        if (itemName.isBlank()) {
            itemName = entry.getItem().getType().name();
        }
        String message = config.getPrefix() + MessageUtil.format(
                config.getBroadcastFormat(),
                "player", player.getName(),
                "item", itemName,
                "crate", definition.getDisplayName()
        );
        Bukkit.getServer().sendMessage(MessageUtil.parse(message));
    }
}
