package com.frostsmp.crates.util;

import org.bukkit.Material;

public final class BlockMaterialUtil {

    private BlockMaterialUtil() {
    }

    public static boolean isValidDisplayBlock(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_SHULKER_BOX") || name.endsWith("_CANDLE");
    }

    public static Material normalizeDisplayBlock(Material material) {
        if (material == null) {
            return Material.LIME_SHULKER_BOX;
        }
        if (material.name().endsWith("_CANDLE")) {
            return candleToShulker(material);
        }
        if (material.name().endsWith("_SHULKER_BOX")) {
            return material;
        }
        return Material.LIME_SHULKER_BOX;
    }

    public static Material candleToShulker(Material candle) {
        if (candle == null || !candle.name().endsWith("_CANDLE")) {
            return Material.LIME_SHULKER_BOX;
        }
        String color = candle.name().substring(0, candle.name().length() - 7);
        Material shulker = Material.matchMaterial(color + "_SHULKER_BOX");
        return shulker != null ? shulker : Material.LIME_SHULKER_BOX;
    }

    public static String miniMessageColor(Material block) {
        Material normalized = normalizeDisplayBlock(block);
        return switch (normalized) {
            case WHITE_SHULKER_BOX -> "white";
            case ORANGE_SHULKER_BOX -> "gold";
            case MAGENTA_SHULKER_BOX, PINK_SHULKER_BOX -> "light_purple";
            case LIGHT_BLUE_SHULKER_BOX, CYAN_SHULKER_BOX -> "aqua";
            case YELLOW_SHULKER_BOX -> "yellow";
            case LIME_SHULKER_BOX -> "green";
            case GRAY_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX -> "gray";
            case PURPLE_SHULKER_BOX -> "dark_purple";
            case BLUE_SHULKER_BOX -> "blue";
            case BROWN_SHULKER_BOX -> "gold";
            case GREEN_SHULKER_BOX -> "dark_green";
            case RED_SHULKER_BOX -> "red";
            case BLACK_SHULKER_BOX -> "dark_gray";
            default -> "aqua";
        };
    }

    public static String rollingActionBarTag(Material block) {
        return "<" + miniMessageColor(block) + "><bold>Rolling...</bold>";
    }
}
