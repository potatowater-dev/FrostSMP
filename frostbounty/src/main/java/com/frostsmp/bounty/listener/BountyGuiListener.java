package com.frostsmp.bounty.listener;

import com.frostsmp.bounty.gui.ListBountyGui;
import com.frostsmp.bounty.gui.PlaceBountyGui;
import com.frostsmp.bounty.gui.ViewBountyGui;
import com.frostsmp.bounty.model.Bounty;
import com.frostsmp.bounty.service.BountyService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public final class BountyGuiListener implements Listener {

    private final BountyService bountyService;

    public BountyGuiListener(BountyService bountyService) {
        this.bountyService = bountyService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (holder instanceof PlaceBountyGui placeGui) {
            int raw = event.getRawSlot();
            int topSize = event.getView().getTopInventory().getSize();
            if (raw < topSize && placeGui.isControlSlot(raw)) {
                event.setCancelled(true);
                if (raw == PlaceBountyGui.SLOT_CONFIRM) {
                    if (bountyService.confirmPlace(player, placeGui)) {
                        player.closeInventory();
                    }
                } else if (raw == PlaceBountyGui.SLOT_CANCEL) {
                    bountyService.cancelPlace(player, placeGui);
                    player.closeInventory();
                }
                return;
            }
            // Allow moving items in reward slots and player inventory
            if (raw < topSize && !placeGui.isRewardSlot(raw)) {
                event.setCancelled(true);
            }
            return;
        }

        if (holder instanceof ListBountyGui listGui) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
                return;
            }
            Bounty bounty = listGui.getBountyAt(event.getSlot());
            if (bounty != null) {
                bountyService.openViewGui(player, bounty);
            }
            return;
        }

        if (holder instanceof ViewBountyGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof PlaceBountyGui placeGui) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize() && placeGui.isControlSlot(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        if (holder instanceof ListBountyGui || holder instanceof ViewBountyGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof PlaceBountyGui placeGui) {
            bountyService.handleClosePlace(player, placeGui);
        }
    }
}
