package com.frostsmp.crates.listener;

import com.frostsmp.crates.config.CrateDefinitionStorage;
import com.frostsmp.crates.gui.CrateEditorGui;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.service.PlacementService;
import com.frostsmp.crates.util.BlockMaterialUtil;
import com.frostsmp.crates.util.MessageUtil;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class CrateEditorListener implements Listener {

    private final CrateDefinitionStorage definitions;
    private final ChatInputListener chatInput;
    private final PlacementService placementService;

    public CrateEditorListener(
            CrateDefinitionStorage definitions,
            ChatInputListener chatInput,
            PlacementService placementService
    ) {
        this.definitions = definitions;
        this.chatInput = chatInput;
        this.placementService = placementService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CrateEditorGui editor)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < top.getSize()) {
            if (isSettingsSlot(rawSlot)) {
                event.setCancelled(true);
                handleSettingsClick(player, editor, rawSlot, event.getClick());
                return;
            }
            if (rawSlot >= CrateEditorGui.PRIZE_SLOTS) {
                event.setCancelled(true);
                return;
            }
            if (rawSlot < CrateEditorGui.PRIZE_SLOTS) {
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    event.setCancelled(true);
                    return;
                }
                ItemStack clicked = top.getItem(rawSlot);
                if (clicked == null || clicked.getType().isAir()) {
                    return;
                }
                if (event.getClick() != ClickType.RIGHT) {
                    return;
                }
                event.setCancelled(true);
                if (!editor.getSlotWeights().containsKey(rawSlot)) {
                    editor.getSlotWeights().put(rawSlot, 1);
                    editor.refreshAllWeightLore();
                }
                chatInput.requestInput(player, "<yellow>Enter weight for this prize (e.g. 15):", input -> {
                    try {
                        int weight = Integer.parseInt(input);
                        if (weight < 1) {
                            MessageUtil.send(player, "<red>Weight must be at least 1.</red>");
                            return;
                        }
                        editor.setSlotWeight(rawSlot, weight);
                        editor.refreshAllWeightLore();
                        MessageUtil.send(player, "<green>Weight set to <white>" + weight + "</green>.");
                    } catch (NumberFormatException e) {
                        MessageUtil.send(player, "<red>Invalid number.</red>");
                    }
                });
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CrateEditorGui)) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < top.getSize() && slot >= CrateEditorGui.PRIZE_SLOTS) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof CrateEditorGui editor)) {
            return;
        }
        for (int slot = 0; slot < CrateEditorGui.PRIZE_SLOTS; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir() && !editor.getSlotWeights().containsKey(slot)) {
                editor.getSlotWeights().put(slot, 1);
            }
        }
        CrateDefinition definition = editor.getDefinition();
        definition.setLoot(editor.collectLootEntries());
        definitions.save(definition);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatInput.clear(event.getPlayer());
    }

    private boolean isSettingsSlot(int slot) {
        return slot == CrateEditorGui.SLOT_DISPLAY_NAME
                || slot == CrateEditorGui.SLOT_BOX_COLOR
                || slot == CrateEditorGui.SLOT_BROADCAST
                || slot == CrateEditorGui.SLOT_BASE_COOLDOWN
                || slot == CrateEditorGui.SLOT_AUTO_KEY
                || slot == CrateEditorGui.SLOT_HELP;
    }

    private void handleSettingsClick(Player player, CrateEditorGui editor, int slot, ClickType click) {
        CrateDefinition definition = editor.getDefinition();
        switch (slot) {
            case CrateEditorGui.SLOT_DISPLAY_NAME -> chatInput.requestInput(
                    player,
                    "<yellow>Enter display name (MiniMessage or & codes):",
                    input -> {
                        definition.setDisplayName(input);
                        definitions.save(definition);
                        placementService.refreshDefinition(definition);
                        editor.refreshDecorations();
                        MessageUtil.send(player, "<green>Display name updated.</green>");
                    }
            );
            case CrateEditorGui.SLOT_BOX_COLOR -> chatInput.requestInput(
                    player,
                    "<yellow>Enter shulker box color (e.g. LIME_SHULKER_BOX):",
                    input -> {
                        Material material = Material.matchMaterial(input.toUpperCase());
                        if (material == null || !BlockMaterialUtil.isValidDisplayBlock(material)) {
                            MessageUtil.send(player, "<red>Invalid shulker box material.</red>");
                            return;
                        }
                        definition.setDisplayBlock(BlockMaterialUtil.normalizeDisplayBlock(material));
                        definitions.save(definition);
                        placementService.refreshDefinition(definition);
                        editor.refreshDecorations();
                        MessageUtil.send(player, "<green>Display block set to <white>" + definition.getDisplayBlock().name() + "</green>.");
                    }
            );
            case CrateEditorGui.SLOT_BROADCAST -> {
                definition.setBroadcastEnabled(!definition.isBroadcastEnabled());
                definitions.save(definition);
                editor.refreshDecorations();
                MessageUtil.send(player, "<green>Broadcast " + (definition.isBroadcastEnabled() ? "enabled" : "disabled") + ".</green>");
            }
            case CrateEditorGui.SLOT_BASE_COOLDOWN -> chatInput.requestInput(
                    player,
                    "<yellow>Enter base cooldown in minutes (e.g. 5 for 5 minutes, 0.05 for 3 seconds):",
                    input -> {
                        try {
                            double minutes = Double.parseDouble(input);
                            if (minutes < 0) {
                                MessageUtil.send(player, "<red>Cooldown cannot be negative.</red>");
                                return;
                            }
                            long ms = Math.round(minutes * 60_000L);
                            definition.setOpenCooldownMs(ms);
                            definitions.save(definition);
                            editor.refreshDecorations();
                            MessageUtil.send(player, "<green>Base cooldown set to <white>"
                                    + MessageUtil.formatDuration(ms) + "</green>.");
                        } catch (NumberFormatException e) {
                            MessageUtil.send(player, "<red>Invalid number.</red>");
                        }
                    }
            );
            case CrateEditorGui.SLOT_AUTO_KEY -> handleAutoKeyClick(player, editor, definition, click);
            default -> {
            }
        }
    }

    private void handleAutoKeyClick(Player player, CrateEditorGui editor, CrateDefinition definition, ClickType click) {
        if (click == ClickType.RIGHT) {
            chatInput.requestInput(
                    player,
                    "<yellow>Enter rank/group (or perm), minutes, and optional amount:",
                    input -> {
                        try {
                            ParsedAutoKey parsed = parseAutoKeyInput(input);
                            long intervalMs = Math.round(parsed.minutes() * 60_000L);
                            var autoKey = definition.getAutoKey();
                            autoKey.setGroup(parsed.group());
                            autoKey.setPermission(parsed.permission());
                            autoKey.setIntervalMs(intervalMs);
                            autoKey.setAmount(parsed.amount());
                            autoKey.setEnabled(true);
                            definitions.save(definition);
                            editor.refreshDecorations();
                            String target = parsed.group().isBlank()
                                    ? parsed.permission()
                                    : "group " + parsed.group();
                            MessageUtil.send(player, "<green>Auto-key timer set for <white>" + target
                                    + "</green> every <white>" + MessageUtil.formatDuration(intervalMs)
                                    + "</green> (<white>" + parsed.amount() + "</white> key(s)).");
                        } catch (IllegalArgumentException e) {
                            MessageUtil.send(player, "<red>" + e.getMessage() + "</red>");
                        }
                    }
            );
            return;
        }
        var autoKey = definition.getAutoKey();
        autoKey.setEnabled(!autoKey.isEnabled());
        definitions.save(definition);
        editor.refreshDecorations();
        MessageUtil.send(player, "<green>Auto-key timer "
                + (autoKey.isEnabled() ? "enabled" : "disabled") + ".</green>");
    }

    private ParsedAutoKey parseAutoKeyInput(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Format: group booster 1440 [amount] or booster 1440");
        }
        String group = "";
        String permission = "";
        int minuteIndex;
        String first = parts[0];
        String firstLower = first.toLowerCase(Locale.ROOT);
        if (firstLower.equals("group")) {
            if (parts.length < 3) {
                throw new IllegalArgumentException("Format: group <rank> <minutes> [amount]");
            }
            group = parts[1];
            minuteIndex = 2;
        } else if (firstLower.startsWith("group:")) {
            group = first.substring("group:".length());
            if (group.isBlank()) {
                throw new IllegalArgumentException("Format: group:<rank> <minutes> [amount]");
            }
            minuteIndex = 1;
        } else if (firstLower.equals("perm") || firstLower.equals("permission")) {
            if (parts.length < 3) {
                throw new IllegalArgumentException("Format: perm <node> <minutes> [amount]");
            }
            permission = parts[1];
            minuteIndex = 2;
        } else if (first.contains(".")) {
            permission = first;
            minuteIndex = 1;
        } else {
            group = first;
            minuteIndex = 1;
        }
        double minutes = Double.parseDouble(parts[minuteIndex]);
        if (minutes < 1) {
            throw new IllegalArgumentException("Interval must be at least 1 minute.");
        }
        int amount = 1;
        if (parts.length > minuteIndex + 1) {
            amount = Integer.parseInt(parts[minuteIndex + 1]);
            if (amount < 1) {
                throw new IllegalArgumentException("Amount must be at least 1.");
            }
        }
        if (group.isBlank() && permission.isBlank()) {
            throw new IllegalArgumentException("Set a LuckPerms group or permission.");
        }
        return new ParsedAutoKey(group, permission, minutes, amount);
    }

    private record ParsedAutoKey(String group, String permission, double minutes, int amount) {
    }
}
