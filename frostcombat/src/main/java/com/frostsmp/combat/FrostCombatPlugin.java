package com.frostsmp.combat;

import com.frostsmp.combat.command.CombatCommandRegistration;
import com.frostsmp.combat.placeholder.CombatExpansion;
import com.frostsmp.combat.recipe.MaceRecipeRemover;
import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.display.CombatActionBar;
import com.frostsmp.combat.item.KillToken;
import com.frostsmp.combat.listener.CombatCommandListener;
import com.frostsmp.combat.listener.ExpoCartListener;
import com.frostsmp.combat.listener.ExplosionDamageListener;
import com.frostsmp.combat.service.ExpoCartTracker;
import com.frostsmp.combat.listener.CombatDamageListener;
import com.frostsmp.combat.listener.CombatJoinListener;
import com.frostsmp.combat.listener.CombatPearlListener;
import com.frostsmp.combat.listener.CombatQuitListener;
import com.frostsmp.combat.listener.KillTokenListener;
import com.frostsmp.combat.listener.PlayerStatsListener;
import com.frostsmp.combat.listener.SpawnListener;
import com.frostsmp.combat.service.CombatService;
import com.frostsmp.combat.service.KillStreakService;
import com.frostsmp.combat.service.PlayerStatsService;
import com.frostsmp.combat.service.SpawnService;
import com.frostsmp.combat.storage.CombatLogStorage;
import com.frostsmp.combat.storage.KillStreakStorage;
import com.frostsmp.combat.storage.PlayerStatsStorage;
import com.frostsmp.license.LicenseGate;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;

public final class FrostCombatPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private CombatService combatService;
    private SpawnService spawnService;
    private CombatLogStorage combatLogStorage;
    private KillStreakService killStreakService;
    private PlayerStatsService playerStatsService;

    @Override
    public void onEnable() {
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        boolean licensed = LicenseGate.activate(this);
        if (!licensed) {
            getLogger().severe("Invalid or missing license.key in config.yml. FrostCombat features are disabled.");
            CombatCommandRegistration.register(this, null, null, null, false);
            return;
        }

        applyMaceRecipeRemoval();
        KillToken.create();

        combatLogStorage = new CombatLogStorage(this);
        combatLogStorage.load();

        KillStreakStorage killStreakStorage = new KillStreakStorage(this);
        killStreakStorage.load();

        File statsFile = new File(getDataFolder(), "player-stats.yml");
        boolean seedFromBukkit = !statsFile.exists();
        PlayerStatsStorage playerStatsStorage = new PlayerStatsStorage(this);
        playerStatsStorage.load();
        playerStatsService = new PlayerStatsService(playerStatsStorage, seedFromBukkit);

        CombatActionBar actionBar = new CombatActionBar(pluginConfig);
        combatService = new CombatService(pluginConfig, actionBar);
        KillStreakService killStreakService = new KillStreakService(pluginConfig, killStreakStorage);
        this.killStreakService = killStreakService;
        spawnService = new SpawnService(pluginConfig, combatService);
        ExpoCartTracker expoCartTracker = new ExpoCartTracker();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new CombatDamageListener(combatService), this);
        pm.registerEvents(new CombatPearlListener(combatService, pluginConfig), this);
        pm.registerEvents(new ExpoCartListener(pluginConfig, expoCartTracker), this);
        pm.registerEvents(new ExplosionDamageListener(pluginConfig, expoCartTracker), this);
        pm.registerEvents(new CombatCommandListener(combatService, pluginConfig), this);
        pm.registerEvents(new CombatQuitListener(combatService, pluginConfig, combatLogStorage, killStreakService), this);
        pm.registerEvents(new CombatJoinListener(combatLogStorage, pluginConfig), this);
        pm.registerEvents(new SpawnListener(spawnService, pluginConfig), this);
        pm.registerEvents(new KillTokenListener(combatService, killStreakService, playerStatsService), this);
        pm.registerEvents(new PlayerStatsListener(playerStatsService), this);

        CombatCommandRegistration.register(this, combatService, spawnService, playerStatsService, true);

        getServer().getScheduler().runTask(this, () -> {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
                getLogger().warning("PlaceholderAPI missing — frostcombat placeholders unavailable.");
                return;
            }
            // Drop any stale expansion left behind by Plugman / soft reloads (it keeps old data).
            unregisterFrostCombatExpansion();
            boolean ok = new CombatExpansion(this, killStreakService, playerStatsService).register();
            if (ok) {
                getLogger().info("Registered PlaceholderAPI expansion: %frostcombat_*%");
            } else {
                getLogger().warning("Failed to register PlaceholderAPI expansion (frostcombat).");
            }
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                combatService.tick();
                spawnService.tick();
            }
        }.runTaskTimer(this, 1L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                playerStatsService.tickPlaytime();
            }
        }.runTaskTimer(this, 20L * 30L, 20L * 30L);

        new BukkitRunnable() {
            @Override
            public void run() {
                playerStatsService.save();
            }
        }.runTaskTimer(this, 20L * 60L * 5L, 20L * 60L * 5L);

        getLogger().info("FrostCombat enabled.");
    }

    @Override
    public void onDisable() {
        if (combatLogStorage != null) {
            combatLogStorage.save();
        }
        if (killStreakService != null) {
            killStreakService.save();
        }
        if (playerStatsService != null) {
            playerStatsService.save();
        }
        getLogger().info("FrostCombat disabled.");
    }

    public void reloadPluginConfig() {
        pluginConfig.load();
        applyMaceRecipeRemoval();
    }

    private void applyMaceRecipeRemoval() {
        if (pluginConfig.isRemoveMaceRecipe()) {
            MaceRecipeRemover.remove(this);
        }
    }

    private void unregisterFrostCombatExpansion() {
        try {
            Object plugin = me.clip.placeholderapi.PlaceholderAPIPlugin.getInstance();
            Object manager = plugin.getClass().getMethod("getLocalExpansionManager").invoke(plugin);
            @SuppressWarnings("unchecked")
            java.util.Collection<me.clip.placeholderapi.expansion.PlaceholderExpansion> expansions =
                    (java.util.Collection<me.clip.placeholderapi.expansion.PlaceholderExpansion>)
                            manager.getClass().getMethod("getExpansions").invoke(manager);
            for (me.clip.placeholderapi.expansion.PlaceholderExpansion expansion : List.copyOf(expansions)) {
                if ("frostcombat".equalsIgnoreCase(expansion.getIdentifier())) {
                    manager.getClass()
                            .getMethod("unregister", me.clip.placeholderapi.expansion.PlaceholderExpansion.class)
                            .invoke(manager, expansion);
                    getLogger().info("Unregistered stale PlaceholderAPI expansion: frostcombat");
                }
            }
        } catch (Throwable throwable) {
            getLogger().warning("Could not unregister old frostcombat PAPI expansion: " + throwable.getMessage());
        }
    }

    public CombatService getCombatService() {
        return combatService;
    }

    public KillStreakService getKillStreakService() {
        return killStreakService;
    }

    public PlayerStatsService getPlayerStatsService() {
        return playerStatsService;
    }
}
