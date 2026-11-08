package com.frostsmp.crates.listener;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.config.MessagesConfig;
import com.frostsmp.crates.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ChatInputListener implements Listener {

    public record PendingInput(
            Consumer<String> handler,
            long expiresAt,
            String prompt
    ) {
    }

    private final FrostCratesPlugin plugin;
    private final MessagesConfig messages;
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

    public ChatInputListener(FrostCratesPlugin plugin, MessagesConfig messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void requestInput(Player player, String prompt, Consumer<String> handler) {
        int timeout = plugin.getPluginConfig().getChatInputTimeoutSeconds();
        pending.put(player.getUniqueId(), new PendingInput(handler, System.currentTimeMillis() + timeout * 1000L, prompt));
        MessageUtil.send(player, prompt);
        messages.send(player, "chat-input-prompt");
    }

    public void clear(Player player) {
        pending.remove(player.getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        PendingInput input = pending.remove(event.getPlayer().getUniqueId());
        if (input == null) {
            return;
        }
        event.setCancelled(true);
        if (System.currentTimeMillis() > input.expiresAt()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    messages.send(event.getPlayer(), "chat-input-timeout");
                }
            }.runTask(plugin);
            return;
        }
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    messages.send(event.getPlayer(), "chat-input-cancelled");
                }
            }.runTask(plugin);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                input.handler().accept(message);
            }
        }.runTask(plugin);
    }
}
