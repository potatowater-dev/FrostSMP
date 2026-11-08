package com.frostsmp.crates.gui;

import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.LootEntry;
import com.frostsmp.crates.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CrateEditorGui implements FrostGuiHolder {

    public static final int PRIZE_SLOTS = 45;
    public static final int SLOT_DISPLAY_NAME = 45;
    public static final int SLOT_BOX_COLOR = 46;
    public static final int SLOT_BROADCAST = 47;
    public static final int SLOT_BASE_COOLDOWN = 48;
    public static final int SLOT_AUTO_KEY = 49;
    public static final int SLOT_HELP = 53;

    private final String crateId;
    private final CrateDefinition definition;
    private final Inventory inventory;
    private final Map<Integer, Integer> slotWeights = new HashMap<>();

    public CrateEditorGui(CrateDefinition definition) {
        this.crateId = definition.getId();
        this.definition = definition;
        this.inventory = Bukkit.createInventory(this, 54, MessageUtil.parse("<dark_purple>Edit Crate: <aqua>" + crateId));
        loadFromDefinition();
        refreshDecorations();
    }

    private void loadFromDefinition() {
        int slot = 0;
        slotWeights.clear();
        for (LootEntry entry : definition.getLoot()) {
            if (slot >= PRIZE_SLOTS) {
                break;
            }
            inventory.setItem(slot, withWeightLore(entry.getItem(), entry.getWeight(), definition.getTotalWeight()));
            slotWeights.put(slot, entry.getWeight());
            slot++;
        }
    }

    public String getCrateId() {
        return crateId;
    }

    public CrateDefinition getDefinition() {
        return definition;
    }

    public Map<Integer, Integer> getSlotWeights() {
        return slotWeights;
    }

    public void setSlotWeight(int slot, int weight) {
        slotWeights.put(slot, Math.max(1, weight));
        ItemStack item = inventory.getItem(slot);
        if (item != null && !item.getType().isAir()) {
            inventory.setItem(slot, withWeightLore(item, weight, computeTotalWeight()));
        }
    }

    public int computeTotalWeight() {
        int total = 0;
        for (int slot = 0; slot < PRIZE_SLOTS; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            total += slotWeights.getOrDefault(slot, 1);
        }
        return total;
    }

    public List<LootEntry> collectLootEntries() {
        List<LootEntry> entries = new ArrayList<>();
        for (int slot = 0; slot < PRIZE_SLOTS; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            int weight = slotWeights.getOrDefault(slot, 1);
            entries.add(new LootEntry(stripWeightLore(item), weight));
        }
        return entries;
    }

    public void refreshDecorations() {
        inventory.setItem(SLOT_DISPLAY_NAME, createDisplayNameButton());
        inventory.setItem(SLOT_BOX_COLOR, createBoxColorButton());
        inventory.setItem(SLOT_BROADCAST, createBroadcastButton());
        inventory.setItem(SLOT_BASE_COOLDOWN, createBaseCooldownButton());
        inventory.setItem(SLOT_AUTO_KEY, createAutoKeyButton());
        inventory.setItem(SLOT_HELP, createHelpBook());
        ItemStack pane = pane(Material.BLACK_STAINED_GLASS_PANE, "<gray>Reserved");
        for (int slot = 50; slot <= 52; slot++) {
            inventory.setItem(slot, pane);
        }
    }

    public void refreshAllWeightLore() {
        int total = computeTotalWeight();
        for (int slot = 0; slot < PRIZE_SLOTS; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            inventory.setItem(slot, withWeightLore(item, slotWeights.getOrDefault(slot, 1), total));
        }
    }

    private ItemStack createDisplayNameButton() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Display Name"));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse("<gray>Current: <white>" + definition.getDisplayName()));
            lore.add(Component.empty());
            lore.add(MessageUtil.parse("<yellow>Click to change in chat."));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBoxColorButton() {
        ItemStack item = new ItemStack(definition.getDisplayBlock());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Shulker Box Color"));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse("<gray>Current: <white>" + definition.getDisplayBlock().name()));
            lore.add(Component.empty());
            lore.add(MessageUtil.parse("<yellow>Click to change in chat."));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBroadcastButton() {
        boolean enabled = definition.isBroadcastEnabled();
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Broadcast Rare Drops"));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse("<gray>Status: " + (enabled ? "<green>On" : "<red>Off")));
            lore.add(Component.empty());
            lore.add(MessageUtil.parse("<yellow>Click to toggle."));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBaseCooldownButton() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Base Open Cooldown"));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse("<gray>Current: <white>" + MessageUtil.formatDuration(definition.getOpenCooldownMs())));
            lore.add(MessageUtil.parse("<dark_gray>(" + definition.getOpenCooldownMs() + "ms)"));
            lore.add(Component.empty());
            lore.add(MessageUtil.parse("<yellow>Click to set minutes in chat."));
            lore.add(MessageUtil.parse("<dark_gray>Used when no rank tier matches."));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createAutoKeyButton() {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var autoKey = definition.getAutoKey();
            meta.displayName(MessageUtil.parse("<gold>Auto Key Timer"));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse("<gray>Status: " + (autoKey.isEnabled() ? "<green>On" : "<red>Off")));
            if (!autoKey.isConfigured() && autoKey.isEnabled()) {
                lore.add(MessageUtil.parse("<dark_gray>No rank/group configured."));
            } else if (autoKey.usesGroup()) {
                lore.add(MessageUtil.parse("<gray>LuckPerms group: <white>" + autoKey.getGroup()));
                lore.add(MessageUtil.parse("<gray>Every: <white>" + MessageUtil.formatDuration(autoKey.getIntervalMs())));
                lore.add(MessageUtil.parse("<gray>Keys per grant: <white>" + autoKey.getAmount()));
            } else if (!autoKey.getPermission().isBlank()) {
                lore.add(MessageUtil.parse("<gray>Permission: <white>" + autoKey.getPermission()));
                lore.add(MessageUtil.parse("<gray>Every: <white>" + MessageUtil.formatDuration(autoKey.getIntervalMs())));
                lore.add(MessageUtil.parse("<gray>Keys per grant: <white>" + autoKey.getAmount()));
            }
            lore.add(Component.empty());
            lore.add(MessageUtil.parse("<yellow>Click: toggle on/off"));
            lore.add(MessageUtil.parse("<yellow>Right-click: configure in chat"));
            lore.add(MessageUtil.parse("<dark_gray>Rank: group booster 1440"));
            lore.add(MessageUtil.parse("<dark_gray>Or: booster 1440"));
            lore.add(MessageUtil.parse("<dark_gray>Perm: perm node.name 1440"));
            lore.add(MessageUtil.parse("<dark_gray>Uses LuckPerms group membership."));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHelpBook() {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Crate Editor Help"));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse("<gray>ID: <white>" + definition.getId()));
            lore.add(MessageUtil.parse("<gray>Top 45 slots: loot table"));
            lore.add(MessageUtil.parse("<gray>Right-click prize: set weight"));
            lore.add(Component.empty());
            lore.add(MessageUtil.parse("<gray>Bottom row: crate settings"));
            lore.add(MessageUtil.parse("<gray>Changes save on close."));
            meta.lore(lore);
            book.setItemMeta(meta);
        }
        return book;
    }

    private ItemStack pane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public static ItemStack withWeightLore(ItemStack base, int weight, int totalWeight) {
        ItemStack item = stripWeightLore(base);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            double percent = totalWeight > 0 ? (weight * 100.0 / totalWeight) : 100.0;
            lore.add(MessageUtil.parse("<dark_gray>---"));
            lore.add(MessageUtil.parse("<gray>Weight: <white>" + weight));
            lore.add(MessageUtil.parse("<gray>Chance: <white>" + String.format("%.1f", percent) + "%"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack stripWeightLore(ItemStack item) {
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return copy;
        }
        List<Component> lore = new ArrayList<>(meta.lore());
        lore.removeIf(line -> {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
            return plain.startsWith("Weight:") || plain.startsWith("Chance:") || plain.equals("---");
        });
        meta.lore(lore.isEmpty() ? null : lore);
        copy.setItemMeta(meta);
        return copy;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
