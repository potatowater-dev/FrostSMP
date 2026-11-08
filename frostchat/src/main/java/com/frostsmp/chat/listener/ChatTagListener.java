package com.frostsmp.chat.listener;

import com.frostsmp.chat.FrostChatPlugin;
import com.frostsmp.chat.service.EnderChestSnapshotService;
import com.frostsmp.chat.util.MessageUtil;
import com.frostsmp.chat.util.PlaceholderUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Tagged chat must not rely on LPC leaving {@link AsyncChatEvent} alone.
 * Cancel early, then broadcast our formatted line on the main thread
 * (same path as {@code /frostchat test}).
 */
public final class ChatTagListener implements Listener {

    private static final Pattern TAG_PATTERN = Pattern.compile(
            "\\[(wallet|item|i|ec|enderchest|inv|inventory)]",
            Pattern.CASE_INSENSITIVE
    );

    private final FrostChatPlugin plugin;

    public ChatTagListener(FrostChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("frostsmp.chat.use")) {
            return;
        }

        String raw = plainMessage(event);
        if (!containsSupportedTag(raw)) {
            return;
        }

        // Snapshot recipients, then stop LPC / Discord / others from delivering literal tags.
        Set<Audience> recipients = new LinkedHashSet<>(event.viewers());
        event.setCancelled(true);
        event.viewers().clear();
        if (recipients.isEmpty()) {
            recipients.add(Bukkit.getConsoleSender());
            recipients.addAll(Bukkit.getOnlinePlayers());
        }

        UUID playerId = player.getUniqueId();
        String message = raw;

        if (plugin.getConfig().getBoolean("debug-tags", false)) {
            plugin.getLogger().info("FrostChat intercept (cancel+resend) for "
                    + player.getName() + ": " + raw);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !online.isOnline()) {
                return;
            }
            Component line;
            try {
                line = buildTaggedLine(online, message);
            } catch (Exception exception) {
                plugin.getLogger().warning("FrostChat tag render failed for " + online.getName()
                        + ": " + exception.getMessage());
                // Fallback: still show something so chat isn't swallowed.
                online.sendMessage(Component.text(online.getName() + ": " + message));
                return;
            }
            for (Audience audience : recipients) {
                audience.sendMessage(line);
            }
        });
    }

    /** Builds a tagged chat line for /frostchat test (no event required). */
    public Component buildTaggedLine(Player player, String raw) {
        Component body = replaceTags(player, normalizeBrackets(raw));
        return formatChat(player, body);
    }

    public static boolean containsSupportedTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        return TAG_PATTERN.matcher(normalizeBrackets(raw)).find();
    }

    private static String plainMessage(AsyncChatEvent event) {
        String fromMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        String fromOriginal = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        String raw = containsSupportedTag(fromMessage) ? fromMessage : fromOriginal;
        return normalizeBrackets(raw);
    }

    private static String normalizeBrackets(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input
                .replace('\uFF3B', '[')
                .replace('\uFF3D', ']')
                .replace('【', '[')
                .replace('】', ']');
    }

    private Component formatChat(Player player, Component body) {
        String name = player.getName() == null ? "Unknown" : player.getName();
        boolean legacySafe = plugin.getConfig().getBoolean("chat-format-legacy-safe", true);
        String template = plugin.getConfig().getString("chat-format", "");

        if (legacySafe || template == null || template.isBlank()) {
            return lpcStyleFallback(player, body);
        }

        try {
            String resolved = template.replace("{prefix}", "%luckperms_prefix%");
            resolved = MessageUtil.format(resolved, "name", name);
            resolved = PlaceholderUtil.apply(player, resolved);
            return MessageUtil.parseSafe(resolved).append(body);
        } catch (Exception ignored) {
            return lpcStyleFallback(player, body);
        }
    }

    private Component lpcStyleFallback(Player player, Component body) {
        String name = player.getName() == null ? "Unknown" : player.getName();
        String format = plugin.getConfig().getString("lpc-fallback-format", "");
        if (format == null || format.isBlank()) {
            format = "%luckperms_prefix%{name}&7: &f";
        }
        format = MessageUtil.format(format, "name", name, "player", name);
        format = PlaceholderUtil.apply(player, format);
        return MessageUtil.parseLegacy(format).append(body);
    }

    private Component replaceTags(Player player, String raw) {
        Component result = Component.empty();
        int index = 0;
        while (index < raw.length()) {
            int open = raw.indexOf('[', index);
            if (open < 0) {
                return result.append(Component.text(raw.substring(index)));
            }
            if (open > index) {
                result = result.append(Component.text(raw.substring(index, open)));
            }
            int close = raw.indexOf(']', open + 1);
            if (close < 0) {
                return result.append(Component.text(raw.substring(open)));
            }
            String token = raw.substring(open, close + 1);
            result = result.append(replaceToken(player, token));
            index = close + 1;
        }
        return result;
    }

    private Component replaceToken(Player player, String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "[wallet]" -> walletComponent(player);
            case "[item]", "[i]" -> itemComponent(player);
            case "[ec]", "[enderchest]" -> enderChestComponent(player);
            case "[inv]", "[inventory]" -> inventoryComponent(player);
            default -> Component.text(token);
        };
    }

    private Component walletComponent(Player player) {
        return MessageUtil.parseSafe(plugin.msg("wallet-label"))
                .hoverEvent(plugin.getWalletPreviews().buildWalletHover(player.getUniqueId(), player.getName()))
                .clickEvent(ClickEvent.runCommand("/frostchat viewwallet " + player.getUniqueId()));
    }

    private Component itemComponent(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return MessageUtil.parseSafe(plugin.msg("item-empty"));
        }
        return MessageUtil.parseSafe(plugin.msg("item-label")).hoverEvent(item.asHoverEvent());
    }

    private Component enderChestComponent(Player player) {
        String id = plugin.getSnapshots().createSnapshot(player.getName(), player.getEnderChest().getContents());
        String hover = MessageUtil.format(plugin.msg("ec-hover"), "player", player.getName());
        return MessageUtil.parseSafe(plugin.msg("ec-label"))
                .hoverEvent(HoverEvent.showText(MessageUtil.parseSafe(hover)))
                .clickEvent(ClickEvent.runCommand("/frostchat viewec " + id));
    }

    private Component inventoryComponent(Player player) {
        String id = plugin.getSnapshots().createSnapshot(
                player.getName(),
                player.getInventory().getContents(),
                EnderChestSnapshotService.ViewKind.INVENTORY
        );
        String hover = MessageUtil.format(
                firstNonBlank(plugin.msg("inv-hover"), "<gray>Click to view</gray> <white>{player}</white><gray>'s inventory</gray>"),
                "player", player.getName()
        );
        String label = firstNonBlank(plugin.msg("inv-label"), "<green><underlined>[Inventory]</underlined></green>");
        return MessageUtil.parseSafe(label)
                .hoverEvent(HoverEvent.showText(MessageUtil.parseSafe(hover)))
                .clickEvent(ClickEvent.runCommand("/frostchat viewinv " + id));
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
