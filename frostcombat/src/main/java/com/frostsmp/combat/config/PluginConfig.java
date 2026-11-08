package com.frostsmp.combat.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PluginConfig {

    private final JavaPlugin plugin;
    private long combatDurationMs;
    private int ticksPerLetter;
    private Set<String> allowedCommands;
    private boolean useWorldSpawn;
    private String spawnWorldName;
    private int spawnDelaySeconds;
    private String spawnCountdownActionBar;
    private String spawnStartedMessage;
    private String spawnTeleportedMessage;
    private String spawnCombatBlockedMessage;
    private String spawnAlreadyPendingMessage;
    private String spawnCancelledMoveMessage;
    private String spawnCancelledCombatMessage;
    private String spawnCancelledDamageMessage;
    private String commandBlockedMessage;
    private String combatLogPenaltyMessage;
    private String combatLogBroadcastMessage;
    private boolean explosionDamageEnabled;
    private double minecartDamage;
    private double expoCartDamage;
    private double expoCartVelocityThreshold;
    private boolean removeMaceRecipe;
    private boolean blockPearlsInCombat;
    private boolean ignoreTimeoutCombatLog;
    private String pearlBlockedMessage;
    private List<StreakTier> streakTiers = List.of();

    public record StreakTier(int min, int maxInclusive, int tokens) {
    }

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        combatDurationMs = config.getLong("combat-duration-seconds", 30L) * 1000L;
        ticksPerLetter = Math.max(1, config.getInt("animation.ticks-per-letter", 4));

        allowedCommands = new HashSet<>();
        List<String> configured = config.getStringList("allowed-commands");
        if (configured.isEmpty()) {
            allowedCommands.add("msg");
            allowedCommands.add("teammsg");
            allowedCommands.add("r");
        } else {
            for (String command : configured) {
                allowedCommands.add(command.toLowerCase(Locale.ROOT));
            }
        }

        useWorldSpawn = config.getBoolean("spawn.use-world-spawn", true);
        spawnWorldName = config.getString("spawn.world", "world");
        spawnDelaySeconds = Math.max(1, config.getInt("spawn.delay-seconds", 5));
        spawnCountdownActionBar = config.getString("messages.spawn-countdown",
                "<gray>Spawning in <white>{seconds}s</white>...</gray>");
        spawnStartedMessage = config.getString("messages.spawn-started",
                "<green>Teleporting to spawn in " + spawnDelaySeconds + " seconds. Do not move.</green>");
        spawnTeleportedMessage = config.getString("messages.spawn-teleported",
                "<green>Teleported to spawn.</green>");
        spawnCombatBlockedMessage = config.getString("messages.spawn-combat-blocked",
                "<red>You cannot use /spawn while in combat.</red>");
        spawnAlreadyPendingMessage = config.getString("messages.spawn-already-pending",
                "<yellow>A spawn teleport is already in progress.</yellow>");
        spawnCancelledMoveMessage = config.getString("messages.spawn-cancelled-move",
                "<red>Spawn teleport cancelled — you moved.</red>");
        spawnCancelledCombatMessage = config.getString("messages.spawn-cancelled-combat",
                "<red>Spawn teleport cancelled — you entered combat.</red>");
        spawnCancelledDamageMessage = config.getString("messages.spawn-cancelled-damage",
                "<red>Spawn teleport cancelled — you took damage.</red>");
        commandBlockedMessage = config.getString("messages.command-blocked",
                "<red>You cannot use that command while in combat.</red>");
        combatLogPenaltyMessage = config.getString("messages.combat-log-penalty",
                "<red>You combat logged and were killed.</red>");
        combatLogBroadcastMessage = config.getString("messages.combat-log-broadcast",
                "<red>{player}</red> <gray>combat logged.</gray>");

        explosionDamageEnabled = config.getBoolean("explosion-damage.enabled", true);
        minecartDamage = heartsToDamage(config.getDouble("explosion-damage.minecart-hearts", 3.0));
        expoCartDamage = heartsToDamage(config.getDouble("explosion-damage.expo-cart-hearts", 5.0));
        expoCartVelocityThreshold = config.getDouble("explosion-damage.expo-velocity-threshold", 0.35);
        removeMaceRecipe = config.getBoolean("remove-mace-recipe", true);
        blockPearlsInCombat = config.getBoolean("block-pearls-in-combat", true);
        ignoreTimeoutCombatLog = config.getBoolean("ignore-timeout-combat-log", true);
        pearlBlockedMessage = config.getString("messages.pearl-blocked",
                "<red>You cannot use pearls while in combat.</red>");
        streakTiers = loadStreakTiers(config);
    }

    private static List<StreakTier> loadStreakTiers(FileConfiguration config) {
        List<StreakTier> tiers = new ArrayList<>();
        List<?> raw = config.getList("kill-tokens.streak-rewards");
        if (raw != null) {
            for (Object entry : raw) {
                if (!(entry instanceof java.util.Map<?, ?> map)) {
                    continue;
                }
                int min = intVal(map.get("min"), 0);
                int max = intVal(map.get("max"), Integer.MAX_VALUE);
                int tokens = Math.max(0, intVal(map.get("tokens"), 1));
                tiers.add(new StreakTier(min, max <= 0 ? Integer.MAX_VALUE : max, tokens));
            }
        }
        if (tiers.isEmpty()) {
            tiers.add(new StreakTier(0, 9, 1));
            tiers.add(new StreakTier(10, 24, 2));
            tiers.add(new StreakTier(25, 49, 3));
            tiers.add(new StreakTier(50, Integer.MAX_VALUE, 4));
        }
        tiers.sort((a, b) -> Integer.compare(a.min(), b.min()));
        return List.copyOf(tiers);
    }

    private static int intVal(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    /** Tokens awarded for a kill that brings the killer to this streak. */
    public int tokensForStreak(int streak) {
        int amount = 1;
        for (StreakTier tier : streakTiers) {
            if (streak >= tier.min() && streak <= tier.maxInclusive()) {
                amount = tier.tokens();
            }
        }
        return Math.max(0, amount);
    }

    private static double heartsToDamage(double hearts) {
        return Math.max(0.0, hearts) * 2.0;
    }

    public long getCombatDurationMs() {
        return combatDurationMs;
    }

    public int getTicksPerLetter() {
        return ticksPerLetter;
    }

    public boolean isCommandAllowed(String label) {
        return allowedCommands.contains(label.toLowerCase(Locale.ROOT));
    }

    public Location resolveSpawnLocation() {
        World world = Bukkit.getWorld(spawnWorldName);
        if (world == null) {
            world = Bukkit.getWorlds().getFirst();
        }
        return world.getSpawnLocation();
    }

    public int getSpawnDelaySeconds() {
        return spawnDelaySeconds;
    }

    public String getSpawnCountdownActionBar() {
        return spawnCountdownActionBar;
    }

    public String getSpawnStartedMessage() {
        return spawnStartedMessage;
    }

    public String getSpawnTeleportedMessage() {
        return spawnTeleportedMessage;
    }

    public String getSpawnCombatBlockedMessage() {
        return spawnCombatBlockedMessage;
    }

    public String getSpawnAlreadyPendingMessage() {
        return spawnAlreadyPendingMessage;
    }

    public String getSpawnCancelledMoveMessage() {
        return spawnCancelledMoveMessage;
    }

    public String getSpawnCancelledCombatMessage() {
        return spawnCancelledCombatMessage;
    }

    public String getSpawnCancelledDamageMessage() {
        return spawnCancelledDamageMessage;
    }

    public String getCommandBlockedMessage() {
        return commandBlockedMessage;
    }

    public String getCombatLogPenaltyMessage() {
        return combatLogPenaltyMessage;
    }

    public String getCombatLogBroadcastMessage() {
        return combatLogBroadcastMessage;
    }

    public boolean isExplosionDamageEnabled() {
        return explosionDamageEnabled;
    }

    public double getMinecartDamage() {
        return minecartDamage;
    }

    public double getExpoCartDamage() {
        return expoCartDamage;
    }

    public double getExpoCartVelocityThreshold() {
        return expoCartVelocityThreshold;
    }

    public boolean isRemoveMaceRecipe() {
        return removeMaceRecipe;
    }

    public boolean isBlockPearlsInCombat() {
        return blockPearlsInCombat;
    }

    public boolean isIgnoreTimeoutCombatLog() {
        return ignoreTimeoutCombatLog;
    }

    public String getPearlBlockedMessage() {
        return pearlBlockedMessage;
    }
}
