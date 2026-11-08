package com.frostsmp.grapple.listener;

import com.frostsmp.grapple.config.GrappleConfig;
import com.frostsmp.grapple.item.GrappleItem;
import com.frostsmp.grapple.service.GrappleService;
import com.frostsmp.grapple.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GrappleListener implements Listener {

    private final GrappleConfig config;
    private final GrappleService grapples;
    private final Set<Integer> landedOnBlock = ConcurrentHashMap.newKeySet();

    public GrappleListener(GrappleConfig config, GrappleService grapples) {
        this.config = config;
        this.grapples = grapples;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHookHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof FishHook hook)) {
            return;
        }
        if (!(hook.getShooter() instanceof Player player)) {
            return;
        }
        if (!GrappleItem.isGrapple(rodUsed(player, null))) {
            return;
        }
        Block hit = event.getHitBlock();
        if (hit != null && !hit.isPassable()) {
            landedOnBlock.add(hook.getEntityId());
            stickHookToBlock(hook, hit, event.getHitBlockFace());
            return;
        }
        // Ignore entity hits — grapple is block/self only.
        if (event.getHitEntity() != null) {
            hook.setHookedEntity(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack rod = rodUsed(player, event.getHand());
        if (!GrappleItem.isGrapple(rod)) {
            return;
        }
        if (!config.isWorldEnabled(player.getWorld())) {
            return;
        }
        if (!player.hasPermission("frostsmp.grapple.use")) {
            event.setCancelled(true);
            MessageUtil.send(player, config.prefixed("no-use"));
            return;
        }
        FishHook hook = event.getHook();
        switch (event.getState()) {
            case FISHING -> hook.setVelocity(hook.getVelocity().multiply(config.speed()));
            case CAUGHT_FISH -> event.setCancelled(true);
            case CAUGHT_ENTITY -> {
                // Grapple never pulls / latches onto entities.
                event.setCancelled(true);
            }
            case IN_GROUND, FAILED_ATTEMPT, REEL_IN -> {
                if (hookLandedOnBlock(hook, event.getState())) {
                    if (grapples.isOnCooldown(player, rod)) {
                        grapples.pulseCooldown(player, rod);
                        landedOnBlock.remove(hook.getEntityId());
                        return;
                    }
                    grapples.tryPullSelf(player, hook.getLocation(), rod);
                }
                landedOnBlock.remove(hook.getEntityId());
            }
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (grapples.isFallProtected(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        grapples.clear(event.getPlayer());
    }

    private boolean hookLandedOnBlock(FishHook hook, PlayerFishEvent.State state) {
        if (state == PlayerFishEvent.State.IN_GROUND) {
            return true;
        }
        if (landedOnBlock.contains(hook.getEntityId())) {
            return true;
        }
        return isAgainstSolidBlock(hook.getLocation());
    }

    private static void stickHookToBlock(FishHook hook, Block hit, BlockFace face) {
        Location stick = hit.getLocation().add(0.5, 0.5, 0.5);
        if (face != null && face != BlockFace.SELF) {
            stick.add(face.getModX() * 0.55, face.getModY() * 0.55, face.getModZ() * 0.55);
        }
        hook.teleport(stick);
        hook.setVelocity(new Vector(0, 0, 0));
        hook.setGravity(false);
    }

    private static boolean isAgainstSolidBlock(Location location) {
        Block origin = location.getBlock();
        if (!origin.isPassable()) {
            return true;
        }
        Block below = location.clone().add(0.0, -0.3, 0.0).getBlock();
        if (!below.isPassable()) {
            return true;
        }
        // Walls / ceilings — bobber sitting against a solid neighbor.
        for (BlockFace face : new BlockFace[]{
                BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN
        }) {
            Block neighbor = origin.getRelative(face);
            if (!neighbor.isPassable()) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack rodUsed(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }
        if (hand == EquipmentSlot.HAND) {
            return player.getInventory().getItemInMainHand();
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (GrappleItem.isGrapple(main)) {
            return main;
        }
        return player.getInventory().getItemInOffHand();
    }
}
