package com.frostsmp.alt.service;

import com.frostsmp.alt.FrostAltPlugin;
import com.frostsmp.alt.config.PluginConfig;
import com.frostsmp.alt.model.AltCase;
import com.frostsmp.alt.model.CaseStatus;
import com.frostsmp.alt.model.PlayerStatus;
import com.frostsmp.alt.storage.CaseStorage;
import com.frostsmp.alt.storage.PlayerStateStorage;
import com.frostsmp.alt.util.MessageUtil;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class EnforcementService {

    private final FrostAltPlugin plugin;
    private final PluginConfig config;
    private final CaseStorage caseStorage;
    private final PlayerStateStorage playerStateStorage;
    private final PlaytimeService playtimeService;

    public EnforcementService(
            FrostAltPlugin plugin,
            PluginConfig config,
            CaseStorage caseStorage,
            PlayerStateStorage playerStateStorage,
            PlaytimeService playtimeService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.caseStorage = caseStorage;
        this.playerStateStorage = playerStateStorage;
        this.playtimeService = playtimeService;
    }

    public void handleDetection(String ip, Player flagged, List<Player> candidates) {
        if (caseStorage.findOpenCaseForPlayer(flagged.getUniqueId()) != null) {
            return;
        }

        AltCase altCase = caseStorage.createCase(ip, flagged.getUniqueId(), flagged.getName());
        for (Player candidate : candidates) {
            if (!candidate.getUniqueId().equals(flagged.getUniqueId())) {
                altCase.getLinkedAccountIds().add(candidate.getUniqueId());
            }
            altCase.getPlaytimeMinutes().put(candidate.getName(), playtimeService.getPlaytimeMinutes(candidate));
        }
        altCase.getPlaytimeMinutes().putIfAbsent(flagged.getName(), playtimeService.getPlaytimeMinutes(flagged));

        saveInventorySnapshot(flagged, altCase);
        caseStorage.save();

        PlayerStateStorage.PlayerRecord record = playerStateStorage.getOrCreate(flagged.getUniqueId());
        record.setStatus(PlayerStatus.APPEAL_REQUIRED);
        record.setOpenCaseId(altCase.getId());
        record.setLastKnownName(flagged.getName());
        playerStateStorage.save();

        for (Player candidate : candidates) {
            if (!candidate.getUniqueId().equals(flagged.getUniqueId())) {
                MessageUtil.send(candidate, config.getMainNotifyMessage());
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (flagged.isOnline()) {
                    flagged.kick(MessageUtil.parse(config.getKickMessage()));
                }
            }
        }.runTask(plugin);
    }

    public void applyAppealMode(Player player) {
        player.teleport(config.getAppealSpawn());
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.showTitle(net.kyori.adventure.title.Title.title(
                MessageUtil.parse(config.getAppealTitle()),
                MessageUtil.parse(config.getAppealSubtitle())
        ));
        MessageUtil.send(player, config.getAppealPromptMessage());
    }

    public void restorePlayer(Player player, AltCase altCase) {
        if (altCase.getSavedInventory() != null) {
            player.getInventory().setContents(cloneItems(altCase.getSavedInventory()));
        }
        if (altCase.getSavedArmor() != null) {
            player.getInventory().setArmorContents(cloneItems(altCase.getSavedArmor()));
        }
        if (altCase.getSavedOffhand() != null) {
            player.getInventory().setItemInOffHand(altCase.getSavedOffhand().clone());
        }
        player.setGameMode(GameMode.SURVIVAL);
    }

    public void approveCase(AltCase altCase) {
        UUID flaggedId = altCase.getFlaggedPlayerId();
        PlayerStateStorage.PlayerRecord record = playerStateStorage.getOrCreate(flaggedId);
        record.setStatus(PlayerStatus.NORMAL);
        record.setOpenCaseId(null);
        playerStateStorage.save();

        altCase.setStatus(CaseStatus.APPROVED);
        caseStorage.save();

        Player online = Bukkit.getPlayer(flaggedId);
        if (online != null && online.isOnline()) {
            restorePlayer(online, altCase);
            MessageUtil.send(online, config.getApprovedMessage());
        }
    }

    public void denyCase(AltCase altCase, String adminName) {
        UUID flaggedId = altCase.getFlaggedPlayerId();
        String playerName = altCase.getFlaggedPlayerName();

        PlayerStateStorage.PlayerRecord record = playerStateStorage.getOrCreate(flaggedId);
        record.setStatus(PlayerStatus.PERMA_BANNED);
        record.setOpenCaseId(null);
        playerStateStorage.save();

        altCase.setStatus(CaseStatus.DENIED);
        caseStorage.save();

        Bukkit.getBanList(BanList.Type.NAME).addBan(
                playerName,
                "FrostAlt appeal denied",
                (Date) null,
                adminName
        );

        Player online = Bukkit.getPlayer(flaggedId);
        if (online != null && online.isOnline()) {
            online.kick(MessageUtil.parse(config.getDeniedMessage()));
        }
    }

    public boolean isInAppealMode(Player player) {
        PlayerStateStorage.PlayerRecord record = playerStateStorage.get(player.getUniqueId());
        return record != null && record.getStatus() == PlayerStatus.APPEAL_REQUIRED;
    }

    private void saveInventorySnapshot(Player player, AltCase altCase) {
        altCase.setSavedInventory(cloneItems(player.getInventory().getContents()));
        altCase.setSavedArmor(cloneItems(player.getInventory().getArmorContents()));
        ItemStack offhand = player.getInventory().getItemInOffHand();
        altCase.setSavedOffhand(offhand == null ? null : offhand.clone());
    }

    private ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] clones = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            clones[i] = items[i] == null ? null : items[i].clone();
        }
        return clones;
    }
}
