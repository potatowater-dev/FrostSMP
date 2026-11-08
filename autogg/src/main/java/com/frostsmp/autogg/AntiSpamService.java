package com.frostsmp.autogg;

import java.util.UUID;

public final class AntiSpamService {

    private static long lastSendAtMs;
    private static UUID lastSentTargetId;
    private static UUID engagementTargetId;
    private static int engagementSends;

    private AntiSpamService() {
    }

    public static void clear() {
        lastSendAtMs = 0L;
        lastSentTargetId = null;
        engagementTargetId = null;
        engagementSends = 0;
    }

    /** Called when a new dealt or taken hit is remembered for a target. */
    public static void onHitRemembered(UUID targetId) {
        if (targetId == null) {
            return;
        }
        if (!targetId.equals(engagementTargetId)) {
            engagementTargetId = targetId;
            engagementSends = 0;
            return;
        }
        // Same target hit again refreshes the engagement send budget.
        engagementSends = 0;
    }

    /**
     * @return remaining cooldown seconds (ceil), or 0 if allowed
     */
    public static int cooldownRemainingSeconds(AutoGgConfig config) {
        if (config == null || !config.antiSpamEnabled) {
            return 0;
        }
        int cooldown = clampCooldown(config.cooldownSeconds);
        if (cooldown <= 0 || lastSendAtMs <= 0L) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - lastSendAtMs;
        long windowMs = cooldown * 1000L;
        if (elapsed >= windowMs) {
            return 0;
        }
        long remainingMs = windowMs - elapsed;
        return (int) Math.max(1L, (remainingMs + 999L) / 1000L);
    }

    public static boolean isOnCooldown(AutoGgConfig config) {
        return cooldownRemainingSeconds(config) > 0;
    }

    /**
     * Same-target burst lock inside the cooldown window.
     * With a global cooldown this is already covered; kept explicit for clarity.
     */
    public static boolean isSameTargetBlocked(AutoGgConfig config, UUID targetId) {
        if (config == null || !config.antiSpamEnabled || targetId == null) {
            return false;
        }
        if (!isOnCooldown(config)) {
            return false;
        }
        return targetId.equals(lastSentTargetId);
    }

    public static boolean isEngagementLimitReached(AutoGgConfig config, UUID targetId) {
        if (config == null || targetId == null) {
            return false;
        }
        int max = Math.max(0, config.maxSendsPerHit);
        if (max <= 0) {
            return false;
        }
        if (!targetId.equals(engagementTargetId)) {
            return false;
        }
        return engagementSends >= max;
    }

    public static void recordSuccessfulSend(UUID targetId) {
        lastSendAtMs = System.currentTimeMillis();
        lastSentTargetId = targetId;
        if (targetId != null && targetId.equals(engagementTargetId)) {
            engagementSends++;
        } else if (targetId != null) {
            engagementTargetId = targetId;
            engagementSends = 1;
        }
    }

    static int clampCooldown(int seconds) {
        if (seconds < 0) {
            return 0;
        }
        return Math.min(60, seconds);
    }
}
