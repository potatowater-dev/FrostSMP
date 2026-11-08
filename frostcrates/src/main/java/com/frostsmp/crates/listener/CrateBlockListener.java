package com.frostsmp.crates.listener;

import com.frostsmp.crates.config.MessagesConfig;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.service.CrateService;
import com.frostsmp.crates.service.PlacementService;
import com.frostsmp.crates.util.BlockMaterialUtil;
import com.frostsmp.crates.util.PluginKeys;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class CrateBlockListener implements Listener {

    private final PluginKeys keys;
    private final CrateService crateService;
    private final PlacementService placementService;
    private final MessagesConfig messages;

    public CrateBlockListener(
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrateBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) {
            return;
        }
        var meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(keys.crateBlock, PersistentDataType.BYTE)) {
            return;
        }
        String crateId = meta.getPersistentDataContainer().get(keys.crateId, PersistentDataType.STRING);
        if (crateId == null) {
            return;
        }
        CrateDefinition definition = crateService.getDefinition(crateId);
        if (definition == null) {
            messages.send(event.getPlayer(), "wand-crate-missing", "crate", crateId);
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        Block target = event.getBlockPlaced();
        if (placementService.placeCrate(player, target, definition)) {
            consumeOne(player, item);
        }
    }

    private void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
            return;
        }
        item.setAmount(item.getAmount() - 1);
    }
}
