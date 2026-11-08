package com.frostsmp.chat.service;

import com.frostsmp.chat.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Reads FrostWallet balances for [wallet] hover/GUI.
 * Uses reflection against FrostWallet when available, YAML fallback otherwise —
 * never hard-depends on FrostWallet classes (avoids chat dying if wallet is outdated).
 */
public final class WalletPreviewService {

    public static final class WalletViewHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final JavaPlugin plugin;

    public WalletPreviewService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Component buildWalletHover(UUID playerId, String playerName) {
        WalletData data = load(playerId, playerName);
        if (data == null) {
            return MessageUtil.parse(plugin.getConfig().getString(
                    "messages.wallet-missing",
                    "<gray>FrostWallet not installed.</gray>"
            ));
        }
        Component out = MessageUtil.parse(MessageUtil.format(
                plugin.getConfig().getString("messages.wallet-header", "<gold>{player}'s wallet</gold>"),
                "player", playerName
        ));
        if (data.lines.isEmpty()) {
            return out.appendNewline().append(MessageUtil.parse(plugin.getConfig().getString(
                    "messages.wallet-empty",
                    "<gray>No wallet balance.</gray>"
            )));
        }
        for (String line : data.lines) {
            out = out.appendNewline().append(MessageUtil.parse(line));
        }
        out = out.appendNewline().append(MessageUtil.parse(plugin.getConfig().getString(
                "messages.wallet-click",
                "<gray>Click to open wallet GUI</gray>"
        )));
        return out;
    }

    public Inventory createWalletView(UUID playerId, String playerName) {
        WalletData data = load(playerId, playerName);
        String title = MessageUtil.format(
                plugin.getConfig().getString("messages.wallet-gui-title", "{player}'s Wallet"),
                "player", playerName
        );
        Inventory inventory = Bukkit.createInventory(new WalletViewHolder(), 27, MessageUtil.parse(title));
        if (data == null) {
            inventory.setItem(13, named(Material.BARRIER, plugin.getConfig().getString(
                    "messages.wallet-missing",
                    "<gray>FrostWallet not installed.</gray>"
            )));
            return inventory;
        }
        if (data.entries.isEmpty()) {
            inventory.setItem(13, named(Material.PAPER, plugin.getConfig().getString(
                    "messages.wallet-empty",
                    "<gray>No wallet balance.</gray>"
            )));
            return inventory;
        }
        int slot = 10;
        for (WalletEntry entry : data.entries) {
            if (slot == 17) {
                slot = 19;
            }
            if (slot >= inventory.getSize()) {
                break;
            }
            ItemStack icon = entry.icon() == null
                    ? new ItemStack(Material.NETHER_STAR)
                    : entry.icon().clone();
            icon.setAmount(1);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(MessageUtil.parse("<gold>" + entry.name() + "</gold>"));
                List<Component> lore = new ArrayList<>();
                lore.add(MessageUtil.parse("<white>" + entry.balance() + "</white>"));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot++, icon);
        }
        return inventory;
    }

    public static boolean isWalletView(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof WalletViewHolder;
    }

    private WalletData load(UUID playerId, String playerName) {
        Plugin walletPlugin = plugin.getServer().getPluginManager().getPlugin("FrostWallet");
        if (walletPlugin == null || !walletPlugin.isEnabled()) {
            return null;
        }
        try {
            WalletData viaApi = loadViaApi(walletPlugin, playerId, playerName);
            if (viaApi != null) {
                return viaApi;
            }
        } catch (Throwable ignored) {
            // Outdated FrostWallet or API mismatch — fall back to YAML.
        }
        return loadViaYaml(walletPlugin, playerId, playerName);
    }

    private WalletData loadViaApi(Plugin walletPlugin, UUID playerId, String playerName) throws Exception {
        Method getConfig = walletPlugin.getClass().getMethod("getPluginConfig");
        Method getService = walletPlugin.getClass().getMethod("getWalletService");
        Object config = getConfig.invoke(walletPlugin);
        Object service = getService.invoke(walletPlugin);
        if (config == null || service == null) {
            return null;
        }
        Method currenciesMethod = config.getClass().getMethod("currencies");
        @SuppressWarnings("unchecked")
        List<Object> currencies = (List<Object>) currenciesMethod.invoke(config);
        if (currencies == null || currencies.isEmpty()) {
            return empty(playerName);
        }

        Method balanceMethod = null;
        for (Method method : service.getClass().getMethods()) {
            if (!method.getName().equals("balance") || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (types[0] == UUID.class) {
                balanceMethod = method;
                break;
            }
        }
        if (balanceMethod == null) {
            return null;
        }

        String lineTemplate = plugin.getConfig().getString(
                "messages.wallet-line",
                "<gray>{currency}:</gray> <white>{balance}</white>"
        );
        List<String> lines = new ArrayList<>();
        List<WalletEntry> entries = new ArrayList<>();

        for (Object currency : currencies) {
            Method nameMethod = currency.getClass().getMethod("name");
            Method denomsMethod = currency.getClass().getMethod("denominations");
            String name = String.valueOf(nameMethod.invoke(currency));
            long balance = ((Number) balanceMethod.invoke(service, playerId, currency)).longValue();
            if (balance <= 0L) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Object> denoms = (List<Object>) denomsMethod.invoke(currency);
            ItemStack icon = highestFromDenoms(denoms);
            entries.add(new WalletEntry(name, balance, icon));
            lines.add(MessageUtil.format(lineTemplate, "currency", name, "balance", String.valueOf(balance)));
        }
        return new WalletData(playerName, lines, entries);
    }

    private WalletData loadViaYaml(Plugin walletPlugin, UUID playerId, String playerName) {
        File dataFolder = walletPlugin.getDataFolder();
        File configFile = new File(dataFolder, "config.yml");
        File walletsFile = new File(dataFolder, "wallets.yml");
        if (!configFile.exists()) {
            return null;
        }

        YamlConfiguration walletConfig = YamlConfiguration.loadConfiguration(configFile);
        YamlConfiguration balancesConfig = YamlConfiguration.loadConfiguration(walletsFile);

        LinkedHashMap<String, CurrencyInfo> currencies = new LinkedHashMap<>();
        ConfigurationSection root = walletConfig.getConfigurationSection("currencies");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                String name = section.getString("name", id);
                ItemStack largest = largestDenominationItem(section);
                currencies.put(id.toLowerCase(Locale.ROOT), new CurrencyInfo(name, largest));
            }
        }

        String lineTemplate = plugin.getConfig().getString(
                "messages.wallet-line",
                "<gray>{currency}:</gray> <white>{balance}</white>"
        );
        List<String> lines = new ArrayList<>();
        List<WalletEntry> entries = new ArrayList<>();
        for (Map.Entry<String, CurrencyInfo> entry : currencies.entrySet()) {
            long balance = balancesConfig.getLong("players." + playerId + "." + entry.getKey(), 0L);
            if (balance <= 0L) {
                continue;
            }
            CurrencyInfo info = entry.getValue();
            entries.add(new WalletEntry(info.name(), balance, info.largestItem()));
            lines.add(MessageUtil.format(lineTemplate, "currency", info.name(), "balance", String.valueOf(balance)));
        }
        return new WalletData(playerName, lines, entries);
    }

    private static ItemStack highestFromDenoms(List<Object> denoms) throws Exception {
        if (denoms == null || denoms.isEmpty()) {
            return new ItemStack(Material.NETHER_STAR);
        }
        // FrostWallet sorts highest-worth first.
        Object top = denoms.getFirst();
        Method template = top.getClass().getMethod("template");
        ItemStack stack = (ItemStack) template.invoke(top);
        if (stack == null || stack.getType().isAir()) {
            return new ItemStack(Material.NETHER_STAR);
        }
        ItemStack clone = stack.clone();
        clone.setAmount(1);
        return clone;
    }

    private ItemStack largestDenominationItem(ConfigurationSection currencySection) {
        List<Map<?, ?>> denoms = currencySection.getMapList("denominations");
        long bestWorth = Long.MIN_VALUE;
        String bestItem = null;
        for (Map<?, ?> denom : denoms) {
            long worth = asLong(denom.get("worth"), 0L);
            Object rawItem = denom.get("item");
            String item = rawItem == null ? "" : String.valueOf(rawItem);
            if (worth <= 0L || item.isBlank()) {
                continue;
            }
            if (worth > bestWorth) {
                bestWorth = worth;
                bestItem = item;
            }
        }
        if (bestItem == null) {
            return new ItemStack(Material.NETHER_STAR);
        }
        try {
            ItemStack stack = Bukkit.getItemFactory().createItemStack(bestItem);
            stack.setAmount(1);
            return stack;
        } catch (IllegalArgumentException exception) {
            Material material = materialFromGiveString(bestItem);
            return new ItemStack(material == null ? Material.NETHER_STAR : material);
        }
    }

    private static Material materialFromGiveString(String give) {
        try {
            String id = give;
            int bracket = give.indexOf('[');
            if (bracket >= 0) {
                id = give.substring(0, bracket);
            }
            id = id.trim();
            if (id.startsWith("minecraft:")) {
                id = id.substring("minecraft:".length());
            }
            return Material.matchMaterial(id);
        } catch (Exception ignored) {
            return null;
        }
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

    private static ItemStack named(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(MessageUtil.parse(name));
        stack.setItemMeta(meta);
        return stack;
    }

    private static WalletData empty(String playerName) {
        return new WalletData(playerName, List.of(), List.of());
    }

    private record CurrencyInfo(String name, ItemStack largestItem) {
    }

    private record WalletEntry(String name, long balance, ItemStack icon) {
    }

    private record WalletData(String playerName, List<String> lines, List<WalletEntry> entries) {
    }
}
