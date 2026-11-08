package com.frostsmp.crates.listener;

import com.frostsmp.crates.config.MessagesConfig;
import com.frostsmp.crates.gui.CratePreviewGui;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.PlacedCrate;
import com.frostsmp.crates.service.CrateClaimService;
import com.frostsmp.crates.service.CrateCooldownService;
import com.frostsmp.crates.service.CrateService;
import com.frostsmp.crates.service.PlacementService;
import com.frostsmp.crates.util.BlockMaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CrateInteractListener implements Listener {

    private final CrateService crateService;
    private final CrateClaimService claimService;
    private final PlacementService placementService;
    private final MessagesConfig messages;

    public CrateInteractListener(
            CrateService crateService,
            CrateClaimService claimService,
            PlacementService placementService,
            MessagesConfig messages
    ) {
        this.crateService = crateService;
        this.claimService = claimService;
        this.placementService = placementService;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (crateService.getPlacedAt(block.getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrateBroken(BlockBreakEvent event) {
        PlacedCrate placed = crateService.getPlacedAt(event.getBlock().getLocation());
        if (placed != null) {
            placementService.unregisterAt(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        PlacedCrate placed = crateService.getPlacedAt(block.getLocation());
        if (placed != null) {
            event.setCancelled(true);
            if (event.getPlayer() instanceof Player player) {
                openPreview(player, placed);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        PlacedCrate placed = crateService.getPlacedAt(block.getLocation());
        if (placed == null) {
            return;
        }
        crateService.repairBlockIfNeeded(block);
        CrateDefinition definition = crateService.getDefinition(placed);
        if (definition == null) {
            return;
        }
        if (block.getType() != BlockMaterialUtil.normalizeDisplayBlock(definition.getDisplayBlock())) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            openPreview(player, placed);
            return;
        }

        openCrate(player, placed, definition);
    }

    private void openPreview(Player player, PlacedCrate placed) {
        if (!player.hasPermission("frostsmp.crates.open")) {
            messages.send(player, "no-permission-open");
            return;
        }
        CrateDefinition definition = crateService.getDefinition(placed);
        if (definition == null) {
            messages.send(player, "preview-missing-definition");
            return;
        }
        player.openInventory(new CratePreviewGui(definition).getInventory());
    }

    private void openCrate(Player player, PlacedCrate placed, CrateDefinition definition) {
        if (!player.hasPermission("frostsmp.crates.open")) {
            messages.send(player, "no-permission-open");
            return;
        }
        String cooldownKey = CrateCooldownService.cooldownKey(player.getUniqueId(), definition.getId());
        if (player.isSneaking()) {
            claimService.claimAll(player, definition, cooldownKey);
        } else {
            claimService.claim(player, definition, cooldownKey);
        }
    }
}
