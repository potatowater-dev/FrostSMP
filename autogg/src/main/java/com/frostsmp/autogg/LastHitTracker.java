package com.frostsmp.autogg;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Tracks both the last player you hit (dealt) and the last who hit you (taken).
 * Resolution prefers dealt, then falls back to taken when enabled.
 */
public final class LastHitTracker {

    public enum Source {
        DEALT,
        TAKEN
    }

    public record Target(UUID id, String name, Source source) {
    }

    private static UUID dealtId;
    private static String dealtName;
    private static long dealtAtMs;

    private static UUID takenId;
    private static String takenName;
    private static long takenAtMs;

    private LastHitTracker() {
    }

    /** @deprecated Use {@link #rememberDealt(PlayerEntity)} */
    public static void remember(PlayerEntity target) {
        rememberDealt(target);
    }

    public static void rememberDealt(PlayerEntity target) {
        rememberSide(target, true);
    }

    public static void rememberTaken(PlayerEntity attacker) {
        rememberSide(attacker, false);
    }

    private static void rememberSide(PlayerEntity player, boolean dealt) {
        if (player == null) {
            return;
        }
        String name = player.getGameProfile().name();
        if (name == null || name.isBlank()) {
            return;
        }
        UUID id = player.getUuid();
        long now = System.currentTimeMillis();
        if (dealt) {
            dealtId = id;
            dealtName = name;
            dealtAtMs = now;
        } else {
            takenId = id;
            takenName = name;
            takenAtMs = now;
        }
        AntiSpamService.onHitRemembered(id);
    }

    public static void clear() {
        dealtId = null;
        dealtName = null;
        dealtAtMs = 0L;
        takenId = null;
        takenName = null;
        takenAtMs = 0L;
    }

    public static Optional<Target> resolve(AutoGgConfig config) {
        int memory = config == null ? 120 : config.hitMemorySeconds;
        boolean allowFallback = config == null || config.allowLastAttackerFallback;

        if (isValid(dealtId, dealtName, dealtAtMs, memory)) {
            return Optional.of(new Target(dealtId, dealtName, Source.DEALT));
        }
        if (allowFallback && isValid(takenId, takenName, takenAtMs, memory)) {
            return Optional.of(new Target(takenId, takenName, Source.TAKEN));
        }
        return Optional.empty();
    }

    public static boolean hasTarget(int memorySeconds) {
        return isValid(dealtId, dealtName, dealtAtMs, memorySeconds)
                || isValid(takenId, takenName, takenAtMs, memorySeconds);
    }

    public static UUID getLastHitId() {
        return dealtId;
    }

    public static String getLastHitName() {
        return dealtName;
    }

    private static boolean isValid(UUID id, String name, long atMs, int memorySeconds) {
        if (id == null || name == null || name.isBlank()) {
            return false;
        }
        if (memorySeconds > 0) {
            long ageMs = System.currentTimeMillis() - atMs;
            return ageMs <= memorySeconds * 1000L;
        }
        return true;
    }
}
