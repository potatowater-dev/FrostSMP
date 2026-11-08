package com.frostsmp.chat.listener;

import com.frostsmp.chat.service.EnderChestSnapshotService;
import com.frostsmp.chat.gui.StatsGui;
import com.frostsmp.chat.service.WalletPreviewService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class SnapshotViewListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (EnderChestSnapshotService.isSnapshotView(event.getView().getTopInventory())
                || WalletPreviewService.isWalletView(event.getView().getTopInventory())
                || StatsGui.isStatsView(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (EnderChestSnapshotService.isSnapshotView(event.getView().getTopInventory())
                || WalletPreviewService.isWalletView(event.getView().getTopInventory())
                || StatsGui.isStatsView(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}
