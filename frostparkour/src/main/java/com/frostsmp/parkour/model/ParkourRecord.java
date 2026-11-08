package com.frostsmp.parkour.model;

public record ParkourRecord(String playerName, long timeMs, long timestamp)
        implements Comparable<ParkourRecord> {

    @Override
    public int compareTo(ParkourRecord o) {
        int cmp = Long.compare(this.timeMs, o.timeMs);
        if (cmp != 0) {
            return cmp;
        }
        return Long.compare(this.timestamp, o.timestamp);
    }

    public String nameKey() {
        return playerName == null ? "" : playerName.toLowerCase(java.util.Locale.ROOT);
    }
}
