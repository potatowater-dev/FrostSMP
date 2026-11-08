package com.frostsmp.ffa.mutator.impl;

import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.util.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Mace-holder-only double jump with a minimal bossbar. */
final class MaceRouletteDoubleJump implements Listener {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final Supplier<UUID> holderSupplier;

    private final Map<UUID, Long> cooldownUntilMs = new HashMap<>();
    private final Map<UUID, Boolean> restoreAllowFlight = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();

    private boolean active;
    private BukkitTask hudTask;

    MaceRouletteDoubleJump(JavaPlugin plugin, PluginConfig config, Supplier<UUID> holderSupplier) {
        this.plugin = plugin;
        this.config = config;
        this.holderSupplier = holderSupplier;
    }

    void start() {
        active = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        hudTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickHud, 0L, 2L);
        syncHolder();
    }

    void stop() {
        active = false;
        HandlerList.unregisterAll(this);
        if (hudTask != null) {
            hudTask.cancel();
            hudTask = null;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            untrack(player);
        }
        cooldownUntilMs.clear();
        restoreAllowFlight.clear();
        bars.clear();
    }

    /** Call whenever the mace holder changes. */
    void syncHolder() {
        if (!active) {
            return;
        }
        UUID holder = holderSupplier.get();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (holder != null && holder.equals(player.getUniqueId())) {
                track(player);
            } else {
                untrack(player);
            }
        }
    }

    void onExit(Player player) {
        untrack(player);
    }

    void tick() {
        if (!active) {
            return;
        }
        syncHolder();
        UUID holder = holderSupplier.get();
        if (holder == null) {
            return;
        }
        Player player = plugin.getServer().getPlayer(holder);
        if (player != null) {
            refreshFlight(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!active || !isHolder(event.getPlayer())) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        event.setCancelled(true);
        player.setFlying(false);

        if (player.isOnGround() || player.getLocation().getBlock().getRelative(0, -1, 0).getType().isSolid()) {
            player.setAllowFlight(true);
            return;
        }

        if (!isReady(player)) {
            updateBossBar(player);
            return;
        }

        Vector velocity = player.getVelocity();
        player.setVelocity(new Vector(velocity.getX(), config.maceRouletteDoubleJumpStrength(), velocity.getZ()));
        player.setAllowFlight(false);

        long cooldownMs = config.maceRouletteDoubleJumpCooldownSeconds() * 1000L;
        cooldownUntilMs.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMs);
        updateBossBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!active || event.getTo() == null || !isHolder(event.getPlayer())) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        refreshFlight(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        untrack(event.getPlayer());
        cooldownUntilMs.remove(event.getPlayer().getUniqueId());
    }

    private void tickHud() {
        UUID holder = holderSupplier.get();
        if (holder == null) {
            return;
        }
        Player player = plugin.getServer().getPlayer(holder);
        if (player != null && player.isOnline()) {
            updateBossBar(player);
            refreshFlight(player);
        }
    }

    private boolean isHolder(Player player) {
        UUID holder = holderSupplier.get();
        return holder != null && holder.equals(player.getUniqueId());
    }

    private void track(Player player) {
        UUID id = player.getUniqueId();
        if (restoreAllowFlight.containsKey(id)) {
            updateBossBar(player);
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        restoreAllowFlight.put(id, player.getAllowFlight());
        refreshFlight(player);
        updateBossBar(player);
    }

    private void untrack(Player player) {
        UUID id = player.getUniqueId();
        BossBar bar = bars.remove(id);
        if (bar != null) {
            player.hideBossBar(bar);
        }
        Boolean previous = restoreAllowFlight.remove(id);
        if (previous == null) {
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(previous);
            player.setFlying(false);
        }
    }

    private void refreshFlight(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (isReady(player) && player.isOnGround()) {
            player.setAllowFlight(true);
            player.setFlying(false);
        }
    }

    private boolean isReady(Player player) {
        Long until = cooldownUntilMs.get(player.getUniqueId());
        return until == null || until <= System.currentTimeMillis();
    }

    private void updateBossBar(Player player) {
        BossBar bar = bars.computeIfAbsent(player.getUniqueId(), ignored -> {
            BossBar created = BossBar.bossBar(
                    Component.text("DJ"),
                    1f,
                    BossBar.Color.WHITE,
                    BossBar.Overlay.PROGRESS
            );
            player.showBossBar(created);
            return created;
        });

        if (isReady(player)) {
            bar.name(MessageUtil.parse(firstNonBlank(
                    config.msg("mace-roulette-double-jump-ready"),
                    "<white>DJ</white> <green>READY</green>"
            )));
            bar.progress(1f);
            bar.color(BossBar.Color.WHITE);
            return;
        }

        Long until = cooldownUntilMs.get(player.getUniqueId());
        if (until == null) {
            return;
        }
        long remainingMs = Math.max(0L, until - System.currentTimeMillis());
        long totalMs = config.maceRouletteDoubleJumpCooldownSeconds() * 1000L;
        float progress = 1f - (float) Math.min(1.0D, remainingMs / (double) Math.max(1L, totalMs));
        bar.name(MessageUtil.parse(firstNonBlank(
                config.msg("mace-roulette-double-jump-cooldown"),
                "<gray>DJ</gray>"
        )));
        bar.progress(Math.max(0f, Math.min(1f, progress)));
        bar.color(BossBar.Color.BLUE);
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
