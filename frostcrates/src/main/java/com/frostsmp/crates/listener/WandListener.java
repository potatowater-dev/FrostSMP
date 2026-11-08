package com.frostsmp.crates.listener;

import com.frostsmp.crates.config.MessagesConfig;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.PlacedCrate;
import com.frostsmp.crates.service.CrateService;
import com.frostsmp.crates.service.PlacementService;
import com.frostsmp.crates.util.MessageUtil;
import com.frostsmp.crates.util.PluginKeys;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class WandListener implements Listener {

    private final PluginKeys keys;
    private final CrateService crateService;
    private final PlacementService placementService;
    private final MessagesConfig messages;

    public WandListener(
            PluginKeys keys,
            CrateService crateService,
            PlacementService placementService,
            MessagesConfig messages
    ) {
        this.keys = keys;
        this.crateService = crateService;
        this.placementService = placementService;
        this.messages = messages;
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        var meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(keys.crateWand, PersistentDataType.BYTE)) {
            return;
        }
        String crateId = meta.getPersistentDataContainer().get(keys.crateId, PersistentDataType.STRING);
        if (crateId == null) {
            return;
        }
        CrateDefinition definition = crateService.getDefinition(crateId);
        if (definition == null) {
            messages.send(event.getPlayer(), "wand-crate-missing", "crate", crateId);
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            placementService.placeCrate(player, block, definition);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            PlacedCrate placed = crateService.getPlacedAt(block.getLocation());
            if (placed != null && placed.getDefinitionId().equalsIgnoreCase(crateId)) {
                placementService.removeCrate(player, block.getLocation());
                if (crateService.isDisplayBlockMaterial(block.getType())) {
                    block.setType(Material.AIR, false);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (crateService.getPlacedAt(block.getLocation()) == null) {
            return;
        }
        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(keys.crateWand, PersistentDataType.BYTE)) {
                return;
            }
            if (meta != null && meta.getPersistentDataContainer().has(keys.crateBlock, PersistentDataType.BYTE)) {
                return;
            }
        }
        if (crateService.isDisplayBlockMaterial(block.getType())) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "wand-block-protected");
        }
    }
}
