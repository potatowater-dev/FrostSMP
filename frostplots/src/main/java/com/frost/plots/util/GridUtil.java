package com.frost.plots.util;

public final class GridUtil {

    private GridUtil() {
    }

    /**
     * Stable square-spiral mapping: 0 -> (0,0), 1 -> (1,0), then around the ring.
     */
    public static int[] indexToOffset(int index) {
        if (index <= 0) {
            return new int[]{0, 0};
        }
        int radius = (int) Math.ceil((Math.sqrt(index + 1.0) - 1.0) / 2.0);
        int ringStart = (2 * radius - 1) * (2 * radius - 1);
        int offset = index - ringStart;
        int sideLen = 2 * radius;
        int side = offset / sideLen;
        int pos = offset % sideLen;
        return switch (side) {
            case 0 -> new int[]{radius, -radius + 1 + pos};
            case 1 -> new int[]{radius - 1 - pos, radius};
            case 2 -> new int[]{-radius, radius - 1 - pos};
            default -> new int[]{-radius + 1 + pos, -radius};
        };
    }

    public static long pack(int gridX, int gridZ) {
        return (((long) gridX) << 32) ^ (gridZ & 0xffffffffL);
    }
}
