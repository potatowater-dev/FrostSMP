package com.frostsmp.colosseum.service;

import com.frostsmp.colosseum.config.PluginConfig;
import com.frostsmp.colosseum.model.CuboidRegion;
import com.frostsmp.colosseum.model.MatchState;
import com.frostsmp.colosseum.model.TeamColor;
import com.frostsmp.colosseum.storage.RegionStorage;
import com.frostsmp.colosseum.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class QueueService {

    private final PluginConfig config;
    private final RegionStorage regionStorage;

    private MatchService matchService;

    private final Set<UUID> redQueue = new LinkedHashSet<>();
    private final Set<UUID> blueQueue = new LinkedHashSet<>();

    public QueueService(PluginConfig config, RegionStorage regionStorage) {
        this.config = config;
        this.regionStorage = regionStorage;
    }

    public void setMatchService(MatchService matchService) {
        this.matchService = matchService;
    }

    public boolean platformsClosed() {
        if (matchService == null) {
            return false;
        }
        MatchState state = matchService.getState();
        return state == MatchState.FIGHTING || state == MatchState.LOOT_WINDOW;
    }

    public int getRedCount() {
        prune();
        return redQueue.size();
    }

    public int getBlueCount() {
        prune();
        return blueQueue.size();
    }

    public List<Player> snapshotTeam(TeamColor color) {
        prune();
        Set<UUID> source = color == TeamColor.RED ? redQueue : blueQueue;
        List<Player> players = new ArrayList<>();
        for (UUID id : source) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                players.add(player);
                if (players.size() >= config.getMaxPerTeam()) {
                    break;
                }
            }
        }
        return players;
    }

    public void clearQueues() {
        redQueue.clear();
        blueQueue.clear();
    }

    public void remove(UUID playerId) {
        redQueue.remove(playerId);
        blueQueue.remove(playerId);
    }

    public TeamColor getQueuedTeam(UUID playerId) {
        if (redQueue.contains(playerId)) {
            return TeamColor.RED;
        }
        if (blueQueue.contains(playerId)) {
            return TeamColor.BLUE;
        }
        return null;
    }

    /**
     * Called on block-change moves. Returns true if the move should be cancelled / shoved back.
     */
    public boolean handleMove(Player player, Location from, Location to) {
        if (player == null || to == null) {
            return false;
        }
        if (from != null
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return false;
        }

        if (matchService != null && matchService.isParticipant(player.getUniqueId())) {
            return false;
        }

        CuboidRegion red = regionStorage.getRegion("platform_red");
        CuboidRegion blue = regionStorage.getRegion("platform_blue");

        boolean inRed = red != null && red.contains(to);
        boolean inBlue = blue != null && blue.contains(to);
        boolean wasRed = red != null && from != null && red.contains(from);
        boolean wasBlue = blue != null && from != null && blue.contains(from);

        UUID id = player.getUniqueId();

        if (platformsClosed()) {
            if ((inRed && !wasRed) || (inBlue && !wasBlue)) {
                MessageUtil.send(player, config.msg("platforms-closed"));
                shoveBack(player, from);
                return true;
            }
            return false;
        }

        if (inRed && !wasRed) {
            return tryJoin(player, TeamColor.RED, from);
        }
        if (inBlue && !wasBlue) {
            return tryJoin(player, TeamColor.BLUE, from);
        }

        if (!inRed && wasRed && redQueue.contains(id)) {
            redQueue.remove(id);
            MessageUtil.send(player, config.msg("left-queue"));
        }
        if (!inBlue && wasBlue && blueQueue.contains(id)) {
            blueQueue.remove(id);
            MessageUtil.send(player, config.msg("left-queue"));
        }
        return false;
    }

    public void scanStandingPlayers() {
        prune();
        CuboidRegion red = regionStorage.getRegion("platform_red");
        CuboidRegion blue = regionStorage.getRegion("platform_blue");

        // Drop anyone no longer standing on their platform
        redQueue.removeIf(id -> {
            Player player = Bukkit.getPlayer(id);
            return player == null || red == null || !red.contains(player.getLocation());
        });
        blueQueue.removeIf(id -> {
            Player player = Bukkit.getPlayer(id);
            return player == null || blue == null || !blue.contains(player.getLocation());
        });

        if (platformsClosed()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (matchService != null && matchService.isParticipant(player.getUniqueId())) {
                continue;
            }
            Location loc = player.getLocation();
            UUID id = player.getUniqueId();
            if (red != null && red.contains(loc) && !redQueue.contains(id)) {
                if (redQueue.size() < config.getMaxPerTeam()) {
                    blueQueue.remove(id);
                    redQueue.add(id);
                    MessageUtil.send(player, config.msg("queued-red"));
                }
            } else if (blue != null && blue.contains(loc) && !blueQueue.contains(id)) {
                if (blueQueue.size() < config.getMaxPerTeam()) {
                    redQueue.remove(id);
                    blueQueue.add(id);
                    MessageUtil.send(player, config.msg("queued-blue"));
                }
            }
        }
    }

    private boolean tryJoin(Player player, TeamColor color, Location from) {
        UUID id = player.getUniqueId();
        Set<UUID> target = color == TeamColor.RED ? redQueue : blueQueue;
        Set<UUID> other = color == TeamColor.RED ? blueQueue : redQueue;

        if (target.contains(id)) {
            return false;
        }
        if (target.size() >= config.getMaxPerTeam()) {
            MessageUtil.send(player, MessageUtil.format(config.msg("platform-full"), "max", String.valueOf(config.getMaxPerTeam())));
            shoveBack(player, from);
            return true;
        }

        other.remove(id);
        target.add(id);
        MessageUtil.send(player, config.msg(color == TeamColor.RED ? "queued-red" : "queued-blue"));
        return false;
    }

    private void shoveBack(Player player, Location from) {
        if (from == null) {
            return;
        }
        player.teleport(from.clone());
        player.setVelocity(new Vector(0, 0, 0));
    }

    private void prune() {
        redQueue.removeIf(id -> {
            Player p = Bukkit.getPlayer(id);
            return p == null || !p.isOnline();
        });
        blueQueue.removeIf(id -> {
            Player p = Bukkit.getPlayer(id);
            return p == null || !p.isOnline();
        });
    }
}
