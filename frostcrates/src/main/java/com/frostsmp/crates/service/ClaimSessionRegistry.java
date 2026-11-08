package com.frostsmp.crates.service;

import com.frostsmp.crates.gui.CrateClaimGui;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.LootEntry;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClaimSessionRegistry {

    public static final class Session {
        private final CrateClaimGui gui;
        private final CrateDefinition definition;
        private final LootEntry reward;
        private volatile boolean rolling;
        private volatile boolean rewardGranted;
        /** When true, close/reopen keep-alive must not fight an intentional close. */
        private volatile boolean suppressReopen;
        /** Escape/close attempts while rolling — used to settle instead of reopen-fighting forever. */
        private volatile int closeAttempts;
        private BukkitRunnable animationTask;

        public Session(CrateClaimGui gui, CrateDefinition definition, LootEntry reward) {
            this.gui = gui;
            this.definition = definition;
            this.reward = reward;
            this.rolling = true;
        }

        public CrateClaimGui getGui() {
            return gui;
        }

        public CrateDefinition getDefinition() {
            return definition;
        }

        public LootEntry getReward() {
            return reward;
        }

        public boolean isRolling() {
            return rolling;
        }

        public boolean isRewardGranted() {
            return rewardGranted;
        }

        /** Returns true only the first time — prevents escape-spam duplicate claims. */
        public synchronized boolean tryMarkRewardGranted() {
            if (rewardGranted) {
                return false;
            }
            rewardGranted = true;
            return true;
        }

        public boolean isSuppressReopen() {
            return suppressReopen;
        }

        public void setSuppressReopen(boolean suppressReopen) {
            this.suppressReopen = suppressReopen;
        }

        public int incrementCloseAttempts() {
            return ++closeAttempts;
        }

        public int getCloseAttempts() {
            return closeAttempts;
        }

        public void finishRolling() {
            rolling = false;
        }

        public void setAnimationTask(BukkitRunnable animationTask) {
            this.animationTask = animationTask;
        }

        public void cancelAnimation() {
            if (animationTask != null) {
                animationTask.cancel();
                animationTask = null;
            }
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public void start(Player player, CrateClaimGui gui, CrateDefinition definition, LootEntry reward) {
        sessions.put(player.getUniqueId(), new Session(gui, definition, reward));
    }

    public Session get(Player player) {
        if (player == null) {
            return null;
        }
        return sessions.get(player.getUniqueId());
    }

    public Session get(java.util.UUID playerId) {
        return sessions.get(playerId);
    }

    public boolean isRolling(Player player) {
        Session session = sessions.get(player.getUniqueId());
        return session != null && session.isRolling();
    }

    public void end(Player player) {
        if (player == null) {
            return;
        }
        end(player.getUniqueId());
    }

    public void end(java.util.UUID playerId) {
        Session session = sessions.remove(playerId);
        if (session != null) {
            session.cancelAnimation();
        }
    }

    public java.util.Collection<java.util.UUID> getActivePlayers() {
        return java.util.List.copyOf(sessions.keySet());
    }
}
