package com.frostsmp.alt.listener;

import com.frostsmp.alt.config.PluginConfig;
import com.frostsmp.alt.model.PlayerStatus;
import com.frostsmp.alt.storage.PlayerStateStorage;
import com.frostsmp.alt.util.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public final class AltLoginListener implements Listener {

    private final PluginConfig config;
    private final PlayerStateStorage playerStateStorage;

    public AltLoginListener(PluginConfig config, PlayerStateStorage playerStateStorage) {
        this.config = config;
        this.playerStateStorage = playerStateStorage;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        PlayerStateStorage.PlayerRecord record = playerStateStorage.get(event.getPlayer().getUniqueId());
        if (record != null && record.getStatus() == PlayerStatus.PERMA_BANNED) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, MessageUtil.parse(config.getDeniedMessage()));
        }
    }
}
