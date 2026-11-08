package com.frostsmp.parkour.util;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static String formatTime(long millis) {
        long safe = Math.max(0L, millis);
        long minutes = (safe / 1000) / 60;
        long seconds = (safe / 1000) % 60;
        long ms = safe % 1000;

        return String.format("%02d:%02d:%03d", minutes, seconds, ms);
    }
}

