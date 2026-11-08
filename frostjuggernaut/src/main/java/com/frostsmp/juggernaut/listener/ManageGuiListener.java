package com.frostsmp.juggernaut.listener;

import com.frostsmp.juggernaut.gui.ManageGui;
import com.frostsmp.juggernaut.service.EventService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.UUID;

public final class ManageGuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ManageGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getSlot();
        EventService eventService = gui.getEventService();

        if (slot == ManageGui.SLOT_REFRESH) {
            gui.populate();
            return;
        }
        if (slot == ManageGui.SLOT_START) {
            eventService.start(player);
            gui.populate();
            return;
        }
        if (slot == ManageGui.SLOT_STOP) {
            eventService.stop(player, false);
            gui.populate();
            return;
        }
        if (slot >= 0 && slot < ManageGui.PLAYER_SLOTS) {
            UUID targetId = gui.getPlayerAtSlot(slot);
            if (targetId == null) {
                return;
            }
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                gui.populate();
                return;
            }
            eventService.toggleJuggernaut(target, player);
            gui.populate();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ManageGui) {
            event.setCancelled(true);
        }
    }
}
