package com.frostsmp.crates.util;

import org.bukkit.Material;

public final class CandleColorUtil {

    private CandleColorUtil() {
    }

    /** MiniMessage color name for bold action-bar text matching the crate candle. */
    public static String miniMessageColor(Material candle) {
        if (candle == null) {
            return "green";
        }
        return switch (candle) {
            case WHITE_CANDLE -> "white";
            case ORANGE_CANDLE -> "gold";
            case MAGENTA_CANDLE, PINK_CANDLE -> "light_purple";
            case LIGHT_BLUE_CANDLE, CYAN_CANDLE -> "aqua";
            case YELLOW_CANDLE -> "yellow";
            case LIME_CANDLE -> "green";
            case GRAY_CANDLE, LIGHT_GRAY_CANDLE -> "gray";
            case PURPLE_CANDLE -> "dark_purple";
            case BLUE_CANDLE -> "blue";
            case BROWN_CANDLE -> "gold";
            case GREEN_CANDLE -> "dark_green";
            case RED_CANDLE -> "red";
            case BLACK_CANDLE -> "dark_gray";
            default -> {
                String name = candle.name();
                if (name.endsWith("_CANDLE")) {
                    yield name.substring(0, name.length() - 7).toLowerCase();
                }
                yield "green";
            }
        };
    }

    public static String rollingActionBarTag(Material candle) {
        return "<" + miniMessageColor(candle) + "><bold>Rolling...</bold>";
    }
}
