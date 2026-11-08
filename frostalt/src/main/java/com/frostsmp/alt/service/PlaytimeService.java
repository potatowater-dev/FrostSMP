package com.frostsmp.alt.service;

import com.frostsmp.alt.storage.PlayerStateStorage;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public final class PlaytimeService {

    private final PlayerStateStorage playerStateStorage;

    public PlaytimeService(PlayerStateStorage playerStateStorage) {
        this.playerStateStorage = playerStateStorage;
    }

    public int getPlaytimeMinutes(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE);
    }

    public void onJoin(Player player) {
        PlayerStateStorage.PlayerRecord record = playerStateStorage.getOrCreate(player.getUniqueId());
        record.setLastKnownName(player.getName());
        record.setSessionStartMs(System.currentTimeMillis());
        record.setLastKnownPlaytimeMinutes(getPlaytimeMinutes(player));
        playerStateStorage.save();
    }

    public void onQuit(Player player) {
        PlayerStateStorage.PlayerRecord record = playerStateStorage.getOrCreate(player.getUniqueId());
        record.setLastKnownName(player.getName());
        record.setLastKnownPlaytimeMinutes(getPlaytimeMinutes(player));
        playerStateStorage.save();
    }
}
