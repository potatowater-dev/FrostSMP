package com.frostsmp.juggernaut.service;

import com.frostsmp.juggernaut.config.PluginConfig;
import com.frostsmp.juggernaut.util.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player bar showing the current #1 damager and the viewer's own damage.
 */
public final class BossBarService {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final DamageTracker damageTracker;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    private EventService eventService;
    private BukkitTask task;

    public BossBarService(JavaPlugin plugin, PluginConfig config, DamageTracker damageTracker) {
        this.plugin = plugin;
        this.config = config;
        this.damageTracker = damageTracker;
    }

    public void attach(EventService eventService) {
        this.eventService = eventService;
    }

    public void start() {
        if (!config.isBossBarEnabled()) {
            return;
        }
        stopTask();
        int interval = config.getBossBarUpdateTicks();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 1L, interval);
    }

    public void stop() {
        stopTask();
        for (Map.Entry<UUID, BossBar> entry : bars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        bars.clear();
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void hide(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private void update() {
        if (eventService == null || !eventService.isRunning()) {
            return;
        }

        DamageTracker.Entry top = damageTracker.getTop();
        String topName = top != null && top.name() != null ? top.name() : config.getBossBarEmptyName();
        double topDamage = top != null ? top.damage() : 0.0;
        String aliveText = String.valueOf(eventService.getAliveJuggernautCount());
        String totalText = String.valueOf(eventService.getJuggernautCount());

        for (Player player : Bukkit.getOnlinePlayers()) {
            double yourDamage = damageTracker.getDamage(player.getUniqueId());
            int rank = damageTracker.getRank(player.getUniqueId());
            boolean isJuggernaut = eventService.isJuggernaut(player.getUniqueId());

            String template = isJuggernaut ? config.getBossBarJuggernautFormat() : config.getBossBarFormat();
            String title = MessageUtil.format(template,
                    "top", topName,
                    "top_damage", MessageUtil.formatDamage(topDamage),
                    "your_damage", MessageUtil.formatDamage(yourDamage),
                    "rank", rank > 0 ? String.valueOf(rank) : "-",
                    "alive", aliveText,
                    "total", totalText);

            float progress = topDamage <= 0 ? 0f : (float) Math.min(1.0, yourDamage / topDamage);
            if (isJuggernaut) {
                progress = topDamage <= 0 ? 0f : 1f;
            }

            BossBar bar = bars.get(player.getUniqueId());
            if (bar == null) {
                bar = BossBar.bossBar(MessageUtil.parse(title), progress,
                        config.getBossBarColor(), config.getBossBarOverlay());
                bars.put(player.getUniqueId(), bar);
                player.showBossBar(bar);
            } else {
                bar.name(MessageUtil.parse(title));
                bar.progress(Math.max(0f, Math.min(1f, progress)));
                bar.color(config.getBossBarColor());
                bar.overlay(config.getBossBarOverlay());
            }
        }

        Iterator<Map.Entry<UUID, BossBar>> it = bars.entrySet().iterator();
        while (it.hasNext()) {
            if (Bukkit.getPlayer(it.next().getKey()) == null) {
                it.remove();
            }
        }
    }
}
