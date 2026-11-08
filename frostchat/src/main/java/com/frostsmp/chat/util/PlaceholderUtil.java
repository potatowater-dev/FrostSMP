package com.frostsmp.chat.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlaceholderUtil {

    private PlaceholderUtil() {
    }

    public static String apply(Player player, String input) {
        if (input == null || input.isEmpty() || player == null) {
            return input == null ? "" : input;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return input;
        }
        return PlaceholderAPI.setPlaceholders(player, input);
    }
}
