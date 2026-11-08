package com.frostsmp.afk.listener;

import com.frostsmp.afk.config.PluginConfig;
import com.frostsmp.afk.service.WandService;
import com.frostsmp.afk.storage.RegionStorage;
import com.frostsmp.afk.util.MessageUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class WandListener implements Listener {

    private final PluginConfig config;
    private final RegionStorage regionStorage;
    private final WandService wandService;

    public WandListener(PluginConfig config, RegionStorage regionStorage, WandService wandService) {
        this.config = config;
        this.regionStorage = regionStorage;
        this.wandService = wandService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("frostsmp.afk.admin")) {
            return;
        }
        if (!wandService.isWand(player.getInventory().getItemInMainHand())) {
            return;
        }

        Action action = event.getAction();
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            regionStorage.setPos1(block.getLocation());
            MessageUtil.send(player, config.getPos1Set().replace("{location}", RegionStorage.format(block.getLocation())));
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            regionStorage.setPos2(block.getLocation());
            MessageUtil.send(player, config.getPos2Set().replace("{location}", RegionStorage.format(block.getLocation())));
        }
    }
}
