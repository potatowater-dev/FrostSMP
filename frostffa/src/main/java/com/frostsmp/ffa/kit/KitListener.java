package com.frostsmp.ffa.kit;

import com.frostsmp.ffa.config.PluginConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class KitListener implements Listener {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final KitService kits;

    public KitListener(JavaPlugin plugin, PluginConfig config, KitService kits) {
        this.plugin = plugin;
        this.config = config;
        this.kits = kits;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.giveOnJoin()) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline() && kits.shouldAutoGive(event.getPlayer())) {
                kits.give(event.getPlayer());
            }
        }, 10L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!config.giveOnRespawn()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (kits.shouldAutoGive(event.getPlayer())) {
                kits.give(event.getPlayer());
            }
        });
    }
}
