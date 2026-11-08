package com.frostsmp.killeffect.listener;

import com.frostsmp.killeffect.config.KillEffectConfig;
import com.frostsmp.killeffect.gui.SelectGuiHolder;
import com.frostsmp.killeffect.item.KillEffectItem;
import com.frostsmp.killeffect.model.KillEffectDefinition;
import com.frostsmp.killeffect.storage.PlayerEffectStorage;
import com.frostsmp.killeffect.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class SelectGuiListener implements Listener {

    private final KillEffectConfig config;
    private final PlayerEffectStorage storage;

    public SelectGuiListener(KillEffectConfig config, PlayerEffectStorage storage) {
        this.config = config;
        this.storage = storage;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SelectGuiHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != event.getView().getTopInventory().getHolder()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            storage.select(player.getUniqueId(), null);
            MessageUtil.send(player, config.prefixed("cleared"));
            player.closeInventory();
            return;
        }
        if (clicked.getType() == Material.GRAY_DYE) {
            MessageUtil.send(player, config.prefixed("gui-not-owned"));
            return;
        }

        String effectId = KillEffectItem.effectId(clicked);
        KillEffectDefinition effect = effectId == null ? null : config.effect(effectId);
        if (effect == null) {
            return;
        }
        if (!storage.hasClaimed(player.getUniqueId(), effect.id())) {
            MessageUtil.send(player, config.prefixed("not-owned", "effect", effect.name()));
            return;
        }
        storage.select(player.getUniqueId(), effect.id());
        MessageUtil.send(player, config.prefixed("selected", "effect", effect.name()));
        player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SelectGuiHolder) {
            event.setCancelled(true);
        }
    }
}
