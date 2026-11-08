package com.frostsmp.alt.service;

import com.frostsmp.alt.config.PluginConfig;
import com.frostsmp.alt.model.PlayerStatus;
import com.frostsmp.alt.storage.PlayerStateStorage;
import com.frostsmp.alt.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class AltDetectionService {

    private final PluginConfig config;
    private final OnlineIpRegistry ipRegistry;
    private final PlaytimeService playtimeService;
    private final PlayerStateStorage playerStateStorage;
    private final EnforcementService enforcementService;

    public AltDetectionService(
            PluginConfig config,
            OnlineIpRegistry ipRegistry,
            PlaytimeService playtimeService,
            PlayerStateStorage playerStateStorage,
            EnforcementService enforcementService
    ) {
        this.config = config;
        this.ipRegistry = ipRegistry;
        this.playtimeService = playtimeService;
        this.playerStateStorage = playerStateStorage;
        this.enforcementService = enforcementService;
    }

    public void onPlayerJoin(Player player) {
        if (!config.isSameIpConcurrent() || player.hasPermission("frostsmp.alt.bypass")) {
            return;
        }
        PlayerStateStorage.PlayerRecord record = playerStateStorage.getOrCreate(player.getUniqueId());
        if (record.getStatus() == PlayerStatus.PERMA_BANNED) {
            return;
        }
        if (record.getStatus() == PlayerStatus.APPEAL_REQUIRED) {
            return;
        }

        String ip = MessageUtil.resolveIp(player);
        Set<Player> onlineOnIp = ipRegistry.getOnlinePlayersOnIp(ip);
        List<Player> candidates = new ArrayList<>();
        for (Player online : onlineOnIp) {
            if (!online.getUniqueId().equals(player.getUniqueId())
                    && !online.hasPermission("frostsmp.alt.bypass")) {
                PlayerStateStorage.PlayerRecord other = playerStateStorage.getOrCreate(online.getUniqueId());
                if (other.getStatus() != PlayerStatus.APPEAL_REQUIRED
                        && other.getStatus() != PlayerStatus.PERMA_BANNED) {
                    candidates.add(online);
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        candidates.add(player);

        Player flagged = selectFlaggedPlayer(candidates);
        if (flagged == null) {
            return;
        }
        enforcementService.handleDetection(ip, flagged, candidates);
    }

    private Player selectFlaggedPlayer(List<Player> candidates) {
        return candidates.stream()
                .min(Comparator
                        .comparingInt(playtimeService::getPlaytimeMinutes)
                        .thenComparingLong(player -> ipRegistry.getJoinTime(player.getUniqueId()))
                        .reversed())
                .orElse(null);
    }
}
