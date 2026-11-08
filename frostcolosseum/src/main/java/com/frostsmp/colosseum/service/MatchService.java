package com.frostsmp.colosseum.service;

import com.frostsmp.colosseum.FrostColosseumPlugin;
import com.frostsmp.colosseum.config.PluginConfig;
import com.frostsmp.colosseum.model.CuboidRegion;
import com.frostsmp.colosseum.model.MatchState;
import com.frostsmp.colosseum.model.TeamColor;
import com.frostsmp.colosseum.storage.RegionStorage;
import com.frostsmp.colosseum.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MatchService {

    private final FrostColosseumPlugin plugin;
    private final PluginConfig config;
    private final RegionStorage regionStorage;
    private final QueueService queueService;
    private final TeamGlowService glowService;
    private final RewardService rewardService;

    private MatchState state = MatchState.IDLE;
    private final Map<UUID, TeamColor> participants = new HashMap<>();
    private final Set<UUID> living = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();

    private BukkitTask lootTask;
    private BukkitTask lootActionbarTask;
    private long lootEndsAtMs;
    private MatchScheduler scheduler;

    public MatchService(
            FrostColosseumPlugin plugin,
            PluginConfig config,
            RegionStorage regionStorage,
            QueueService queueService,
            TeamGlowService glowService,
            RewardService rewardService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.regionStorage = regionStorage;
        this.queueService = queueService;
        this.glowService = glowService;
        this.rewardService = rewardService;
    }

    public void setScheduler(MatchScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public MatchState getState() {
        return state;
    }

    public boolean isParticipant(UUID playerId) {
        return participants.containsKey(playerId);
    }

    public boolean isLiving(UUID playerId) {
        return living.contains(playerId);
    }

    public boolean isSpectator(UUID playerId) {
        return spectators.contains(playerId);
    }

    public TeamColor getTeam(UUID playerId) {
        return participants.get(playerId);
    }

    public boolean canEnterArena(Player player) {
        if (state == MatchState.IDLE) {
            return true;
        }
        UUID id = player.getUniqueId();
        return living.contains(id);
    }

    public boolean tryStart(boolean forced) {
        if (state != MatchState.IDLE) {
            return false;
        }
        if (!regionStorage.isFullyConfigured()) {
            if (forced) {
                List<String> missing = regionStorage.missingSetup();
                MessageUtil.broadcast(MessageUtil.format(
                        config.msg("incomplete-setup"),
                        "missing", String.join(", ", missing)
                ));
            }
            return false;
        }

        List<Player> red = queueService.snapshotTeam(TeamColor.RED);
        List<Player> blue = queueService.snapshotTeam(TeamColor.BLUE);
        if (red.isEmpty() || blue.isEmpty()) {
            if (!forced) {
                MessageUtil.broadcast(config.msg("skip-round"));
            }
            return false;
        }

        Location spawnRed = regionStorage.getPoint("spawn_red");
        Location spawnBlue = regionStorage.getPoint("spawn_blue");
        if (spawnRed == null || spawnBlue == null) {
            return false;
        }

        participants.clear();
        living.clear();
        spectators.clear();

        for (Player player : red) {
            participants.put(player.getUniqueId(), TeamColor.RED);
            living.add(player.getUniqueId());
            queueService.remove(player.getUniqueId());
            player.teleport(spawnRed);
            glowService.apply(player, TeamColor.RED);
        }
        for (Player player : blue) {
            participants.put(player.getUniqueId(), TeamColor.BLUE);
            living.add(player.getUniqueId());
            queueService.remove(player.getUniqueId());
            player.teleport(spawnBlue);
            glowService.apply(player, TeamColor.BLUE);
        }

        queueService.clearQueues();
        state = MatchState.FIGHTING;
        MessageUtil.broadcast(config.msg("match-start"));
        return true;
    }

    public void handleDeath(Player player) {
        if (state != MatchState.FIGHTING) {
            return;
        }
        UUID id = player.getUniqueId();
        if (!living.remove(id)) {
            return;
        }

        glowService.clear(player);
        spectators.add(id);
        MessageUtil.send(player, config.msg("eliminated"));
        checkWin();
    }

    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        queueService.remove(id);
        if (!participants.containsKey(id)) {
            return;
        }
        living.remove(id);
        spectators.remove(id);
        glowService.clear(player);
        participants.remove(id);

        if (state == MatchState.FIGHTING) {
            checkWin();
        }
    }

    private void checkWin() {
        if (state != MatchState.FIGHTING) {
            return;
        }
        int redLiving = countLiving(TeamColor.RED);
        int blueLiving = countLiving(TeamColor.BLUE);

        if (redLiving > 0 && blueLiving > 0) {
            return;
        }
        if (redLiving == 0 && blueLiving == 0) {
            forceEnd(false);
            return;
        }

        TeamColor winners = redLiving > 0 ? TeamColor.RED : TeamColor.BLUE;
        beginLootWindow(winners);
    }

    private int countLiving(TeamColor color) {
        int count = 0;
        for (UUID id : living) {
            if (participants.get(id) == color) {
                count++;
            }
        }
        return count;
    }

    private void beginLootWindow(TeamColor winners) {
        state = MatchState.LOOT_WINDOW;
        int seconds = config.getLootSeconds();
        MessageUtil.broadcast(MessageUtil.format(
                config.msg("winners"),
                "team", winners.display(),
                "seconds", String.valueOf(seconds)
        ));

        ItemStack reward = rewardService.getReward();
        for (UUID id : living) {
            if (participants.get(id) != winners) {
                continue;
            }
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                continue;
            }
            if (reward != null) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward.clone());
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                MessageUtil.send(player, config.msg("reward-given"));
            } else {
                MessageUtil.send(player, config.msg("no-reward"));
            }
        }

        lootEndsAtMs = System.currentTimeMillis() + seconds * 1000L;
        cancelLootTasks();

        lootActionbarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long remaining = Math.max(0, (lootEndsAtMs - System.currentTimeMillis() + 999) / 1000);
            String bar = "<gold>Loot window:</gold> <white>" + remaining + "s</white>";
            for (UUID id : participants.keySet()) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    player.sendActionBar(MessageUtil.parse(bar));
                }
            }
        }, 0L, 20L);

        lootTask = Bukkit.getScheduler().runTaskLater(plugin, this::finishMatch, seconds * 20L);
    }

    public void forceEnd(boolean announce) {
        cancelLootTasks();
        if (state == MatchState.IDLE && participants.isEmpty()) {
            if (announce) {
                MessageUtil.broadcast(config.msg("force-end"));
            }
            return;
        }
        finishMatch();
        if (announce) {
            MessageUtil.broadcast(config.msg("force-end"));
        }
    }

    private void finishMatch() {
        cancelLootTasks();

        Location exit = regionStorage.getPoint("exit");
        Set<UUID> all = new HashSet<>(participants.keySet());
        glowService.clearAll(all);

        for (UUID id : all) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline() && exit != null) {
                player.teleport(exit);
            }
        }

        clearArenaDrops();

        participants.clear();
        living.clear();
        spectators.clear();
        state = MatchState.IDLE;
        queueService.clearQueues();

        MessageUtil.broadcast(config.msg("match-reset"));
        if (scheduler != null) {
            scheduler.resetInterval();
        }
    }

    private void clearArenaDrops() {
        CuboidRegion arena = regionStorage.getRegion("arena");
        if (arena == null || arena.getWorld() == null) {
            return;
        }
        for (Item item : arena.getWorld().getEntitiesByClass(Item.class)) {
            if (arena.contains(item.getLocation())) {
                item.remove();
            }
        }
    }

    private void cancelLootTasks() {
        if (lootTask != null) {
            lootTask.cancel();
            lootTask = null;
        }
        if (lootActionbarTask != null) {
            lootActionbarTask.cancel();
            lootActionbarTask = null;
        }
    }

    public void shutdown() {
        cancelLootTasks();
        if (state != MatchState.IDLE) {
            glowService.clearAll(participants.keySet());
        }
        participants.clear();
        living.clear();
        spectators.clear();
        state = MatchState.IDLE;
    }
}
