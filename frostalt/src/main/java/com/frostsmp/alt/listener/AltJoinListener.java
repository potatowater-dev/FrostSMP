package com.frostsmp.alt.listener;

import com.frostsmp.alt.model.PlayerStatus;
import com.frostsmp.alt.service.AltDetectionService;
import com.frostsmp.alt.service.EnforcementService;
import com.frostsmp.alt.service.OnlineIpRegistry;
import com.frostsmp.alt.service.PlaytimeService;
import com.frostsmp.alt.storage.IpHistoryStorage;
import com.frostsmp.alt.storage.PlayerStateStorage;
import com.frostsmp.alt.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class AltJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final OnlineIpRegistry ipRegistry;
    private final IpHistoryStorage ipHistoryStorage;
    private final PlaytimeService playtimeService;
    private final PlayerStateStorage playerStateStorage;
    private final EnforcementService enforcementService;
    private final AltDetectionService detectionService;

    public AltJoinListener(
            JavaPlugin plugin,
            OnlineIpRegistry ipRegistry,
            IpHistoryStorage ipHistoryStorage,
            PlaytimeService playtimeService,
            PlayerStateStorage playerStateStorage,
            EnforcementService enforcementService,
            AltDetectionService detectionService
    ) {
        this.plugin = plugin;
        this.ipRegistry = ipRegistry;
        this.ipHistoryStorage = ipHistoryStorage;
        this.playtimeService = playtimeService;
        this.playerStateStorage = playerStateStorage;
        this.enforcementService = enforcementService;
        this.detectionService = detectionService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playtimeService.onJoin(player);
        String ip = MessageUtil.resolveIp(player);
        ipHistoryStorage.record(ip, player.getUniqueId());
        ipRegistry.register(player);

        PlayerStateStorage.PlayerRecord record = playerStateStorage.getOrCreate(player.getUniqueId());
        record.setLastKnownName(player.getName());

        if (record.getStatus() == PlayerStatus.APPEAL_REQUIRED) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        enforcementService.applyAppealMode(player);
                    }
                }
            }.runTask(plugin);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    detectionService.onPlayerJoin(player);
                }
            }
        }.runTask(plugin);
    }
}
