package com.frostsmp.chat.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private MessageUtil() {
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parseSafe(message));
    }

    public static Component parse(String input) {
        return parseSafe(input);
    }

    /**
     * Never throws and never leaves broken MiniMessage dumps on screen.
     * Order: valid MiniMessage → LuckPerms-style {@code &}/{@code §} legacy → plain text.
     */
    public static Component parseSafe(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String normalized = input.replace('§', '&');

        // Pure legacy (typical LuckPerms prefix) — do not run through MiniMessage.
        if (!looksLikeMiniMessage(normalized)) {
            return parseLegacy(normalized);
        }

        try {
            String forMini = normalized.indexOf('&') >= 0 ? ampersandToMini(normalized) : normalized;
            return MINI.deserialize(forMini);
        } catch (Exception ignored) {
            // Broken MiniMessage used to fall through to legacy and show raw <tags>.
            // Strip failed tags, then apply any remaining & codes (LPC-style).
            return parseLegacy(stripMiniTags(normalized));
        }
    }

    /** LuckPerms / LPC prefixes — legacy only. */
    public static Component parseLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String normalized = input.replace('§', '&');
        try {
            return LEGACY.deserialize(normalized);
        } catch (Exception ignored) {
            try {
                return LEGACY_SECTION.deserialize(input);
            } catch (Exception ignoredAgain) {
                return Component.text(stripMiniTags(normalized));
            }
        }
    }

    public static String format(String template, String... keyValues) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            result = result.replace("{" + keyValues[i] + "}", keyValues[i + 1] == null ? "" : keyValues[i + 1]);
        }
        return result;
    }

    public static boolean looksLikeMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        int open = input.indexOf('<');
        if (open < 0) {
            return false;
        }
        int close = input.indexOf('>', open + 1);
        if (close < 0) {
            return false;
        }
        // <red>, </#abc>, <#RRGGBB>, </red>, <bold>
        char next = input.charAt(open + 1);
        return Character.isLetter(next) || next == '#' || next == '/';
    }

    public static String stripMiniTags(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.replaceAll("<[^>]*>", "");
    }

    /** Converts common & / &#RRGGBB codes so they work inside MiniMessage templates. */
    public static String ampersandToMini(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c != '&' || i + 1 >= input.length()) {
                out.append(c);
                continue;
            }
            char code = input.charAt(i + 1);
            if (code == '#' && i + 7 < input.length()) {
                String hex = input.substring(i + 2, i + 8);
                if (hex.chars().allMatch(ch -> Character.digit(ch, 16) >= 0)) {
                    out.append("<#").append(hex).append('>');
                    i += 7;
                    continue;
                }
            }
            String tag = switch (Character.toLowerCase(code)) {
                case '0' -> "<black>";
                case '1' -> "<dark_blue>";
                case '2' -> "<dark_green>";
                case '3' -> "<dark_aqua>";
                case '4' -> "<dark_red>";
                case '5' -> "<dark_purple>";
                case '6' -> "<gold>";
                case '7' -> "<gray>";
                case '8' -> "<dark_gray>";
                case '9' -> "<blue>";
                case 'a' -> "<green>";
                case 'b' -> "<aqua>";
                case 'c' -> "<red>";
                case 'd' -> "<light_purple>";
                case 'e' -> "<yellow>";
                case 'f' -> "<white>";
                case 'k' -> "<obfuscated>";
                case 'l' -> "<bold>";
                case 'm' -> "<strikethrough>";
                case 'n' -> "<underlined>";
                case 'o' -> "<italic>";
                case 'r' -> "<reset>";
                default -> null;
            };
            if (tag == null) {
                out.append(c);
                continue;
            }
            out.append(tag);
            i++;
        }
        return out.toString();
    }
}
