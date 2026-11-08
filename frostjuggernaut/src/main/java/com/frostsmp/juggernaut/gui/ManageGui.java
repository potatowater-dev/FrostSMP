package com.frostsmp.juggernaut.gui;

import com.frostsmp.juggernaut.config.PluginConfig;
import com.frostsmp.juggernaut.service.EventService;
import com.frostsmp.juggernaut.service.EventState;
import com.frostsmp.juggernaut.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ManageGui implements ManageGuiHolder {

    public static final int SLOT_REFRESH = 45;
    public static final int SLOT_START = 49;
    public static final int SLOT_STOP = 53;
    public static final int PLAYER_SLOTS = 45;

    private final Inventory inventory;
    private final EventService eventService;
    private final PluginConfig config;
    private final List<UUID> slotPlayers = new ArrayList<>();

    public ManageGui(EventService eventService, PluginConfig config) {
        this.eventService = eventService;
        this.config = config;
        this.inventory = Bukkit.createInventory(this, 54, MessageUtil.parse("<gold>Juggernaut Manage</gold>"));
        populate();
    }

    public void populate() {
        inventory.clear();
        slotPlayers.clear();

        Set<UUID> selected = eventService.getJuggernauts();
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= PLAYER_SLOTS) {
                break;
            }
            slotPlayers.add(online.getUniqueId());
            inventory.setItem(slot, playerHead(online, selected.contains(online.getUniqueId())));
            slot++;
        }

        inventory.setItem(SLOT_REFRESH, controlItem(Material.SUNFLOWER, "<yellow>Refresh</yellow>",
                List.of("<gray>Reload online players.</gray>")));
        inventory.setItem(SLOT_START, controlItem(Material.LIME_DYE, "<green>Start Event</green>",
                List.of(
                        "<gray>Enable PvP and begin tracking.</gray>",
                        "<gray>Juggernauts:</gray> <white>" + selected.size() + "/"
                                + config.getMaxJuggernauts() + "</white>"
                )));
        inventory.setItem(SLOT_STOP, controlItem(Material.RED_DYE, "<red>Stop Event</red>",
                List.of("<gray>End event and restore PvP.</gray>")));

        ItemStack filler = fillerPane();
        for (int i = PLAYER_SLOTS; i < 54; i++) {
            if (i == SLOT_REFRESH || i == SLOT_START || i == SLOT_STOP) {
                continue;
            }
            inventory.setItem(i, filler);
        }
    }

    public UUID getPlayerAtSlot(int slot) {
        if (slot < 0 || slot >= slotPlayers.size()) {
            return null;
        }
        return slotPlayers.get(slot);
    }

    public EventService getEventService() {
        return eventService;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private ItemStack playerHead(Player player, boolean selected) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.displayName(MessageUtil.parse("<white>" + player.getName() + "</white>"));
            List<Component> lore = new ArrayList<>();
            if (selected) {
                lore.add(MessageUtil.parse("<green>Selected</green>"));
                lore.add(MessageUtil.parse("<gray>Click to remove.</gray>"));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(MessageUtil.parse("<gray>Click to select.</gray>"));
            }
            EventState state = eventService.getState();
            if (state == EventState.ACTIVE || state == EventState.FINISHED) {
                lore.add(MessageUtil.parse("<dark_gray>Event: " + state.name() + "</dark_gray>"));
                if (selected) {
                    lore.add(MessageUtil.parse(eventService.isAliveJuggernaut(player.getUniqueId())
                            ? "<green>Alive</green>"
                            : "<red>Down</red>"));
                }
            }
            meta.lore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private static ItemStack controlItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MessageUtil.parse(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack fillerPane() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            filler.setItemMeta(meta);
        }
        return filler;
    }
}
