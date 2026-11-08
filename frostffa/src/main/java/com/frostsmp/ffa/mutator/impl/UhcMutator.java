package com.frostsmp.ffa.mutator.impl;

import com.frostsmp.ffa.arena.MembershipCache;
import com.frostsmp.ffa.mutator.Mutator;
import com.frostsmp.ffa.mutator.MutatorId;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class UhcMutator implements Mutator, Listener {

    private final JavaPlugin plugin;
    private final MembershipCache membership;
    private boolean active;

    public UhcMutator(JavaPlugin plugin, MembershipCache membership) {
        this.plugin = plugin;
        this.membership = membership;
    }

    @Override
    public String id() {
        return MutatorId.UHC.id();
    }

    @Override
    public String displayName() {
        return MutatorId.UHC.displayName();
    }

    @Override
    public void start() {
        active = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void stop() {
        active = false;
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (!active || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!membership.isInArena(player)) {
            return;
        }
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
                || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
            event.setCancelled(true);
        }
    }
}
