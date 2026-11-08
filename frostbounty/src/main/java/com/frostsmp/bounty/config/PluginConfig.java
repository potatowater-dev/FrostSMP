package com.frostsmp.bounty.config;

import com.frostsmp.bounty.FrostBountyPlugin;

public final class PluginConfig {

    private final FrostBountyPlugin plugin;

    private int maxRewardSlots;
    private String noPermission;
    private String playersOnly;
    private String usage;
    private String reloaded;
    private String playerNotFound;
    private String cannotBountySelf;
    private String placeTitle;
    private String placeEmpty;
    private String placeConfirmed;
    private String placeBroadcast;
    private String placeCancelled;
    private String listTitle;
    private String listEmpty;
    private String listHeadName;
    private String listHeadLorePlacer;
    private String listHeadLoreItems;
    private String listHeadLoreEntry;
    private String viewTitle;
    private String claimed;
    private String claimedBroadcast;
    private String targetKilledNoKiller;

    public PluginConfig(FrostBountyPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var config = plugin.getConfig();

        maxRewardSlots = Math.min(45, Math.max(1, config.getInt("max-reward-slots", 45)));

        noPermission = config.getString("messages.no-permission", "<red>No permission.</red>");
        playersOnly = config.getString("messages.players-only", "<red>Players only.</red>");
        usage = config.getString("messages.usage", "<gray>Usage: /bounty <place <player>|list|reload></gray>");
        reloaded = config.getString("messages.reloaded", "<green>FrostBounty reloaded.</green>");
        playerNotFound = config.getString("messages.player-not-found", "<red>Player not found.</red>");
        cannotBountySelf = config.getString("messages.cannot-bounty-self", "<red>You cannot place a bounty on yourself.</red>");
        placeTitle = config.getString("messages.place-title", "<gold>Bounty:</gold> <white>{player}</white>");
        placeEmpty = config.getString("messages.place-empty", "<red>Put at least one item in the bounty.</red>");
        placeConfirmed = config.getString("messages.place-confirmed", "<green>Bounty placed on</green> <white>{player}</white><green>.</green>");
        placeBroadcast = config.getString(
                "messages.place-broadcast",
                "<gold>Bounty:</gold> <white>{placer}</white> <gray>placed a bounty on</gray> <red>{player}</red> <gray>({count} stacks).</gray>"
        );
        placeCancelled = config.getString("messages.place-cancelled", "<yellow>Bounty cancelled — items returned.</yellow>");
        listTitle = config.getString("messages.list-title", "<gold>Active Bounties</gold>");
        listEmpty = config.getString("messages.list-empty", "<gray>There are no active bounties.</gray>");
        listHeadName = config.getString("messages.list-head-name", "<red>{player}</red>");
        listHeadLorePlacer = config.getString("messages.list-head-lore-placer", "<gray>Placed by:</gray> <white>{placer}</white>");
        listHeadLoreItems = config.getString("messages.list-head-lore-items", "<gray>Rewards:</gray> <white>{count}</white>");
        listHeadLoreEntry = config.getString("messages.list-head-lore-entry", "<dark_gray>•</dark_gray> <white>{amount}x</white> <gray>{item}</gray>");
        viewTitle = config.getString("messages.view-title", "<gold>Bounty loot:</gold> <white>{player}</white>");
        claimed = config.getString("messages.claimed", "<green>You claimed the bounty on</green> <white>{player}</white><green>!</green>");
        claimedBroadcast = config.getString("messages.claimed-broadcast", "<gold>{killer}</gold> <gray>claimed the bounty on</gray> <white>{player}</white><gray>.</gray>");
        targetKilledNoKiller = config.getString("messages.target-killed-no-killer", "<yellow>Bounty on</yellow> <white>{player}</white> <yellow>expired.</yellow>");
    }

    public int getMaxRewardSlots() {
        return maxRewardSlots;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getPlayersOnly() {
        return playersOnly;
    }

    public String getUsage() {
        return usage;
    }

    public String getReloaded() {
        return reloaded;
    }

    public String getPlayerNotFound() {
        return playerNotFound;
    }

    public String getCannotBountySelf() {
        return cannotBountySelf;
    }

    public String getPlaceTitle() {
        return placeTitle;
    }

    public String getPlaceEmpty() {
        return placeEmpty;
    }

    public String getPlaceConfirmed() {
        return placeConfirmed;
    }

    public String getPlaceBroadcast() {
        return placeBroadcast;
    }

    public String getPlaceCancelled() {
        return placeCancelled;
    }

    public String getListTitle() {
        return listTitle;
    }

    public String getListEmpty() {
        return listEmpty;
    }

    public String getListHeadName() {
        return listHeadName;
    }

    public String getListHeadLorePlacer() {
        return listHeadLorePlacer;
    }

    public String getListHeadLoreItems() {
        return listHeadLoreItems;
    }

    public String getListHeadLoreEntry() {
        return listHeadLoreEntry;
    }

    public String getViewTitle() {
        return viewTitle;
    }

    public String getClaimed() {
        return claimed;
    }

    public String getClaimedBroadcast() {
        return claimedBroadcast;
    }

    public String getTargetKilledNoKiller() {
        return targetKilledNoKiller;
    }
}
