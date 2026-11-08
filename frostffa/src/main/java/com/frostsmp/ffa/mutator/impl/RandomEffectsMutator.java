package com.frostsmp.ffa.mutator.impl;

import com.frostsmp.ffa.arena.MembershipCache;
import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.mutator.Mutator;
import com.frostsmp.ffa.mutator.MutatorId;
import com.frostsmp.ffa.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomEffectsMutator implements Mutator {

    private record Roll(PotionEffectType type, int amplifier, String label) {
    }

    private static final List<Roll> POOL = List.of(
            new Roll(PotionEffectType.SPEED, 1, "Speed II"),
            new Roll(PotionEffectType.SLOWNESS, 0, "Slowness I"),
            new Roll(PotionEffectType.HASTE, 0, "Haste I"),
            new Roll(PotionEffectType.STRENGTH, 1, "Strength II"),
            new Roll(PotionEffectType.JUMP_BOOST, 0, "Jump Boost I"),
            new Roll(PotionEffectType.NAUSEA, 7, "Nausea VIII"),
            new Roll(PotionEffectType.REGENERATION, 1, "Regeneration II"),
            new Roll(PotionEffectType.RESISTANCE, 0, "Resistance I"),
            new Roll(PotionEffectType.BLINDNESS, 1, "Blindness II"),
            new Roll(PotionEffectType.WEAKNESS, 0, "Weakness I"),
            new Roll(PotionEffectType.HEALTH_BOOST, 2, "Health Boost III"),
            new Roll(PotionEffectType.GLOWING, 0, "Glowing"),
            new Roll(PotionEffectType.DARKNESS, 1, "Darkness II")
    );

    private final PluginConfig config;
    private final MembershipCache membership;
    private Roll current;
    private int tick;
    private int intervalTicks;
    private int durationTicks;

    public RandomEffectsMutator(PluginConfig config, MembershipCache membership) {
        this.config = config;
        this.membership = membership;
    }

    @Override
    public String id() {
        return MutatorId.RANDOM_EFFECTS.id();
    }

    @Override
    public String displayName() {
        return MutatorId.RANDOM_EFFECTS.displayName();
    }

    @Override
    public void start() {
        tick = 0;
        intervalTicks = config.randomEffectIntervalSeconds() * 20;
        durationTicks = config.randomEffectDurationSeconds() * 20;
        roll();
    }

    @Override
    public void tick() {
        tick++;
        if (tick % intervalTicks == 0) {
            roll();
        }
    }

    @Override
    public void onEnter(Player player) {
        apply(player);
    }

    @Override
    public void onExit(Player player) {
        if (current != null) {
            player.removePotionEffect(current.type());
        }
    }

    @Override
    public void stop() {
        Roll ending = current;
        current = null;
        if (ending == null) {
            return;
        }
        for (UUID id : membership.members()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.removePotionEffect(ending.type());
            }
        }
    }

    private void roll() {
        Roll previous = current;
        current = POOL.get(ThreadLocalRandom.current().nextInt(POOL.size()));
        if (previous != null && previous.type() != current.type()) {
            for (UUID id : membership.members()) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    player.removePotionEffect(previous.type());
                }
            }
        }
        MessageUtil.broadcast(config.prefixed("random-effect", "effect", current.label()));
        for (UUID id : membership.members()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                apply(player);
            }
        }
    }

    private void apply(Player player) {
        if (current == null) {
            return;
        }
        player.addPotionEffect(new PotionEffect(current.type(), durationTicks, current.amplifier(), true, true, true));
    }
}
