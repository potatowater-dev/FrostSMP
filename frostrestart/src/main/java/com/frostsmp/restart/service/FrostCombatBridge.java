package com.frostsmp.restart.service;

import com.frostsmp.combat.FrostCombatPlugin;
import com.frostsmp.combat.service.CombatService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostCombatBridge {

    private final JavaPlugin plugin;

    public FrostCombatBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enableCombatLogGrace(int seconds) {
        CombatService combatService = getCombatService();
        if (combatService == null) {
            return;
        }
        combatService.enableCombatLogGrace(seconds * 1000L);
    }

    public void disableCombatLogGrace() {
        CombatService combatService = getCombatService();
        if (combatService == null) {
            return;
        }
        combatService.disableCombatLogGrace();
    }

    private CombatService getCombatService() {
        if (!Bukkit.getPluginManager().isPluginEnabled("FrostCombat")) {
            return null;
        }
        var frostCombat = Bukkit.getPluginManager().getPlugin("FrostCombat");
        if (!(frostCombat instanceof FrostCombatPlugin frostCombatPlugin)) {
            plugin.getLogger().warning("FrostCombat is present but could not be accessed for combat-log grace.");
            return null;
        }
        return frostCombatPlugin.getCombatService();
    }
}
