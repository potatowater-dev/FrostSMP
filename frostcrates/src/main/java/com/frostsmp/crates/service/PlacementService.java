package com.frostsmp.crates.service;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.config.CrateDefinitionStorage;
import com.frostsmp.crates.config.PlacedCrateStorage;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.PlacedCrate;
import com.frostsmp.crates.util.BlockMaterialUtil;
import com.frostsmp.crates.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class PlacementService {

    private final FrostCratesPlugin plugin;
    private final PlacedCrateStorage placedCrates;
    private final HologramService hologramService;
    private CrateDefinitionStorage definitions;

    public PlacementService(FrostCratesPlugin plugin, PlacedCrateStorage placedCrates, HologramService hologramService) {
        this.plugin = plugin;
        this.placedCrates = placedCrates;
        this.hologramService = hologramService;
    }

    public void setDefinitions(CrateDefinitionStorage definitions) {
        this.definitions = definitions;
    }

    public boolean placeCrate(Player player, Block target, CrateDefinition definition) {
        if (target == null || definition == null) {
            return false;
        }
        Location location = target.getLocation();
        if (placedCrates.getAt(location) != null) {
            MessageUtil.send(player, "<red>A crate is already registered at this location.</red>");
            return false;
        }
        Material displayBlock = BlockMaterialUtil.normalizeDisplayBlock(definition.getDisplayBlock());
        if (!BlockMaterialUtil.isValidDisplayBlock(displayBlock)) {
            MessageUtil.send(player, "<red>Invalid display block for crate definition.</red>");
            return false;
        }
        target.setType(displayBlock, false);
        PlacedCrate placed = placedCrates.register(location, definition.getId());
        hologramService.spawn(placed, definition);
        MessageUtil.send(player, "<green>Placed <aqua>" + definition.getId() + "</green> crate at "
                + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ".");
        return true;
    }

    public boolean removeCrate(Player player, Location location) {
        PlacedCrate placed = placedCrates.getAt(location);
        if (placed == null) {
            MessageUtil.send(player, "<red>No registered crate at this location.</red>");
            return false;
        }
        hologramService.remove(placed);
        placedCrates.unregister(location);
        MessageUtil.send(player, "<yellow>Removed crate registration at this location.</yellow>");
        return true;
    }

    public void unregisterAt(Location location) {
        PlacedCrate placed = placedCrates.getAt(location);
        if (placed == null) {
            return;
        }
        hologramService.remove(placed);
        placedCrates.unregister(location);
    }

    public int pruneInvalidPlacements() {
        if (definitions == null) {
            return 0;
        }
        if (definitions.getIds().isEmpty()) {
            plugin.getLogger().severe(
                    "Skipped placement cleanup — no crate definitions are loaded. "
                            + "Check plugins/FrostCrates/definitions/ before reloading."
            );
            return 0;
        }
        int removed = 0;
        for (PlacedCrate placed : new java.util.ArrayList<>(placedCrates.getAll())) {
            CrateDefinition definition = definitions.get(placed.getDefinitionId());
            if (definition == null) {
                hologramService.remove(placed);
                placedCrates.unregister(placed.getLocation());
                removed++;
                continue;
            }
            Block block = placed.getLocation().getBlock();
            Material expected = BlockMaterialUtil.normalizeDisplayBlock(definition.getDisplayBlock());
            if (block.getType() != expected) {
                hologramService.remove(placed);
                placedCrates.unregister(placed.getLocation());
                removed++;
            }
        }
        return removed;
    }

    public void refreshDefinition(CrateDefinition definition) {
        Material displayBlock = BlockMaterialUtil.normalizeDisplayBlock(definition.getDisplayBlock());
        for (PlacedCrate placed : placedCrates.getAll()) {
            if (!placed.getDefinitionId().equalsIgnoreCase(definition.getId())) {
                continue;
            }
            placed.getLocation().getBlock().setType(displayBlock, false);
            hologramService.spawn(placed, definition);
        }
    }
}
