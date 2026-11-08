package com.frostsmp.ffa.mutator.impl;

import com.frostsmp.ffa.arena.MembershipCache;
import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.mutator.Mutator;
import com.frostsmp.ffa.mutator.MutatorId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public final class StrengthIiiMutator implements Mutator {

    private final PluginConfig config;
    private final MembershipCache membership;

    public StrengthIiiMutator(PluginConfig config, MembershipCache membership) {
        this.config = config;
        this.membership = membership;
    }

    @Override
    public String id() {
        return MutatorId.STRENGTH_III.id();
    }

    @Override
    public String displayName() {
        return MutatorId.STRENGTH_III.displayName();
    }

    @Override
    public void start() {
        applyAll();
    }

    @Override
    public void tick() {
        applyAll();
    }

    @Override
    public void onEnter(Player player) {
        apply(player);
    }

    @Override
    public void onExit(Player player) {
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }

    @Override
    public void stop() {
        for (UUID id : membership.members()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                onExit(player);
            }
        }
    }

    private void applyAll() {
        for (UUID id : membership.members()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                apply(player);
            }
        }
    }

    private void apply(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.STRENGTH, 60, config.strengthAmplifier(), true, false, true));
    }
}
