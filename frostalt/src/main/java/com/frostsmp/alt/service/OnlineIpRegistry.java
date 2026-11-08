package com.frostsmp.alt.service;

import com.frostsmp.alt.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OnlineIpRegistry {

    private final Map<String, Set<UUID>> onlineByIp = new HashMap<>();
    private final Map<UUID, String> playerIps = new HashMap<>();
    private final Map<UUID, Long> joinTimes = new HashMap<>();

    public void register(Player player) {
        String ip = MessageUtil.resolveIp(player);
        playerIps.put(player.getUniqueId(), ip);
        joinTimes.put(player.getUniqueId(), System.currentTimeMillis());
        onlineByIp.computeIfAbsent(ip, ignored -> new HashSet<>()).add(player.getUniqueId());
    }

    public void unregister(Player player) {
        UUID id = player.getUniqueId();
        String ip = playerIps.remove(id);
        joinTimes.remove(id);
        if (ip != null) {
            Set<UUID> online = onlineByIp.get(ip);
            if (online != null) {
                online.remove(id);
                if (online.isEmpty()) {
                    onlineByIp.remove(ip);
                }
            }
        }
    }

    public String getIp(UUID playerId) {
        return playerIps.get(playerId);
    }

    public long getJoinTime(UUID playerId) {
        return joinTimes.getOrDefault(playerId, 0L);
    }

    public Set<UUID> getOnlineOnIp(String ip) {
        return onlineByIp.getOrDefault(ip, Collections.emptySet());
    }

    public Set<Player> getOnlinePlayersOnIp(String ip) {
        Set<Player> players = new HashSet<>();
        for (UUID id : getOnlineOnIp(ip)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }
}
