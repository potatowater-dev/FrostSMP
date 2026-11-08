package com.frostsmp.combat.item;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class KillToken {

    private static final String ITEM =
            "minecraft:nether_star[custom_name={\"text\":\"\",\"extra\":["
                    + "{\"text\":\"K\",\"bold\":true,\"italic\":false,\"color\":\"#c77dff\"},"
                    + "{\"text\":\"i\",\"bold\":true,\"italic\":false,\"color\":\"#cd85fa\"},"
                    + "{\"text\":\"l\",\"bold\":true,\"italic\":false,\"color\":\"#d391f5\"},"
                    + "{\"text\":\"l\",\"bold\":true,\"italic\":false,\"color\":\"#da9df1\"},"
                    + "{\"text\":\" \",\"bold\":true,\"italic\":false,\"color\":\"#e0aaec\"},"
                    + "{\"text\":\"T\",\"bold\":true,\"italic\":false,\"color\":\"#e6b6e7\"},"
                    + "{\"text\":\"o\",\"bold\":true,\"italic\":false,\"color\":\"#edc2e2\"},"
                    + "{\"text\":\"k\",\"bold\":true,\"italic\":false,\"color\":\"#f3ceDD\"},"
                    + "{\"text\":\"e\",\"bold\":true,\"italic\":false,\"color\":\"#f9dbd8\"},"
                    + "{\"text\":\"n\",\"bold\":true,\"italic\":false,\"color\":\"#ffd6ff\"}"
                    + "]},custom_data={coin:1b}]";

    private static ItemStack template;

    private KillToken() {
    }

    public static ItemStack create() {
        if (template == null) {
            template = Bukkit.getItemFactory().createItemStack(ITEM);
            template.setAmount(1);
        }
        return template.clone();
    }

    public static void reward(Player killer, Location dropAt) {
        reward(killer, dropAt, 1);
    }

    public static void reward(Player killer, Location dropAt, int amount) {
        if (amount <= 0) {
            return;
        }
        int remaining = amount;
        while (remaining > 0) {
            ItemStack token = create();
            int stack = Math.min(remaining, token.getMaxStackSize());
            token.setAmount(stack);
            remaining -= stack;
            if (killer != null && killer.isOnline()) {
                Map<Integer, ItemStack> leftover = killer.getInventory().addItem(token);
                if (!leftover.isEmpty()) {
                    Location loc = dropAt != null ? dropAt : killer.getLocation();
                    drop(loc, leftover.values());
                }
            } else {
                drop(dropAt, java.util.List.of(token));
            }
        }
    }

    private static void drop(Location loc, Iterable<ItemStack> stacks) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            loc.getWorld().dropItemNaturally(loc, stack);
        }
    }
}
