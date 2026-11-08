package com.frostsmp.ffa.mutator;

import com.frostsmp.ffa.arena.ArenaService;
import com.frostsmp.ffa.arena.MembershipCache;
import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.mutator.impl.BlackoutMutator;
import com.frostsmp.ffa.mutator.impl.FogOfWarMutator;
import com.frostsmp.ffa.mutator.impl.MaceRouletteMutator;
import com.frostsmp.ffa.mutator.impl.RandomEffectsMutator;
import com.frostsmp.ffa.mutator.impl.ShrinkingBorderMutator;
import com.frostsmp.ffa.mutator.impl.StrengthIiiMutator;
import com.frostsmp.ffa.mutator.impl.UhcMutator;
import com.frostsmp.ffa.storage.FfaStorage;
import com.frostsmp.ffa.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class MutatorManager {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final ArenaService arena;
    private final MembershipCache membership;
    private final FfaStorage storage;

    private Mutator active;
    private long endsAt;
    private BukkitTask tickTask;

    public MutatorManager(JavaPlugin plugin, PluginConfig config, ArenaService arena,
                          MembershipCache membership, FfaStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.arena = arena;
        this.membership = membership;
        this.storage = storage;
        membership.setListener(this::onMembership);
    }

    public boolean isActive() {
        return active != null;
    }

    public Mutator active() {
        return active;
    }

    public String activeName() {
        return active == null ? "None" : active.displayName();
    }

    public long remainingMs() {
        if (active == null) {
            return 0L;
        }
        return Math.max(0L, endsAt - System.currentTimeMillis());
    }

    public boolean start(MutatorId id) {
        if (active != null) {
            return false;
        }
        if (arena.region() == null) {
            plugin.getLogger().warning("Cannot start mutator: region " + arena.regionId() + " missing.");
            return false;
        }
        int min = config.durationMinMinutes();
        int max = config.durationMaxMinutes();
        int minutes = min + ThreadLocalRandom.current().nextInt(max - min + 1);
        Mutator mutator = create(id);
        if (mutator instanceof ShrinkingBorderMutator shrinking) {
            shrinking.setDurationMs(minutes * 60_000L);
        }
        active = mutator;
        endsAt = System.currentTimeMillis() + minutes * 60_000L;
        mutator.start();
        membership.scanOnline();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (membership.isInArena(player)) {
                mutator.onEnter(player);
            }
        }
        storage.pushMutator(id.id());
        MessageUtil.broadcast(config.prefixed("mutator-started",
                "name", mutator.displayName(),
                "minutes", String.valueOf(minutes)));
        MessageUtil.broadcastTitle(
                MessageUtil.format(config.mutatorTitle(id.id()),
                        "name", mutator.displayName(),
                        "minutes", String.valueOf(minutes)),
                MessageUtil.format(config.mutatorSubtitle(id.id()),
                        "name", mutator.displayName(),
                        "minutes", String.valueOf(minutes)),
                config.titleFadeInTicks(),
                config.titleStayTicks(),
                config.titleFadeOutTicks());
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        return true;
    }

    public boolean startRandom() {
        MutatorId pick = pickRandom();
        return pick != null && start(pick);
    }

    public void stop() {
        if (active == null) {
            return;
        }
        Mutator ending = active;
        active = null;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (membership.isInArena(player)) {
                ending.onExit(player);
            }
        }
        ending.stop();
        MessageUtil.broadcast(config.prefixed("mutator-ended", "name", ending.displayName()));
    }

    public void shutdown() {
        if (active != null) {
            Mutator ending = active;
            active = null;
            if (tickTask != null) {
                tickTask.cancel();
                tickTask = null;
            }
            ending.stop();
        }
    }

    private void tick() {
        if (active == null) {
            return;
        }
        if (System.currentTimeMillis() >= endsAt) {
            stop();
            return;
        }
        active.tick();
    }

    private void onMembership(Player player, boolean entered) {
        if (active == null) {
            return;
        }
        if (entered) {
            active.onEnter(player);
        } else {
            active.onExit(player);
        }
    }

    public MutatorId pickRandom() {
        List<String> recent = storage.getRecentMutators();
        Map<String, List<String>> blocked = config.blockedAfter();
        String last = recent.isEmpty() ? null : recent.get(0);
        List<MutatorId> candidates = new ArrayList<>();
        for (MutatorId id : MutatorId.values()) {
            if (recent.contains(id.id())) {
                continue;
            }
            if (last != null && blocked.getOrDefault(id.id(), List.of()).contains(last)) {
                continue;
            }
            candidates.add(id);
        }
        if (candidates.isEmpty()) {
            for (MutatorId id : MutatorId.values()) {
                if (!recent.contains(id.id())) {
                    candidates.add(id);
                }
            }
        }
        if (candidates.isEmpty()) {
            return MutatorId.values()[ThreadLocalRandom.current().nextInt(MutatorId.values().length)];
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private Mutator create(MutatorId id) {
        return switch (id) {
            case SHRINKING_BORDER -> new ShrinkingBorderMutator(plugin, config, arena);
            case FOG_OF_WAR -> new FogOfWarMutator(config, membership);
            case UHC -> new UhcMutator(plugin, membership);
            case STRENGTH_III -> new StrengthIiiMutator(config, membership);
            case RANDOM_EFFECTS -> new RandomEffectsMutator(config, membership);
            case BLACKOUT -> new BlackoutMutator(plugin, config, membership);
            case MACE_ROULETTE -> new MaceRouletteMutator(plugin, config, membership);
        };
    }
}
