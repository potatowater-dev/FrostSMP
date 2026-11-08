package com.frostsmp.killeffect.listener;

import com.frostsmp.killeffect.config.KillEffectConfig;
import com.frostsmp.killeffect.item.KillEffectItem;
import com.frostsmp.killeffect.model.KillEffectDefinition;
import com.frostsmp.killeffect.storage.PlayerEffectStorage;
import com.frostsmp.killeffect.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Claim vouchers on right-click. Dye materials (e.g. old blood = red_dye) make the client
 * predict consumption on sheep/cauldrons; cancelling + mutating inventory same-tick dupes.
 * We only handle the main hand and defer claim/consume by 1 tick.
 */
public final class ClaimListener implements Listener {

    private static final long CLAIM_COOLDOWN_MS = 250L;

    private final JavaPlugin plugin;
    private final KillEffectConfig config;
    private final PlayerEffectStorage storage;
    private final Map<UUID, Long> recentClaim = new ConcurrentHashMap<>();

    public ClaimListener(JavaPlugin plugin, KillEffectConfig config, PlayerEffectStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        // Off-hand fires a second interact — ignore it (prevents double claim / desync).
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        KillEffectDefinition effect = config.effectOf(item);
        if (effect == null) {
            return;
        }

        // Holding a voucher must not block ender chests / containers.
        if (action == Action.RIGHT_CLICK_BLOCK && isUsableBlock(event.getClickedBlock())) {
            event.setUseItemInHand(Event.Result.DENY);
            return;
        }

        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> claim(player, EquipmentSlot.HAND, effect));
    }

    /**
     * Dyes on sheep/signs/etc. use entity interact — client predicts dye loss.
     * Cancel and resync; do not claim here (claim is air/block right-click only).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (config.effectOf(item) == null && !KillEffectItem.isClaimItem(item)) {
            return;
        }
        event.setCancelled(true);
        ItemStack snapshot = item.clone();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            ItemStack current = player.getInventory().getItemInMainHand();
            // If the client ate the dye, put the voucher back.
            if (current == null || current.getType().isAir()
                    || (KillEffectItem.isClaimItem(snapshot) && !KillEffectItem.isClaimItem(current))) {
                player.getInventory().setItemInMainHand(snapshot);
            }
        });
    }

    private void claim(Player player, EquipmentSlot hand, KillEffectDefinition effect) {
        if (!player.isOnline()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = recentClaim.get(player.getUniqueId());
        if (last != null && now - last < CLAIM_COOLDOWN_MS) {
            return;
        }

        ItemStack stack = handItem(player, hand);
        if (stack == null || stack.getType().isAir() || config.effectOf(stack) == null) {
            return;
        }

        if (!player.hasPermission(effect.permission()) && !player.hasPermission("frostsmp.killeffect.claim.*")) {
            MessageUtil.send(player, config.prefixed("cannot-claim"));
            return;
        }

        recentClaim.put(player.getUniqueId(), now);

        if (storage.hasClaimed(player.getUniqueId(), effect.id())) {
            consumeOne(player, hand);
            MessageUtil.send(player, config.prefixed("already-claimed", "effect", effect.name()));
            return;
        }

        if (!storage.claim(player.getUniqueId(), effect.id())) {
            consumeOne(player, hand);
            MessageUtil.send(player, config.prefixed("already-claimed", "effect", effect.name()));
            return;
        }

        consumeOne(player, hand);
        MessageUtil.send(player, config.prefixed("claimed", "effect", effect.name()));
    }

    private static ItemStack handItem(Player player, EquipmentSlot hand) {
        PlayerInventory inventory = player.getInventory();
        return hand == EquipmentSlot.OFF_HAND
                ? inventory.getItemInOffHand()
                : inventory.getItemInMainHand();
    }

    private static boolean isUsableBlock(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (type == Material.ENDER_CHEST
                || type == Material.CRAFTING_TABLE
                || type == Material.ENCHANTING_TABLE
                || type == Material.ANVIL
                || type == Material.CHIPPED_ANVIL
                || type == Material.DAMAGED_ANVIL
                || type == Material.GRINDSTONE
                || type == Material.STONECUTTER
                || type == Material.CARTOGRAPHY_TABLE
                || type == Material.LOOM
                || type == Material.SMITHING_TABLE
                || type == Material.BEACON
                || type == Material.LECTERN
                || type == Material.RESPAWN_ANCHOR
                || type == Material.CAULDRON
                || type == Material.WATER_CAULDRON
                || type == Material.LAVA_CAULDRON
                || type == Material.POWDER_SNOW_CAULDRON) {
            return true;
        }
        try {
            if (block.getState() instanceof InventoryHolder) {
                return true;
            }
        } catch (Exception ignored) {
            // Some blocks throw if chunk is unloading; treat as non-usable.
        }
        String name = type.name();
        return name.contains("DOOR")
                || name.contains("GATE")
                || name.contains("BUTTON")
                || name.contains("LEVER")
                || name.contains("BED")
                || name.endsWith("_SIGN")
                || name.endsWith("_HANGING_SIGN");
    }

    private static void consumeOne(Player player, EquipmentSlot hand) {
        PlayerInventory inventory = player.getInventory();
        ItemStack stack = hand == EquipmentSlot.OFF_HAND
                ? inventory.getItemInOffHand()
                : inventory.getItemInMainHand();
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        int amount = stack.getAmount();
        if (amount <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) {
                inventory.setItemInOffHand(null);
            } else {
                inventory.setItemInMainHand(null);
            }
        } else {
            stack.setAmount(amount - 1);
        }
    }
}
