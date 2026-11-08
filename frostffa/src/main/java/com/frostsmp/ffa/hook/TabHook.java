package com.frostsmp.ffa.hook;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Talks to TAB so LuckPerms prefixes / TAB nametags actually hide during Blackout.
 */
public final class TabHook {

    private TabHook() {
    }

    public static boolean present() {
        return Bukkit.getPluginManager().getPlugin("TAB") != null;
    }

    public static void hide(Player player) {
        if (!present()) {
            return;
        }
        try {
            TabAPI api = TabAPI.getInstance();
            TabPlayer tabPlayer = api.getPlayer(player.getUniqueId());
            if (tabPlayer == null) {
                return;
            }
            NameTagManager nametags = api.getNameTagManager();
            if (nametags != null) {
                nametags.hideNameTag(tabPlayer);
                nametags.hideNameTagVisibilityView(tabPlayer, false);
                nametags.setPrefix(tabPlayer, "");
                nametags.setSuffix(tabPlayer, "");
            }
            TabListFormatManager tablist = api.getTabListFormatManager();
            if (tablist != null) {
                tablist.setPrefix(tabPlayer, "");
                tablist.setName(tabPlayer, " ");
                tablist.setSuffix(tabPlayer, "");
            }
        } catch (Throwable ignored) {
            // TAB missing, outdated, or nametags disabled
        }
    }

    public static void show(Player player) {
        if (!present()) {
            return;
        }
        try {
            TabAPI api = TabAPI.getInstance();
            TabPlayer tabPlayer = api.getPlayer(player.getUniqueId());
            if (tabPlayer == null) {
                return;
            }
            NameTagManager nametags = api.getNameTagManager();
            if (nametags != null) {
                nametags.showNameTag(tabPlayer);
                nametags.showNameTagVisibilityView(tabPlayer, false);
                nametags.setPrefix(tabPlayer, null);
                nametags.setSuffix(tabPlayer, null);
            }
            TabListFormatManager tablist = api.getTabListFormatManager();
            if (tablist != null) {
                tablist.setPrefix(tabPlayer, null);
                tablist.setName(tabPlayer, null);
                tablist.setSuffix(tabPlayer, null);
            }
        } catch (Throwable ignored) {
            // TAB missing or player already gone
        }
    }
}
