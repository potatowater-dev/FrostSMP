package com.frostsmp.ffa.mutator.impl;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.frostsmp.ffa.arena.MembershipCache;
import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.hook.TabHook;
import com.frostsmp.ffa.mutator.BlackoutSkinService;
import com.frostsmp.ffa.mutator.Mutator;
import com.frostsmp.ffa.mutator.MutatorId;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Blackout: same signed skin for everyone + hidden nametags.
 * Apply once (plus one post-fetch refresh). No hide/show loops.
 */
public final class BlackoutMutator implements Mutator {

    private static final String TEAM = "ffa_bo";

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final MembershipCache membership;
    private final BlackoutSkinService skins;
    private final Map<UUID, PlayerProfile> originalProfiles = new HashMap<>();
    private BukkitTask refreshTask;
    private int tick;

    public BlackoutMutator(JavaPlugin plugin, PluginConfig config, MembershipCache membership) {
        this.plugin = plugin;
        this.config = config;
        this.membership = membership;
        this.skins = new BlackoutSkinService(plugin);
    }

    @Override
    public String id() {
        return MutatorId.BLACKOUT.id();
    }

    @Override
    public String displayName() {
        return MutatorId.BLACKOUT.displayName();
    }

    @Override
    public void start() {
        tick = 0;
        cancelRefreshTask();
        skins.refreshAsync(config.blackoutSkinPlayer(), () ->
                plugin.getServer().getScheduler().runTask(plugin, () -> reapplyAll(true)));
        // MutatorManager calls onEnter for members after start() — do not double-apply here.
    }

    @Override
    public void tick() {
        tick++;
        // Keep nametags hidden (TAB fights back). Never hide/show or spam setPlayerProfile.
        if (tick % 40 != 0) {
            return;
        }
        for (UUID id : membership.members()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                TabHook.hide(player);
            }
        }
        if (!TabHook.present()) {
            applyNametags();
        }
    }

    @Override
    public void onEnter(Player player) {
        originalProfiles.putIfAbsent(
                player.getUniqueId(),
                BlackoutSkinService.copyOf(player.getPlayerProfile())
        );
        disguise(player);
        hideIdentity(player);
        // One viewer refresh shortly after enter so clients pick up the new skin.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && membership.isInArena(player)) {
                refreshSkin(player);
            }
        }, 5L);
    }

    @Override
    public void onExit(Player player) {
        TabHook.show(player);
        removeFromTeams(player.getName());
        PlayerProfile original = originalProfiles.remove(player.getUniqueId());
        if (original != null) {
            player.setPlayerProfile(original);
        }
        player.playerListName(null);
        player.displayName(null);
        player.customName(null);
        player.setCustomNameVisible(true);
        refreshSkin(player);
    }

    @Override
    public void stop() {
        cancelRefreshTask();
        for (UUID id : new HashMap<>(originalProfiles).keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                onExit(player);
            }
        }
        // Anyone still marked in arena without a saved profile
        for (UUID id : membership.members()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && originalProfiles.containsKey(id)) {
                onExit(player);
            }
        }
        originalProfiles.clear();
        unregisterTeams();
    }

    private void reapplyAll(boolean refreshViewers) {
        for (UUID id : membership.members()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                continue;
            }
            disguise(player);
            hideIdentity(player);
            if (refreshViewers) {
                refreshSkin(player);
            }
        }
    }

    private void disguise(Player player) {
        player.setPlayerProfile(skins.disguise(player.getUniqueId(), player.getName()));
    }

    private void hideIdentity(Player player) {
        String hidden = config.blackoutHiddenName();
        if (hidden == null || hidden.isEmpty()) {
            hidden = " ";
        }
        player.playerListName(Component.text(hidden));
        player.displayName(Component.text(hidden));
        player.customName(Component.text(hidden));
        player.setCustomNameVisible(false);
        TabHook.hide(player);
        applyNametags();
    }

    private void applyNametags() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Team mainTeam = hideTeam(main);
        for (UUID id : membership.members()) {
            Player target = Bukkit.getPlayer(id);
            if (target == null) {
                continue;
            }
            if (!mainTeam.hasEntry(target.getName())) {
                mainTeam.addEntry(target.getName());
            }
            Scoreboard personal = target.getScoreboard();
            if (personal != null && personal != main) {
                Team team = hideTeam(personal);
                if (!team.hasEntry(target.getName())) {
                    team.addEntry(target.getName());
                }
                for (UUID otherId : membership.members()) {
                    Player other = Bukkit.getPlayer(otherId);
                    if (other == null) {
                        continue;
                    }
                    if (!team.hasEntry(other.getName())) {
                        team.addEntry(other.getName());
                    }
                }
            }
        }
    }

    private Team hideTeam(Scoreboard board) {
        Team team = board.getTeam(TEAM);
        if (team == null) {
            team = board.registerNewTeam(TEAM);
        }
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.setCanSeeFriendlyInvisibles(false);
        team.prefix(Component.empty());
        team.suffix(Component.empty());
        team.setColor(ChatColor.RESET);
        return team;
    }

    private void removeFromTeams(String name) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Team team = viewer.getScoreboard().getTeam(TEAM);
            if (team != null && team.hasEntry(name)) {
                team.removeEntry(name);
            }
        }
        Team main = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(TEAM);
        if (main != null && main.hasEntry(name)) {
            main.removeEntry(name);
        }
    }

    private void unregisterTeams() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Team team = viewer.getScoreboard().getTeam(TEAM);
            if (team != null) {
                try {
                    team.unregister();
                } catch (IllegalStateException ignored) {
                }
            }
        }
        Team mainTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(TEAM);
        if (mainTeam != null) {
            try {
                mainTeam.unregister();
            } catch (IllegalStateException ignored) {
            }
        }
    }

    private void refreshSkin(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player)) {
                continue;
            }
            viewer.hidePlayer(plugin, player);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(player)) {
                    continue;
                }
                viewer.showPlayer(plugin, player);
            }
        }, 2L);
    }

    private void cancelRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }
}
