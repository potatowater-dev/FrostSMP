package com.frostsmp.parkour.listener;

import com.frostsmp.parkour.model.Course;
import com.frostsmp.parkour.service.CourseService;
import com.frostsmp.parkour.service.ParkourSessionService;
import com.frostsmp.parkour.session.ParkourSession;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

public final class ParkourTriggerListener implements Listener {

    private final CourseService courseService;
    private final ParkourSessionService sessionService;

    public ParkourTriggerListener(CourseService courseService, ParkourSessionService sessionService) {
        this.courseService = courseService;
        this.sessionService = sessionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        checkLocationTrigger(player, block.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        Player player = event.getPlayer();
        checkLocationTrigger(player, to.getBlock().getLocation());
    }

    private void checkLocationTrigger(Player player, Location blockLoc) {
        // 1. If player is in parkour session: check checkpoints and end
        if (sessionService.isInParkour(player.getUniqueId())) {
            ParkourSession session = sessionService.getSession(player.getUniqueId());
            if (session == null) {
                return;
            }
            Course course = courseService.getCourse(session.getCourseId());
            if (course == null) {
                return;
            }

            // Check End
            if (isSameBlock(blockLoc, course.getEndLocation())) {
                sessionService.handleFinish(player);
                return;
            }

            // Check Checkpoints
            List<Location> checkpoints = course.getCheckpoints();
            for (int i = 0; i < checkpoints.size(); i++) {
                if (isSameBlock(blockLoc, checkpoints.get(i))) {
                    sessionService.handleCheckpointReached(player, i, checkpoints.get(i));
                    return;
                }
            }

            // Check Start again (restart)
            if (isSameBlock(blockLoc, course.getStartLocation())) {
                if (session.getCurrentCheckpointIndex() >= 0 || session.getElapsedTimeMs() > 2000L) {
                    sessionService.restartCourse(player);
                }
            }
            return;
        }

        // 2. If player is NOT in parkour: check if stepping on any course start
        if (sessionService.isOnStartCooldown(player.getUniqueId())) {
            return;
        }
        for (Course course : courseService.getAllCourses()) {
            if (course.isConfigured() && isSameBlock(blockLoc, course.getStartLocation())) {
                sessionService.startCourse(player, course);
                break;
            }
        }
    }

    private static boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || loc1.getWorld() == null || loc2.getWorld() == null) {
            return false;
        }
        return loc1.getWorld().equals(loc2.getWorld())
                && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }
}
