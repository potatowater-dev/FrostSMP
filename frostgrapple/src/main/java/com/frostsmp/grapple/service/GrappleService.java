package com.frostsmp.grapple.service;

import com.frostsmp.grapple.config.GrappleConfig;
import com.frostsmp.grapple.util.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GrappleService {

    private final JavaPlugin plugin;
    private final GrappleConfig config;
    private final Map<Integer, Integer> noFallTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cooldownTotalMs = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> cooldownBars = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    public GrappleService(JavaPlugin plugin, GrappleConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean tryPullSelf(Player player, Location hookLocation, ItemStack rod) {
        if (!canPull(player, hookLocation, rod)) {
            return false;
        }
        pullEntityToLocation(player, hookLocation);
        afterSuccessfulPull(player, rod);
        return true;
    }

    public boolean tryPullEntity(Player player, Entity target, ItemStack rod) {
        if (target == null || !target.isValid()) {
            return false;
        }
        if (!canPull(player, target.getLocation(), rod)) {
            return false;
        }
        pullEntityToLocation(target, player.getLocation());
        afterSuccessfulPull(player, rod);
        return true;
    }

    public boolean isOnCooldown(Player player, ItemStack rod) {
        if (player.hasPermission("frostsmp.grapple.nocooldown")) {
            return false;
        }
        Long until = cooldownUntil.get(player.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    /** Shows the cooldown UI if the player is currently cooling down. */
    public void pulseCooldown(Player player, ItemStack rod) {
        if (isOnCooldown(player, rod)) {
            sendCooldownDisplay(player);
        }
    }

    public boolean isFallProtected(Entity entity) {
        return noFallTasks.containsKey(entity.getEntityId());
    }

    public void clear(Player player) {
        Integer taskId = noFallTasks.remove(player.getEntityId());
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        cooldownUntil.remove(player.getUniqueId());
        cooldownTotalMs.remove(player.getUniqueId());
        BukkitTask actionBar = cooldownBars.remove(player.getUniqueId());
        if (actionBar != null) {
            actionBar.cancel();
        }
        hideBossBar(player);
        player.sendActionBar(Component.empty());
    }

    public void clearAll() {
        for (Integer taskId : noFallTasks.values()) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        noFallTasks.clear();
        for (BukkitTask actionBar : cooldownBars.values()) {
            actionBar.cancel();
        }
        cooldownBars.clear();
        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        bossBars.clear();
        cooldownUntil.clear();
        cooldownTotalMs.clear();
    }

    private boolean canPull(Player player, Location target, ItemStack rod) {
        if (isOnCooldown(player, rod)) {
            sendCooldownDisplay(player);
            return false;
        }
        if (target.getWorld() == null || player.getWorld() == null || !target.getWorld().equals(player.getWorld())) {
            return false;
        }
        double distance = player.getLocation().distance(target);
        if (distance > config.range()) {
            MessageUtil.send(player, config.prefixed("too-far",
                    "range", stripTrailingZeros(config.range())));
            return false;
        }
        return true;
    }

    private void afterSuccessfulPull(Player player, ItemStack rod) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
        int cooldownSeconds = config.cooldownSeconds();
        if (cooldownSeconds <= 0 || player.hasPermission("frostsmp.grapple.nocooldown")) {
            return;
        }
        int totalMs = cooldownSeconds * 1000;
        long until = System.currentTimeMillis() + totalMs;
        cooldownUntil.put(player.getUniqueId(), until);
        cooldownTotalMs.put(player.getUniqueId(), totalMs);
        startCooldownDisplay(player);
    }

    private void pullEntityToLocation(Entity entity, Location loc) {
        Location from = entity.getLocation();
        double distance = from.distance(loc);
        if (distance < 0.5) {
            return;
        }
        if (entity instanceof Player player && distance < 6.0) {
            pullSlightly(player, loc);
            protectFall(entity);
            return;
        }

        Vector boost = entity.getVelocity();
        boost.setY(config.verticalBoost());
        entity.setVelocity(boost);
        entity.setFallDistance(0.0f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!entity.isValid()) {
                return;
            }
            Location origin = entity.getLocation();
            double g = -0.08;
            double t = Math.max(1.0, origin.distance(loc));
            double vX = (1.0 + 0.07 * t) * (loc.getX() - origin.getX()) / t;
            double vY = (1.0 + 0.03 * t) * (loc.getY() - origin.getY()) / t - 0.5 * g * t;
            double vZ = (1.0 + 0.07 * t) * (loc.getZ() - origin.getZ()) / t;
            Vector velocity = new Vector(vX, vY, vZ).multiply(config.strength());
            entity.setVelocity(velocity);
            entity.setFallDistance(0.0f);
        }, 1L);

        protectFall(entity);
    }

    private void pullSlightly(Player player, Location loc) {
        if (loc.getY() > player.getLocation().getY()) {
            player.setVelocity(new Vector(0, 0.25, 0));
            return;
        }
        player.setVelocity(loc.toVector().subtract(player.getLocation().toVector()));
        player.setFallDistance(0.0f);
    }

    private void protectFall(Entity entity) {
        if (config.fallDamage()) {
            return;
        }
        Integer previous = noFallTasks.remove(entity.getEntityId());
        if (previous != null) {
            plugin.getServer().getScheduler().cancelTask(previous);
        }
        int taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            noFallTasks.remove(entity.getEntityId());
        }, config.fallProtectionTicks());
        noFallTasks.put(entity.getEntityId(), taskId);
    }

    private static String stripTrailingZeros(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private void startCooldownDisplay(Player player) {
        BukkitTask previous = cooldownBars.remove(player.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }
        sendCooldownDisplay(player);
        // Every tick so it stays visible over other action bars when possible
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                clear(player);
                return;
            }
            Long endsAt = cooldownUntil.get(player.getUniqueId());
            if (endsAt == null || endsAt <= System.currentTimeMillis()) {
                cooldownUntil.remove(player.getUniqueId());
                cooldownTotalMs.remove(player.getUniqueId());
                BukkitTask current = cooldownBars.remove(player.getUniqueId());
                if (current != null) {
                    current.cancel();
                }
                hideBossBar(player);
                player.sendActionBar(MessageUtil.parse(config.prefixed("cooldown-ready")));
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && !isOnCooldown(player, null)) {
                        player.sendActionBar(Component.empty());
                    }
                }, 20L);
                return;
            }
            sendCooldownDisplay(player);
        }, 1L, 1L);
        cooldownBars.put(player.getUniqueId(), task);
    }

    private void sendCooldownDisplay(Player player) {
        Long until = cooldownUntil.get(player.getUniqueId());
        if (until == null) {
            return;
        }
        long remainingMs = Math.max(0L, until - System.currentTimeMillis());
        double remainingSec = remainingMs / 1000.0D;
        int totalMs = cooldownTotalMs.getOrDefault(player.getUniqueId(), Math.max(1, config.cooldownSeconds()) * 1000);
        double remainingFraction = Math.min(1.0D, remainingMs / (double) Math.max(1, totalMs));
        float fillProgress = 1f - (float) remainingFraction;
        String seconds = String.format(java.util.Locale.US, "%.1f", remainingSec);
        String bar = progressBar(remainingFraction);

        if (config.showCooldownActionBar()) {
            player.sendActionBar(MessageUtil.parse(config.prefixed(
                    "cooldown-actionbar",
                    "seconds", seconds,
                    "bar", bar
            )));
        }
        if (config.showCooldownBossBar()) {
            updateBossBar(player, seconds, fillProgress);
        }
    }

    private void updateBossBar(Player player, String seconds, float fillProgress) {
        BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), ignored -> {
            BossBar created = BossBar.bossBar(
                    Component.text("Grapple"),
                    0f,
                    BossBar.Color.YELLOW,
                    BossBar.Overlay.PROGRESS
            );
            player.showBossBar(created);
            return created;
        });
        bar.name(MessageUtil.parse(config.prefixed(
                "cooldown-bossbar",
                "seconds", seconds
        )));
        bar.progress(Math.max(0f, Math.min(1f, fillProgress)));
        bar.color(fillProgress >= 1f ? BossBar.Color.GREEN : BossBar.Color.YELLOW);
    }

    private void hideBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private static String progressBar(double fractionRemaining) {
        final int segments = 12;
        int filled = (int) Math.ceil(fractionRemaining * segments);
        filled = Math.max(0, Math.min(segments, filled));
        StringBuilder bar = new StringBuilder(segments * 16);
        for (int i = 0; i < segments; i++) {
            if (i < filled) {
                bar.append("<gold>■</gold>");
            } else {
                bar.append("<dark_gray>■</dark_gray>");
            }
        }
        return bar.toString();
    }
}
