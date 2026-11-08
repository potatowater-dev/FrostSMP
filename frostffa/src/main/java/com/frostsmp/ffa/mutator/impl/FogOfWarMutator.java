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

import java.util.UUID;

public final class FogOfWarMutator implements Mutator {

    private static final int MAX_STAGE = 3;
    private static final int REFRESH_TICKS = 40;
    private static final int EFFECT_TICKS = 80;

    private final PluginConfig config;
    private final MembershipCache membership;
    private long startedAt;
    private int stage = 1;
    private int tick;

    public FogOfWarMutator(PluginConfig config, MembershipCache membership) {
        this.config = config;
        this.membership = membership;
    }

    @Override
    public String id() {
        return MutatorId.FOG_OF_WAR.id();
    }

    @Override
    public String displayName() {
        return MutatorId.FOG_OF_WAR.displayName();
    }

    @Override
    public void start() {
        startedAt = System.currentTimeMillis();
        stage = 1;
        tick = 0;
        applyAll();
    }

    @Override
    public void tick() {
        tick++;
        int next = currentStage();
        if (next != stage) {
            stage = next;
            applyAll();
            announceStage(stage);
            return;
        }
        if (tick % REFRESH_TICKS == 0) {
            applyAll();
        }
    }

    @Override
    public void onEnter(Player player) {
        apply(player);
    }

    @Override
    public void onExit(Player player) {
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
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

    private int currentStage() {
        long elapsedMin = Math.max(0L, System.currentTimeMillis() - startedAt) / 60_000L;
        int step = config.fogStageMinutes();
        return (int) Math.min(MAX_STAGE, 1L + elapsedMin / step);
    }

    private void announceStage(int stage) {
        String roman = toRoman(stage);
        MessageUtil.broadcastTitle(
                MessageUtil.format(config.fogStageTitle(stage), "stage", String.valueOf(stage), "roman", roman),
                MessageUtil.format(config.fogStageSubtitle(stage), "stage", String.valueOf(stage), "roman", roman),
                config.titleFadeInTicks(),
                config.titleStayTicks(),
                config.titleFadeOutTicks()
        );
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
        int amplifier = Math.max(0, stage - 1);
        PotionEffect current = player.getPotionEffect(PotionEffectType.DARKNESS);
        if (current != null && current.getAmplifier() != amplifier) {
            player.removePotionEffect(PotionEffectType.DARKNESS);
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, EFFECT_TICKS, amplifier, true, false, true));
    }

    private static String toRoman(int stage) {
        return switch (stage) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(stage);
        };
    }
}
