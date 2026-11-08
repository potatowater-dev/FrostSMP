package com.frostsmp.crates.listener;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.gui.CrateClaimGui;
import com.frostsmp.crates.gui.CratePreviewGui;
import com.frostsmp.crates.gui.FrostGuiHolder;
import com.frostsmp.crates.service.CrateClaimService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CrateGuiListener implements Listener {

    private final CrateClaimService claimService;

    public CrateGuiListener(FrostCratesPlugin plugin, CrateClaimService claimService) {
        this.claimService = claimService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CratePreviewGui
                || event.getView().getTopInventory().getHolder() instanceof CrateClaimGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FrostGuiHolder holder
                && (holder instanceof CratePreviewGui || holder instanceof CrateClaimGui)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof CrateClaimGui)) {
            return;
        }
        // Never fight intentional plugin/death/disconnect closes.
        InventoryCloseEvent.Reason reason = event.getReason();
        if (reason == InventoryCloseEvent.Reason.PLUGIN
                || reason == InventoryCloseEvent.Reason.DISCONNECT
                || reason == InventoryCloseEvent.Reason.DEATH) {
            return;
        }
        // Opening another inventory (including our own reopen) — don't count as escape.
        if (reason == InventoryCloseEvent.Reason.OPEN_NEW) {
            return;
        }

        claimService.handleRollingInventoryClose(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        claimService.handlePlayerQuit(event.getPlayer());
    }
}
