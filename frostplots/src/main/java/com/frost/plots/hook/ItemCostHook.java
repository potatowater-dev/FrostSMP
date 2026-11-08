package com.frost.plots.hook;

import com.frost.plots.FrostPlotsPlugin;
import com.frost.plots.model.UpgradeTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.logging.Level;

public final class ItemCostHook {

    public ItemCostHook(FrostPlotsPlugin plugin) {
    }

    public static ItemStack parse(FrostPlotsPlugin plugin, String snbt) {
        if (snbt == null || snbt.isBlank()) {
            return null;
        }
        try {
            ItemStack item = Bukkit.getItemFactory().createItemStack(snbt.trim());
            item.setAmount(1);
            return item;
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().log(Level.WARNING, "Invalid upgrade cost-item SNBT: " + snbt, exception);
            return null;
        }
    }

    public static String toSnbt(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        ItemStack clone = item.clone();
        clone.setAmount(1);
        String key = clone.getType().getKey().toString();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return key;
        }
        String components = meta.getAsComponentString();
        if (components == null || components.isBlank() || components.equals("[]") || components.equals("{}")) {
            return key;
        }
        if (components.startsWith("[") || components.startsWith("{")) {
            return key + components;
        }
        return key + "[" + components + "]";
    }

    public static String displayName(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "item";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component name = meta.displayName();
            if (name != null) {
                String plain = PlainTextComponentSerializer.plainText().serialize(name).trim();
                if (!plain.isEmpty()) {
                    return plain;
                }
            }
        }
        String key = item.getType().getKey().getKey();
        String[] parts = key.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    public String format(UpgradeTier tier) {
        if (tier == null || tier.isFree()) {
            return "Free";
        }
        String name = displayName(tier.costTemplate() == null ? null : tier.costTemplate());
        if (tier.costTemplate() == null && (tier.costItem() == null || tier.costItem().isBlank())) {
            name = "unconfigured item";
        }
        return tier.costAmount() + "x " + name;
    }

    public int count(Player player, ItemStack template) {
        if (player == null || template == null) {
            return 0;
        }
        int total = 0;
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (matches(template, stack)) {
                total += stack.getAmount();
            }
        }
        ItemStack offhand = inventory.getItemInOffHand();
        if (matches(template, offhand)) {
            total += offhand.getAmount();
        }
        return total;
    }

    public boolean has(Player player, UpgradeTier tier) {
        if (tier == null || tier.isFree()) {
            return true;
        }
        if (tier.costTemplate() == null) {
            return false;
        }
        return count(player, tier.costTemplate()) >= tier.costAmount();
    }

    public boolean take(Player player, UpgradeTier tier) {
        if (tier == null || tier.isFree()) {
            return true;
        }
        ItemStack template = tier.costTemplate();
        if (player == null || template == null || !has(player, tier)) {
            return false;
        }
        int remaining = tier.costAmount();
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            remaining = takeFromSlot(inventory, slot, template, remaining);
        }
        if (remaining > 0) {
            ItemStack offhand = inventory.getItemInOffHand();
            if (matches(template, offhand)) {
                int have = offhand.getAmount();
                int remove = Math.min(have, remaining);
                int left = have - remove;
                remaining -= remove;
                if (left <= 0) {
                    inventory.setItemInOffHand(null);
                } else {
                    offhand.setAmount(left);
                }
            }
        }
        return remaining <= 0;
    }

    private int takeFromSlot(PlayerInventory inventory, int slot, ItemStack template, int remaining) {
        ItemStack stack = inventory.getItem(slot);
        if (!matches(template, stack)) {
            return remaining;
        }
        int have = stack.getAmount();
        int remove = Math.min(have, remaining);
        int left = have - remove;
        if (left <= 0) {
            inventory.setItem(slot, null);
        } else {
            stack.setAmount(left);
        }
        return remaining - remove;
    }

    private boolean matches(ItemStack template, ItemStack stack) {
        return template != null && stack != null && !stack.getType().isAir() && template.isSimilar(stack);
    }
}
