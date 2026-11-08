package com.frostsmp.bounty.gui;

import com.frostsmp.bounty.config.PluginConfig;
import com.frostsmp.bounty.model.Bounty;
import com.frostsmp.bounty.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ListBountyGui implements BountyGuiHolder {

    private final Inventory inventory;
    private final List<Bounty> slotBounties = new ArrayList<>();

    public ListBountyGui(PluginConfig config, List<Bounty> bounties) {
        int size = Math.min(54, Math.max(9, ((bounties.size() + 8) / 9) * 9));
        if (size < 9) {
            size = 9;
        }
        this.inventory = Bukkit.createInventory(this, size, MessageUtil.parse(config.getListTitle()));
        int slot = 0;
        for (Bounty bounty : bounties) {
            if (slot >= size) {
                break;
            }
            slotBounties.add(bounty);
            inventory.setItem(slot, headFor(config, bounty));
            slot++;
        }
    }

    private ItemStack headFor(PluginConfig config, Bounty bounty) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(bounty.getTargetId()));
            meta.displayName(MessageUtil.parse(config.getListHeadName().replace("{player}", bounty.getTargetName())));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse(config.getListHeadLorePlacer().replace("{placer}", bounty.getPlacerName())));
            List<ItemStack> rewards = bounty.getRewards();
            lore.add(MessageUtil.parse(config.getListHeadLoreItems().replace("{count}", String.valueOf(rewards.size()))));
            int shown = 0;
            for (ItemStack reward : rewards) {
                if (shown >= 6) {
                    lore.add(MessageUtil.parse("<dark_gray>... +" + (rewards.size() - shown) + " more</dark_gray>"));
                    break;
                }
                lore.add(MessageUtil.parse(config.getListHeadLoreEntry()
                        .replace("{amount}", String.valueOf(reward.getAmount()))
                        .replace("{item}", prettyName(reward))));
                shown++;
            }
            lore.add(MessageUtil.parse("<yellow>Click to view loot</yellow>"));
            meta.lore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private static String prettyName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(item.getItemMeta().displayName());
        }
        String name = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return name;
    }

    public Bounty getBountyAt(int slot) {
        if (slot < 0 || slot >= slotBounties.size()) {
            return null;
        }
        return slotBounties.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
