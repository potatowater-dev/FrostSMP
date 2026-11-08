package com.frostsmp.store.service;

import com.frostsmp.store.util.MessageUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class PurchasePoller {

    private record Pending(String id, String username, String group, String rank) {
    }

    private final JavaPlugin plugin;
    private BukkitTask task;

    public PurchasePoller(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        int seconds = Math.max(3, plugin.getConfig().getInt("poll-seconds", 8));
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::pollSafe, 40L, seconds * 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void pollSafe() {
        try {
            List<Pending> pending = fetchPending();
            if (pending.isEmpty()) {
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (Pending purchase : pending) {
                    if (grant(purchase)) {
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                claim(purchase.id());
                            } catch (Exception exception) {
                                plugin.getLogger().warning("Claim failed for " + purchase.id() + ": " + exception.getMessage());
                            }
                        });
                    }
                }
            });
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "FrostStore poll failed: " + exception.getMessage());
        }
    }

    private List<Pending> fetchPending() throws Exception {
        String apiKey = plugin.getConfig().getString("api-key", "");
        String base = plugin.getConfig().getString("api-base-url", "https://frostffa.web.app");
        if (apiKey == null || apiKey.isBlank() || apiKey.equalsIgnoreCase("CHANGE_ME")) {
            return List.of();
        }
        if (base == null || base.isBlank()) {
            return List.of();
        }
        base = base.replaceAll("/$", "");

        HttpURLConnection connection = (HttpURLConnection) URI.create(base + "/api/pending-purchases").toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        connection.setRequestProperty("Accept", "application/json");

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String body = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code + ": " + body);
        }

        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray purchases = root.has("purchases") ? root.getAsJsonArray("purchases") : new JsonArray();
        List<Pending> result = new ArrayList<>();
        for (JsonElement element : purchases) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject purchase = element.getAsJsonObject();
            String id = text(purchase, "id");
            String username = text(purchase, "username");
            String group = text(purchase, "group");
            String rank = text(purchase, "rank");
            if (id.isBlank() || username.isBlank() || group.isBlank()) {
                continue;
            }
            result.add(new Pending(id, username, group, rank.isBlank() ? group : rank));
        }
        return result;
    }

    private boolean grant(Pending purchase) {
        String template = plugin.getConfig().getString("grant-command", "lp user {user} parent set {group}");
        String command = MessageUtil.format(
                template,
                "user", purchase.username(),
                "group", purchase.group().toLowerCase(Locale.ROOT)
        );
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            String staffMsg = plugin.getConfig().getString(
                    "messages.granted",
                    "<green>Granted {group} to {user}</green>"
            );
            plugin.getLogger().info("Granted " + purchase.group() + " to " + purchase.username());
            MessageUtil.send(Bukkit.getConsoleSender(), MessageUtil.format(
                    staffMsg,
                    "user", purchase.username(),
                    "group", purchase.group()
            ));
            Player online = Bukkit.getPlayerExact(purchase.username());
            if (online != null) {
                String playerMsg = plugin.getConfig().getString(
                        "messages.granted-player",
                        "<gold>Thanks!</gold> <gray>You received</gray> <aqua>{rank}</aqua>"
                );
                MessageUtil.send(online, MessageUtil.format(
                        playerMsg,
                        "rank", purchase.rank(),
                        "group", purchase.group(),
                        "user", purchase.username()
                ));
            }
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("Grant failed for " + purchase.username() + ": " + exception.getMessage());
            return false;
        }
    }

    private void claim(String id) throws Exception {
        String apiKey = plugin.getConfig().getString("api-key", "").trim();
        String base = plugin.getConfig().getString("api-base-url", "https://frostffa.web.app").replaceAll("/$", "");
        HttpURLConnection connection = (HttpURLConnection) URI.create(base + "/api/claim-purchase").toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        byte[] payload = ("{\"id\":\"" + id.replace("\"", "") + "\"}").getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(payload);
        }
        int code = connection.getResponseCode();
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("Claim HTTP " + code + " for " + id);
        }
    }

    private static String text(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString().trim();
    }
}
