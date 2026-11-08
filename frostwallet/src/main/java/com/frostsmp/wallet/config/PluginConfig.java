package com.frostsmp.wallet.config;

import com.frostsmp.wallet.model.Currency;
import com.frostsmp.wallet.model.Denomination;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PluginConfig {

    private final JavaPlugin plugin;
    private final Map<String, Currency> currencies = new LinkedHashMap<>();

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        currencies.clear();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("currencies");
        if (root == null) {
            plugin.getLogger().warning("No currencies configured.");
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            String name = section.getString("name", id);
            List<Denomination> denoms = new ArrayList<>();
            List<Map<?, ?>> raw = section.getMapList("denominations");
            for (Map<?, ?> entry : raw) {
                String denomId = String.valueOf(entry.get("id"));
                long worth = asLong(entry.get("worth"), 0L);
                String item = entry.get("item") == null ? "" : String.valueOf(entry.get("item"));
                if (worth <= 0L || item.isBlank()) {
                    plugin.getLogger().warning("Skipping invalid denomination in " + id + ": " + denomId);
                    continue;
                }
                try {
                    ItemStack template = Bukkit.getItemFactory().createItemStack(item);
                    template.setAmount(1);
                    denoms.add(new Denomination(denomId, worth, template));
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Bad item for " + id + "/" + denomId + ": " + exception.getMessage());
                }
            }
            denoms.sort(Comparator.comparingLong(Denomination::worth).reversed());
            if (denoms.isEmpty()) {
                plugin.getLogger().warning("Currency " + id + " has no valid denominations.");
                continue;
            }
            currencies.put(id.toLowerCase(Locale.ROOT), new Currency(id.toLowerCase(Locale.ROOT), name, List.copyOf(denoms)));
        }
    }

    public List<Currency> currencies() {
        return List.copyOf(currencies.values());
    }

    public Currency currency(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.toLowerCase(Locale.ROOT).replace(' ', '_');
        Currency direct = currencies.get(key);
        if (direct != null) {
            return direct;
        }
        for (Currency currency : currencies.values()) {
            if (currency.name().equalsIgnoreCase(raw) || currency.id().equalsIgnoreCase(raw)) {
                return currency;
            }
        }
        return null;
    }

    public Currency currencyOf(ItemStack item) {
        for (Currency currency : currencies.values()) {
            if (currency.match(item) != null) {
                return currency;
            }
        }
        return null;
    }

    public String prefixed(String key, String... kv) {
        return plugin.getConfig().getString("messages.prefix", "") + format(msg(key), kv);
    }

    public String msg(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    private static String format(String template, String... kv) {
        return com.frostsmp.wallet.util.MessageUtil.format(template, kv);
    }

    private static long asLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }
}
