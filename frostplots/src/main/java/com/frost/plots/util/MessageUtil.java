package com.frost.plots.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        if (input.contains("<") && input.contains(">")) {
            return MINI.deserialize(input);
        }
        return LEGACY.deserialize(input);
    }

    public static void send(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(parse(message));
    }

    public static String format(String template, String... replacements) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return result;
    }

    public static void send(CommandSender sender, String template, String... replacements) {
        send(sender, format(template, replacements));
    }

    public static void sendPrefixed(CommandSender sender, String prefix, String template, String... replacements) {
        send(sender, prefix + format(template, replacements));
    }

    public static String nameOf(Player player) {
        return player == null ? "Unknown" : player.getName();
    }
}
