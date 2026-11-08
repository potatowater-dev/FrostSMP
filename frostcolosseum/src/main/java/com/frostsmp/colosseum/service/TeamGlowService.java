package com.frostsmp.colosseum.service;

import com.frostsmp.colosseum.model.TeamColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

public final class TeamGlowService {

    private static final String RED_TEAM = "frostcolo_red";
    private static final String BLUE_TEAM = "frostcolo_blue";

    private final Scoreboard scoreboard;

    public TeamGlowService() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void apply(Player player, TeamColor color) {
        Team team = ensureTeam(color);
        String entry = player.getName();
        removeFromBoth(entry);
        team.addEntry(entry);
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0, false, false, true));
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        removeFromBoth(player.getName());
        player.removePotionEffect(PotionEffectType.GLOWING);
    }

    public void clearAll(Iterable<UUID> playerIds) {
        for (UUID id : playerIds) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                clear(player);
            }
        }
        // Also wipe leftover entries if players went offline
        Team red = scoreboard.getTeam(RED_TEAM);
        Team blue = scoreboard.getTeam(BLUE_TEAM);
        if (red != null) {
            for (String entry : red.getEntries()) {
                red.removeEntry(entry);
            }
        }
        if (blue != null) {
            for (String entry : blue.getEntries()) {
                blue.removeEntry(entry);
            }
        }
    }

    private void removeFromBoth(String entry) {
        Team red = scoreboard.getTeam(RED_TEAM);
        Team blue = scoreboard.getTeam(BLUE_TEAM);
        if (red != null && red.hasEntry(entry)) {
            red.removeEntry(entry);
        }
        if (blue != null && blue.hasEntry(entry)) {
            blue.removeEntry(entry);
        }
    }

    private Team ensureTeam(TeamColor color) {
        String name = color == TeamColor.RED ? RED_TEAM : BLUE_TEAM;
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        team.setAllowFriendlyFire(true);
        team.setCanSeeFriendlyInvisibles(true);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        if (color == TeamColor.RED) {
            team.color(NamedTextColor.RED);
            team.prefix(Component.text("", NamedTextColor.RED));
        } else {
            team.color(NamedTextColor.AQUA);
            team.prefix(Component.text("", NamedTextColor.AQUA));
        }
        return team;
    }
}
