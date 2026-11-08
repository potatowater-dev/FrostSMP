package com.frostsmp.ffa.placeholder;

import com.frostsmp.ffa.mutator.MutatorManager;
import com.frostsmp.ffa.schedule.ScheduleService;
import com.frostsmp.ffa.util.MessageUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class FfaExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final MutatorManager mutators;
    private final ScheduleService schedule;

    public FfaExpansion(JavaPlugin plugin, MutatorManager mutators, ScheduleService schedule) {
        this.plugin = plugin;
        this.mutators = mutators;
        this.schedule = schedule;
    }

    @Override
    public String getIdentifier() {
        return "frostffa";
    }

    @Override
    public String getAuthor() {
        return "FrostSMP";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        return switch (params.toLowerCase()) {
            case "mutator_active" -> mutators.isActive()
                    ? mutators.activeName()
                    : MessageUtil.mmss(schedule.millisUntilNextMutator());
            case "mutator_timeleft", "mutator_next" -> mutators.isActive()
                    ? mutators.activeName()
                    : MessageUtil.mmss(schedule.millisUntilNextMutator());
            default -> null;
        };
    }
}
