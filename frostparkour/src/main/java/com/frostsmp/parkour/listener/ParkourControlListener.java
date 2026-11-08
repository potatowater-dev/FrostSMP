package com.frostsmp.parkour.listener;

import com.frostsmp.parkour.gui.LeaderboardGuiHolder;
import com.frostsmp.parkour.gui.ParkourGuiService;
import com.frostsmp.parkour.gui.StatsGuiHolder;
import com.frostsmp.parkour.model.Course;
import com.frostsmp.parkour.service.CourseService;
import com.frostsmp.parkour.service.ParkourSessionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ParkourControlListener implements Listener {

    private final ParkourSessionService sessionService;
    private final ParkourGuiService guiService;
    private final CourseService courseService;
    private final Map<UUID, Long> clickDebounce = new ConcurrentHashMap<>();

    public ParkourControlListener(ParkourSessionService sessionService, ParkourGuiService guiService, CourseService courseService) {
        this.sessionService = sessionService;
        this.guiService = guiService;
        this.courseService = courseService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!sessionService.isInParkour(player.getUniqueId())) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !sessionService.isActionItem(item)) {
            return;
        }

        event.setCancelled(true);

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        long now = System.currentTimeMillis();
        long last = clickDebounce.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 250L) {
            return;
        }
        clickDebounce.put(player.getUniqueId(), now);

        ParkourSessionService.ActionItemType type = sessionService.getActionItemType(item);
        if (type == null) {
            return;
        }

        switch (type) {
            case RESTART -> sessionService.restartCourse(player);
            case CHECKPOINT -> sessionService.teleportLastCheckpoint(player);
            case QUIT -> sessionService.quitParkour(player, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // GUI interactions
        if (event.getInventory().getHolder() instanceof LeaderboardGuiHolder) {
            event.setCancelled(true);
            int closeSlot = event.getView().getTopInventory().getSize() - 5; // default 40
            if (event.getRawSlot() == closeSlot || (event.getCurrentItem() != null && event.getCurrentItem().getType().name().contains("BARRIER"))) {
                player.closeInventory();
            }
            return;
        }

        if (event.getInventory().getHolder() instanceof StatsGuiHolder holder) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null) {
                if (clicked.getType().name().contains("BARRIER")) {
                    player.closeInventory();
                    return;
                }
                if (clicked.getType().name().contains("COMPASS")) {
                    // Try to match course
                    String displayName = clicked.getItemMeta() != null && clicked.getItemMeta().hasDisplayName() ? clicked.getItemMeta().getDisplayName() : "";
                    for (Course course : courseService.getAllCourses()) {
                        guiService.openLeaderboard(player, course.getId());
                        break;
                    }
                }
            }
            return;
        }

        // In-game session inventory protection
        if (sessionService.isInParkour(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof LeaderboardGuiHolder || event.getInventory().getHolder() instanceof StatsGuiHolder) {
            event.setCancelled(true);
            return;
        }
        if (event.getWhoClicked() instanceof Player player && sessionService.isInParkour(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (sessionService.isInParkour(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && sessionService.isInParkour(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (sessionService.isInParkour(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
