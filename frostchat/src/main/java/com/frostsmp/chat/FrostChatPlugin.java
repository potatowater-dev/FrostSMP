package com.frostsmp.chat;

import com.frostsmp.chat.command.FrostChatCommand;
import com.frostsmp.chat.command.SafeCommandExecutor;
import com.frostsmp.chat.command.StatsCommand;
import com.frostsmp.chat.listener.ChatTagListener;
import com.frostsmp.chat.listener.SnapshotViewListener;
import com.frostsmp.chat.service.EnderChestSnapshotService;
import com.frostsmp.chat.service.StatsService;
import com.frostsmp.chat.service.WalletPreviewService;
import com.frostsmp.chat.util.MessageUtil;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostChatPlugin extends JavaPlugin {

    private EnderChestSnapshotService snapshots;
    private WalletPreviewService walletPreviews;
    private StatsService statsService;
    private ChatTagListener chatTagListener;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        snapshots = new EnderChestSnapshotService(this);
        walletPreviews = new WalletPreviewService(this);
        statsService = new StatsService(this);

        chatTagListener = new ChatTagListener(this);
        getServer().getPluginManager().registerEvents(chatTagListener, this);
        getServer().getPluginManager().registerEvents(new SnapshotViewListener(), this);
        getLogger().info("FrostChat tags ready: [wallet] [item]/[i] [ec] [inv] (cancel+resend).");

        FrostChatCommand command = new FrostChatCommand(this);
        PluginCommand frostchat = getCommand("frostchat");
        if (frostchat != null) {
            frostchat.setExecutor(new SafeCommandExecutor(this, command));
            frostchat.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'frostchat' missing from plugin.yml!");
        }

        StatsCommand statsCommand = new StatsCommand(this, statsService);
        PluginCommand stats = getCommand("stats");
        if (stats != null) {
            stats.setExecutor(new SafeCommandExecutor(this, statsCommand));
            stats.setTabCompleter(statsCommand);
        } else {
            getLogger().severe("Command 'stats' missing from plugin.yml!");
        }

        getLogger().info("FrostChat enabled.");
    }

    public void reloadAll() {
        reloadConfig();
        snapshots = new EnderChestSnapshotService(this);
        walletPreviews = new WalletPreviewService(this);
        statsService = new StatsService(this);
    }

    public EnderChestSnapshotService getSnapshots() {
        return snapshots;
    }

    public WalletPreviewService getWalletPreviews() {
        return walletPreviews;
    }

    public StatsService getStatsService() {
        return statsService;
    }

    public ChatTagListener getChatTagListener() {
        return chatTagListener;
    }

    public String msg(String key) {
        return getConfig().getString("messages." + key, "");
    }

    public String prefixed(String key, String... kv) {
        return getConfig().getString("messages.prefix", "") + MessageUtil.format(msg(key), kv);
    }
}
