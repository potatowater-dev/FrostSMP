package com.frostsmp.autogg;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.Optional;
import java.util.UUID;

public final class QuickMsgAction {

    private QuickMsgAction() {
    }

    public static void send(MinecraftClient client) {
        ClientPlayerEntity self = client.player;
        if (self == null || client.world == null) {
            return;
        }

        AutoGgConfig config = AutoGgClient.config();

        if (self.isSpectator()
                || (client.interactionManager != null
                && client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)) {
            return;
        }

        // Anti-spam first: skip target resolution and world scans while cooling down.
        int remaining = AntiSpamService.cooldownRemainingSeconds(config);
        if (remaining > 0) {
            feedback(self, config, Text.translatable("autogg.feedback.cooldown", remaining).formatted(Formatting.YELLOW));
            return;
        }

        Optional<LastHitTracker.Target> resolved = LastHitTracker.resolve(config);
        if (resolved.isEmpty()) {
            feedback(self, config, Text.translatable("autogg.feedback.none").formatted(Formatting.RED));
            return;
        }

        LastHitTracker.Target target = resolved.get();
        UUID targetId = target.id();
        String playerName = stripIllegal(target.name());
        if (playerName.isBlank() || !isValidUsername(playerName)) {
            feedback(self, config, Text.translatable("autogg.feedback.none").formatted(Formatting.RED));
            return;
        }

        if (targetId != null && targetId.equals(self.getUuid())) {
            feedback(self, config, Text.translatable("autogg.feedback.none").formatted(Formatting.RED));
            return;
        }

        if (AntiSpamService.isEngagementLimitReached(config, targetId)) {
            feedback(self, config, Text.translatable("autogg.feedback.already_sent").formatted(Formatting.YELLOW));
            return;
        }

        if (AntiSpamService.isSameTargetBlocked(config, targetId)) {
            remaining = AntiSpamService.cooldownRemainingSeconds(config);
            feedback(self, config, Text.translatable("autogg.feedback.cooldown", Math.max(1, remaining)).formatted(Formatting.YELLOW));
            return;
        }

        PlayerEntity online = targetId == null ? null : findOnline(client, targetId);
        if (online != null && !online.isAlive() && config.requirePlayerOnline) {
            feedback(self, config, Text.translatable("autogg.feedback.offline").formatted(Formatting.RED));
            return;
        }
        if (config.requirePlayerOnline && online == null) {
            feedback(self, config, Text.translatable("autogg.feedback.offline").formatted(Formatting.RED));
            return;
        }

        if (config.maxDistance > 0 && online != null) {
            double distSq = self.squaredDistanceTo(online);
            if (distSq > (double) config.maxDistance * config.maxDistance) {
                feedback(self, config, Text.translatable("autogg.feedback.too_far").formatted(Formatting.RED));
                return;
            }
        }

        String message = stripIllegal(config.message == null ? "" : config.message.trim());
        String template = config.commandTemplate == null || config.commandTemplate.isBlank()
                ? "/msg {player} {message}"
                : config.commandTemplate.trim();

        if (!templateContainsPlayerPlaceholder(template)) {
            feedback(self, config, Text.translatable("autogg.feedback.bad_template").formatted(Formatting.RED));
            return;
        }

        String filled = fillTemplate(template, playerName, message, targetId);
        filled = stripIllegal(filled).trim();
        if (filled.isBlank()) {
            feedback(self, config, Text.translatable("autogg.feedback.bad_command").formatted(Formatting.RED));
            return;
        }

        ClientPlayNetworkHandler network = self.networkHandler;
        if (network == null) {
            return;
        }

        AutoGgConfig.SendMode mode = config.sendMode == null ? AutoGgConfig.SendMode.COMMAND : config.sendMode;
        boolean sent;
        if (mode == AutoGgConfig.SendMode.CHAT) {
            String chat = filled.startsWith("/") ? filled : "/" + filled;
            if (chat.length() <= 1) {
                feedback(self, config, Text.translatable("autogg.feedback.bad_command").formatted(Formatting.RED));
                return;
            }
            network.sendChatMessage(chat);
            sent = true;
        } else {
            String command = filled.startsWith("/") ? filled.substring(1) : filled;
            if (command.isBlank()) {
                feedback(self, config, Text.translatable("autogg.feedback.bad_command").formatted(Formatting.RED));
                return;
            }
            network.sendChatCommand(command);
            sent = true;
        }

        if (!sent) {
            return;
        }

        AntiSpamService.recordSuccessfulSend(targetId);
        feedback(self, config, Text.translatable("autogg.feedback.sent", playerName).formatted(Formatting.GREEN));
    }

    static boolean templateContainsPlayerPlaceholder(String template) {
        if (template == null) {
            return false;
        }
        return template.contains("{player}")
                || template.contains("{Player}")
                || template.contains("{PLAYER}");
    }

    static String fillTemplate(String template, String playerName, String message, UUID uuid) {
        String uuidStr = uuid == null ? "" : uuid.toString();
        String filled = template
                .replace("{player}", playerName)
                .replace("{Player}", playerName)
                .replace("{PLAYER}", playerName)
                .replace("{message}", message)
                .replace("{Message}", message)
                .replace("{MESSAGE}", message)
                .replace("{uuid}", uuidStr)
                .replace("{UUID}", uuidStr);
        return collapseSpaces(filled).trim();
    }

    static String collapseSpaces(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        boolean inSpace = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c <= ' ') {
                if (!inSpace && !sb.isEmpty()) {
                    sb.append(' ');
                    inSpace = true;
                }
            } else {
                sb.append(c);
                inSpace = false;
            }
        }
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == ' ') {
            end--;
        }
        return end == sb.length() ? sb.toString() : sb.substring(0, end);
    }

    private static PlayerEntity findOnline(MinecraftClient client, UUID id) {
        if (client.world == null) {
            return null;
        }
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player != null && player.getUuid().equals(id)) {
                return player;
            }
        }
        return null;
    }

    static String stripIllegal(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String out = input.replace("\u00A7", "");
        out = stripAmpCodes(out);
        return out;
    }

    private static String stripAmpCodes(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < input.length()) {
                char n = Character.toLowerCase(input.charAt(i + 1));
                if ((n >= '0' && n <= '9') || (n >= 'a' && n <= 'f') || n == 'k' || n == 'l' || n == 'm' || n == 'n' || n == 'o' || n == 'r') {
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    static boolean isValidUsername(String name) {
        if (name.length() < 1 || name.length() > 32) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z')
                    && !(c >= 'A' && c <= 'Z')
                    && !(c >= '0' && c <= '9')
                    && c != '_'
                    && c != '.') {
                return false;
            }
        }
        return true;
    }

    private static void feedback(ClientPlayerEntity self, AutoGgConfig config, Text text) {
        if (!config.feedbackEnabled) {
            return;
        }
        self.sendMessage(text, true);
    }
}
