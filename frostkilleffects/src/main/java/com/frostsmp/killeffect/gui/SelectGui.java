package com.frostsmp.killeffect.gui;

import com.frostsmp.killeffect.config.KillEffectConfig;
import com.frostsmp.killeffect.model.KillEffectDefinition;
import com.frostsmp.killeffect.storage.PlayerEffectStorage;
import com.frostsmp.killeffect.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SelectGui {

    private SelectGui() {
    }

    public static void open(Player player, KillEffectConfig config, PlayerEffectStorage storage) {
        SelectGuiHolder holder = new SelectGuiHolder();
        Component title = MessageUtil.parse(config.msg("gui-title"));
        Inventory inventory = Bukkit.createInventory(holder, 27, title);
        holder.bind(inventory);

        Set<String> owned = storage.claimed(player.getUniqueId());
        String selected = storage.selected(player.getUniqueId());

        int slot = 11;
        for (KillEffectDefinition effect : config.effects()) {
            if (slot > 15) {
                break;
            }
            boolean has = owned.contains(effect.id());
            boolean isSelected = effect.id().equalsIgnoreCase(selected);
            inventory.setItem(slot++, icon(effect, has, isSelected, config));
        }

        inventory.setItem(22, clearIcon(selected == null || selected.isBlank(), config));
        player.openInventory(inventory);
    }

    private static ItemStack icon(KillEffectDefinition effect, boolean owned, boolean selected, KillEffectConfig config) {
        ItemStack stack = effect.claimItem().clone();
        stack.setAmount(1);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(MessageUtil.parse("<white>" + effect.name() + "</white>"));
        List<Component> lore = new ArrayList<>();
        if (!owned) {
            lore.add(MessageUtil.parse(config.msg("gui-locked")));
            stack.setType(Material.GRAY_DYE);
        } else if (selected) {
            lore.add(MessageUtil.parse(config.msg("gui-selected")));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add(MessageUtil.parse(config.msg("gui-click-select")));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack clearIcon(boolean alreadyClear, KillEffectConfig config) {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(MessageUtil.parse(config.msg("gui-clear-name")));
        List<Component> lore = new ArrayList<>();
        lore.add(MessageUtil.parse(alreadyClear ? config.msg("gui-clear-active") : config.msg("gui-clear-click")));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
