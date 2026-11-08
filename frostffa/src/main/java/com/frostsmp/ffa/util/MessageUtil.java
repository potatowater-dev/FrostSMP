package com.frostsmp.ffa.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class MessageUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    public static void broadcast(String message) {
        Component component = parse(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(component);
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

    public static String mmss(long millis) {
        long total = Math.max(0L, millis / 1000L);
        long hours = total / 3600L;
        long minutes = (total % 3600L) / 60L;
        long seconds = total % 60L;
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    public static void broadcastTitle(String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        Title adventure = Title.title(
                parse(title),
                parse(subtitle),
                Title.Times.times(
                        Duration.ofMillis(Math.max(0, fadeInTicks) * 50L),
                        Duration.ofMillis(Math.max(1, stayTicks) * 50L),
                        Duration.ofMillis(Math.max(0, fadeOutTicks) * 50L)
                )
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(adventure);
        }
    }

    public static void showTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        player.showTitle(Title.title(
                parse(title),
                parse(subtitle),
                Title.Times.times(
                        Duration.ofMillis(Math.max(0, fadeInTicks) * 50L),
                        Duration.ofMillis(Math.max(1, stayTicks) * 50L),
                        Duration.ofMillis(Math.max(0, fadeOutTicks) * 50L)
                )
        ));
    }
}
