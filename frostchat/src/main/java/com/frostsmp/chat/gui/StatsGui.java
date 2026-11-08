package com.frostsmp.chat.gui;

import com.frostsmp.chat.service.StatsService;
import com.frostsmp.chat.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class StatsGui {

    public static final class StatsViewHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final int[] BORDER = {
            0, 1, 2, 3, 5, 6, 7, 8,
            9, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26
    };
    private static final int[] ACCENTS = {0, 8, 18, 26};

    private StatsGui() {
    }

    public static Inventory create(JavaPlugin plugin, OfflinePlayer target, StatsService.PlayerStats stats) {
        String title = MessageUtil.format(
                plugin.getConfig().getString("stats.title", "<aqua>{player}</aqua><dark_gray>'s Stats</dark_gray>"),
                "player", stats.name()
        );
        Inventory inventory = Bukkit.createInventory(new StatsViewHolder(), 27, MessageUtil.parse(title));

        ItemStack border = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        ItemStack accent = pane(Material.CYAN_STAINED_GLASS_PANE, " ");
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, " ");

        for (int slot : BORDER) {
            inventory.setItem(slot, border);
        }
        for (int slot : ACCENTS) {
            inventory.setItem(slot, accent);
        }
        inventory.setItem(10, filler);
        inventory.setItem(16, filler);

        inventory.setItem(4, playerHead(plugin, target, stats));
        inventory.setItem(11, wealthItem(plugin, stats));
        inventory.setItem(12, killsItem(plugin, stats));
        inventory.setItem(13, streakItem(plugin, stats));
        inventory.setItem(14, deathsItem(plugin, stats));
        inventory.setItem(15, playtimeItem(plugin, stats));

        return inventory;
    }

    public static boolean isStatsView(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof StatsViewHolder;
    }

    private static ItemStack playerHead(JavaPlugin plugin, OfflinePlayer target, StatsService.PlayerStats stats) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.displayName(MessageUtil.parse(MessageUtil.format(
                    plugin.getConfig().getString("stats.head-name", "<aqua><bold>{player}</bold></aqua>"),
                    "player", stats.name()
            )));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse(plugin.getConfig().getString(
                    "stats.head-lore-1",
                    "<dark_gray>FrostSMP profile</dark_gray>"
            )));
            lore.add(MessageUtil.parse(MessageUtil.format(
                    plugin.getConfig().getString("stats.head-lore-kd", "<gray>K/D:</gray> <white>{kd}</white>"),
                    "kd", StatsService.formatRatio(stats.kdRatio())
            )));
            meta.lore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private static ItemStack wealthItem(JavaPlugin plugin, StatsService.PlayerStats stats) {
        String value = stats.walletAvailable()
                ? StatsService.formatNumber(stats.wealth())
                : plugin.getConfig().getString("stats.unavailable", "N/A");
        return icon(
                Material.NETHER_STAR,
                plugin.getConfig().getString("stats.wealth-name", "<gold><bold>Wealth</bold></gold>"),
                List.of(
                        MessageUtil.format(
                                plugin.getConfig().getString("stats.wealth-lore", "<gray>Balance:</gray> <white>{value}</white>"),
                                "value", value
                        ),
                        plugin.getConfig().getString("stats.wealth-hint", "<dark_gray>FrostWallet total</dark_gray>")
                )
        );
    }

    private static ItemStack killsItem(JavaPlugin plugin, StatsService.PlayerStats stats) {
        return icon(
                Material.DIAMOND_SWORD,
                plugin.getConfig().getString("stats.kills-name", "<aqua><bold>Kills</bold></aqua>"),
                List.of(MessageUtil.format(
                        plugin.getConfig().getString("stats.kills-lore", "<gray>Total:</gray> <white>{value}</white>"),
                        "value", StatsService.formatNumber(stats.kills())
                ))
        );
    }

    private static ItemStack streakItem(JavaPlugin plugin, StatsService.PlayerStats stats) {
        String current = stats.combatAvailable()
                ? StatsService.formatNumber(stats.killstreak())
                : plugin.getConfig().getString("stats.unavailable", "N/A");
        String best = stats.combatAvailable()
                ? StatsService.formatNumber(stats.bestKillstreak())
                : plugin.getConfig().getString("stats.unavailable", "N/A");
        return icon(
                Material.BLAZE_POWDER,
                plugin.getConfig().getString("stats.streak-name", "<red><bold>Killstreak</bold></red>"),
                List.of(
                        MessageUtil.format(
                                plugin.getConfig().getString("stats.streak-lore-current", "<gray>Current:</gray> <white>{value}</white>"),
                                "value", current
                        ),
                        MessageUtil.format(
                                plugin.getConfig().getString("stats.streak-lore-best", "<gray>Best:</gray> <white>{value}</white>"),
                                "value", best
                        )
                )
        );
    }

    private static ItemStack deathsItem(JavaPlugin plugin, StatsService.PlayerStats stats) {
        return icon(
                Material.SKELETON_SKULL,
                plugin.getConfig().getString("stats.deaths-name", "<dark_gray><bold>Deaths</bold></dark_gray>"),
                List.of(MessageUtil.format(
                        plugin.getConfig().getString("stats.deaths-lore", "<gray>Total:</gray> <white>{value}</white>"),
                        "value", StatsService.formatNumber(stats.deaths())
                ))
        );
    }

    private static ItemStack playtimeItem(JavaPlugin plugin, StatsService.PlayerStats stats) {
        return icon(
                Material.CLOCK,
                plugin.getConfig().getString("stats.playtime-name", "<green><bold>Playtime</bold></green>"),
                List.of(MessageUtil.format(
                        plugin.getConfig().getString("stats.playtime-lore", "<gray>Time:</gray> <white>{value}</white>"),
                        "value", stats.formattedPlaytime()
                ))
        );
    }

    private static ItemStack icon(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MessageUtil.parse(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
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
}
