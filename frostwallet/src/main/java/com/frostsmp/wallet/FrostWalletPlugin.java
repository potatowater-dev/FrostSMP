package com.frostsmp.wallet;

import com.frostsmp.license.LicenseGate;
import com.frostsmp.wallet.command.SafeCommandExecutor;
import com.frostsmp.wallet.command.WalletCommand;
import com.frostsmp.wallet.config.PluginConfig;
import com.frostsmp.wallet.placeholder.WalletExpansion;
import com.frostsmp.wallet.service.WalletService;
import com.frostsmp.wallet.storage.WalletStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostWalletPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private WalletStorage walletStorage;
    private WalletService walletService;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        walletStorage = new WalletStorage(this);
        walletStorage.load();
        walletService = new WalletService(pluginConfig, walletStorage);

        WalletCommand command = new WalletCommand(this, pluginConfig, walletService);
        SafeCommandExecutor safe = new SafeCommandExecutor(this, command);
        bind("wallet", safe, command);
        bind("balance", safe, command);
        bind("deposit", safe, command);
        bind("withdraw", safe, command);
        bind("depositall", safe, command);

        var papi = getServer().getPluginManager().getPlugin("PlaceholderAPI");
        // Next-tick so FancyHolograms / PAPI always see a registered expansion.
        getServer().getScheduler().runTask(this, () -> {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null
                    || !getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                getLogger().warning("PlaceholderAPI missing/disabled — wallet placeholders unavailable.");
                return;
            }
            try {
                Object papiPlugin = me.clip.placeholderapi.PlaceholderAPIPlugin.getInstance();
                Object manager = papiPlugin.getClass().getMethod("getLocalExpansionManager").invoke(papiPlugin);
                @SuppressWarnings("unchecked")
                java.util.Collection<me.clip.placeholderapi.expansion.PlaceholderExpansion> expansions =
                        (java.util.Collection<me.clip.placeholderapi.expansion.PlaceholderExpansion>)
                                manager.getClass().getMethod("getExpansions").invoke(manager);
                for (me.clip.placeholderapi.expansion.PlaceholderExpansion expansion
                        : java.util.List.copyOf(expansions)) {
                    if ("frostwallet".equalsIgnoreCase(expansion.getIdentifier())) {
                        manager.getClass()
                                .getMethod("unregister", me.clip.placeholderapi.expansion.PlaceholderExpansion.class)
                                .invoke(manager, expansion);
                        getLogger().info("Unregistered stale PlaceholderAPI expansion: frostwallet");
                    }
                }
            } catch (Throwable throwable) {
                getLogger().warning("Could not unregister old frostwallet PAPI expansion: " + throwable.getMessage());
            }
            boolean ok = new WalletExpansion(this, walletService).register();
            if (ok) {
                getLogger().info("Registered PlaceholderAPI expansion: %frostwallet_*%");
            } else {
                getLogger().warning("Failed to register PlaceholderAPI expansion (frostwallet).");
            }
        });

        getLogger().info("FrostWallet enabled.");
    }

    @Override
    public void onDisable() {
        if (walletStorage != null) {
            walletStorage.save();
        }
    }

    public void reloadAll() {
        if (walletStorage != null) {
            walletStorage.save();
        }
        pluginConfig.load();
        walletStorage.load();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public WalletService getWalletService() {
        return walletService;
    }

    private void bind(String name, SafeCommandExecutor executor, WalletCommand tabs) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command '" + name + "' missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(tabs);
    }
}
