package com.frostsmp.killeffect.service;

import com.frostsmp.killeffect.config.KillEffectConfig;
import com.frostsmp.killeffect.model.KillEffectDefinition;
import com.frostsmp.killeffect.storage.PlayerEffectStorage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class KillEffectService {

    private final KillEffectConfig config;
    private final PlayerEffectStorage storage;

    public KillEffectService(KillEffectConfig config, PlayerEffectStorage storage) {
        this.config = config;
        this.storage = storage;
    }

    public void playForKiller(Player killer, Location at) {
        if (killer == null || at == null || at.getWorld() == null) {
            return;
        }
        String selected = storage.selected(killer.getUniqueId());
        if (selected == null || selected.isBlank()) {
            return;
        }
        if (!storage.hasClaimed(killer.getUniqueId(), selected)) {
            return;
        }
        KillEffectDefinition effect = config.effect(selected);
        if (effect == null) {
            return;
        }
        play(effect.id(), at);
    }

    public void play(String effectId, Location at) {
        World world = at.getWorld();
        if (world == null) {
            return;
        }
        Location center = at.clone().add(0.0, 1.0, 0.0);
        switch (effectId.toLowerCase()) {
            case "lightning" -> {
                world.strikeLightningEffect(center);
                world.spawnParticle(Particle.ELECTRIC_SPARK, center, 40, 0.6, 1.0, 0.6, 0.15);
                world.spawnParticle(Particle.END_ROD, center, 20, 0.4, 0.8, 0.4, 0.05);
                world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.2f);
            }
            case "explosion" -> {
                world.spawnParticle(Particle.EXPLOSION, center, 3, 0.2, 0.2, 0.2, 0.0);
                world.spawnParticle(Particle.SMOKE, center, 35, 0.7, 0.7, 0.7, 0.05);
                world.spawnParticle(Particle.LARGE_SMOKE, center, 15, 0.5, 0.5, 0.5, 0.02);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.55f, 1.1f);
            }
            case "blood" -> {
                world.spawnParticle(Particle.DAMAGE_INDICATOR, center, 25, 0.5, 0.8, 0.5, 0.1);
                world.spawnParticle(Particle.BLOCK, center, 50, 0.5, 0.8, 0.5, 0.05, org.bukkit.Material.REDSTONE_BLOCK.createBlockData());
                world.spawnParticle(Particle.DUST, center, 30, 0.5, 0.7, 0.5, 0.0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(140, 0, 0), 1.4f));
                world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 0.7f);
            }
            case "frost" -> {
                world.spawnParticle(Particle.SNOWFLAKE, center, 50, 0.7, 1.0, 0.7, 0.02);
                world.spawnParticle(Particle.ITEM_SNOWBALL, center, 25, 0.5, 0.8, 0.5, 0.1);
                world.spawnParticle(Particle.SOUL, center, 15, 0.4, 0.6, 0.4, 0.01);
                world.playSound(center, Sound.BLOCK_POWDER_SNOW_BREAK, 1.0f, 0.6f);
            }
            case "flame" -> {
                world.spawnParticle(Particle.FLAME, center, 45, 0.6, 0.9, 0.6, 0.03);
                world.spawnParticle(Particle.LAVA, center, 12, 0.4, 0.5, 0.4, 0.0);
                world.spawnParticle(Particle.SMOKE, center, 20, 0.5, 0.7, 0.5, 0.02);
                world.playSound(center, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.0f);
            }
            default -> {
            }
        }
    }
}
