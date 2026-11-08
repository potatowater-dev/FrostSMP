package com.frostsmp.store.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private MessageUtil() {
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return MINI.deserialize(input);
    }

    public static String format(String template, String... keyValues) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            result = result.replace("{" + keyValues[i] + "}", keyValues[i + 1]);
        }
        return result;
    }
}
