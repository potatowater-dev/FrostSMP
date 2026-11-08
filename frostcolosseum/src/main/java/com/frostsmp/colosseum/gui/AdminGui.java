package com.frostsmp.colosseum.gui;

import com.frostsmp.colosseum.config.PluginConfig;
import com.frostsmp.colosseum.service.MatchScheduler;
import com.frostsmp.colosseum.service.MatchService;
import com.frostsmp.colosseum.service.QueueService;
import com.frostsmp.colosseum.service.RewardService;
import com.frostsmp.colosseum.service.WandService;
import com.frostsmp.colosseum.storage.RegionStorage;
import com.frostsmp.colosseum.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class AdminGui implements Listener {

    private final PluginConfig config;
    private final RegionStorage regionStorage;
    private final RewardService rewardService;
    private final MatchService matchService;
    private final MatchScheduler scheduler;
    private final QueueService queueService;
    private final WandService wandService;
    private final RewardGui rewardGui;

    public AdminGui(
            PluginConfig config,
            RegionStorage regionStorage,
            RewardService rewardService,
            MatchService matchService,
            MatchScheduler scheduler,
            QueueService queueService,
            WandService wandService,
            RewardGui rewardGui
    ) {
        this.config = config;
        this.regionStorage = regionStorage;
        this.rewardService = rewardService;
        this.matchService = matchService;
        this.scheduler = scheduler;
        this.queueService = queueService;
        this.wandService = wandService;
        this.rewardGui = rewardGui;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(), 27, MessageUtil.parse("<gold>Colosseum Admin</gold>"));

        inventory.setItem(10, statusItem(
                "platform_red",
                Material.RED_CONCRETE,
                regionStorage.isRegionSet("platform_red")
        ));
        inventory.setItem(11, statusItem(
                "platform_blue",
                Material.LIGHT_BLUE_CONCRETE,
                regionStorage.isRegionSet("platform_blue")
        ));
        inventory.setItem(12, statusItem(
                "arena",
                Material.IRON_BLOCK,
                regionStorage.isRegionSet("arena")
        ));
        inventory.setItem(13, statusItem(
                "spectate",
                Material.GLASS,
                regionStorage.isRegionSet("spectate")
        ));
        inventory.setItem(14, statusItem(
                "spawn_red",
                Material.RED_BED,
                regionStorage.isPointSet("spawn_red")
        ));
        inventory.setItem(15, statusItem(
                "spawn_blue",
                Material.BLUE_BED,
                regionStorage.isPointSet("spawn_blue")
        ));
        inventory.setItem(16, statusItem(
                "exit",
                Material.OAK_DOOR,
                regionStorage.isPointSet("exit")
        ));

        inventory.setItem(4, infoItem());
        inventory.setItem(19, actionItem(Material.BLAZE_ROD, "<gold>Give Wand</gold>", "Click to receive the selection wand"));
        inventory.setItem(21, actionItem(Material.CHEST, "<yellow>Set Reward</yellow>",
                rewardService.hasReward() ? "Reward configured" : "No reward set"));
        inventory.setItem(23, actionItem(Material.EMERALD, "<green>Force Start</green>", "Start a match now if queues are ready"));
        inventory.setItem(25, actionItem(Material.REDSTONE, "<red>Force End</red>", "End the current match and reset"));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) {
            return;
        }
        int slot = event.getRawSlot();
        switch (slot) {
            case 19 -> {
                player.closeInventory();
                wandService.giveWand(player);
            }
            case 21 -> {
                player.closeInventory();
                rewardGui.open(player);
            }
            case 23 -> {
                player.closeInventory();
                if (!regionStorage.isFullyConfigured()) {
                    MessageUtil.send(player, MessageUtil.format(
                            config.msg("incomplete-setup"),
                            "missing", String.join(", ", regionStorage.missingSetup())
                    ));
                } else if (matchService.tryStart(true)) {
                    MessageUtil.send(player, config.msg("force-start"));
                } else {
                    MessageUtil.send(player, config.msg("skip-round"));
                }
            }
            case 25 -> {
                player.closeInventory();
                matchService.forceEnd(true);
            }
            default -> {
            }
        }
    }

    private ItemStack infoItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<aqua>Match Info</aqua>"));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse("<gray>State: <white>" + matchService.getState().name() + "</white></gray>"));
            lore.add(MessageUtil.parse("<gray>Next: <white>" + scheduler.formatCountdown() + "</white></gray>"));
            lore.add(MessageUtil.parse("<gray>Queue: <red>R" + queueService.getRedCount()
                    + "</red><gray>/</gray><aqua>B" + queueService.getBlueCount() + "</aqua></gray>"));
            lore.add(MessageUtil.parse("<gray>Interval: <white>" + config.getIntervalMinutes() + "m</white></gray>"));
            lore.add(MessageUtil.parse("<gray>Loot: <white>" + config.getLootSeconds() + "s</white></gray>"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack statusItem(String name, Material material, boolean set) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse((set ? "<green>" : "<red>") + name + "</" + (set ? "green>" : "red>")));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse(set ? "<green>Configured</green>" : "<red>Missing</red>"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack actionItem(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse("<gray>" + loreLine + "</gray>"));
            meta.lore(lore);
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
