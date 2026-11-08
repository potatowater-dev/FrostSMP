package com.frostsmp.crates.service;

import com.frostsmp.crates.config.CrateDefinitionStorage;
import com.frostsmp.crates.config.PlacedCrateStorage;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.PlacedCrate;
import com.frostsmp.crates.util.BlockMaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public final class CrateService {

    private final CrateDefinitionStorage definitions;
    private final PlacedCrateStorage placedCrates;

    public CrateService(CrateDefinitionStorage definitions, PlacedCrateStorage placedCrates) {
        this.definitions = definitions;
        this.placedCrates = placedCrates;
    }

    public PlacedCrate getPlacedAt(Location location) {
        return placedCrates.getAt(location);
    }

    public CrateDefinition getDefinition(PlacedCrate placed) {
        return definitions.get(placed.getDefinitionId());
    }

    public CrateDefinition getDefinition(String id) {
        return definitions.get(id);
    }

    public boolean isRegisteredDisplayBlock(Block block) {
        if (block == null) {
            return false;
        }
        PlacedCrate placed = placedCrates.getAt(block.getLocation());
        if (placed == null) {
            return false;
        }
        CrateDefinition definition = definitions.get(placed.getDefinitionId());
        return definition != null && block.getType() == BlockMaterialUtil.normalizeDisplayBlock(definition.getDisplayBlock());
    }

    public void repairBlockIfNeeded(Block block) {
        PlacedCrate placed = placedCrates.getAt(block.getLocation());
        if (placed == null) {
            return;
        }
        CrateDefinition definition = definitions.get(placed.getDefinitionId());
        if (definition == null) {
            placedCrates.unregister(block.getLocation());
            return;
        }
        Material expected = BlockMaterialUtil.normalizeDisplayBlock(definition.getDisplayBlock());
        if (block.getType() != expected) {
            block.setType(expected, false);
        }
    }

    public boolean isDisplayBlockMaterial(Material material) {
        return BlockMaterialUtil.isValidDisplayBlock(material);
    }

    /** @deprecated use {@link #isDisplayBlockMaterial(Material)} */
    @Deprecated
    public boolean isCandleMaterial(Material material) {
        return isDisplayBlockMaterial(material);
    }
}
