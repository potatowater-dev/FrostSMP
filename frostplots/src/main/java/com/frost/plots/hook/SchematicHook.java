package com.frost.plots.hook;

import com.frost.plots.FrostPlotsPlugin;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;

public final class SchematicHook {

    private final FrostPlotsPlugin plugin;

    public SchematicHook(FrostPlotsPlugin plugin) {
        this.plugin = plugin;
    }

    public File resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        File file = new File(relativePath);
        if (!file.isAbsolute()) {
            file = new File(plugin.getDataFolder(), relativePath);
        }
        return file;
    }

    public boolean paste(World world, int x, int y, int z, String relativePath, boolean ignoreAir) {
        File file = resolve(relativePath);
        if (file == null || !file.isFile()) {
            return false;
        }
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            plugin.getLogger().warning("Unknown schematic format: " + file.getAbsolutePath());
            return false;
        }
        try (FileInputStream input = new FileInputStream(file);
             ClipboardReader reader = format.getReader(input)) {
            Clipboard clipboard = reader.read();
            try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(session)
                        .to(BlockVector3.at(x, y, z))
                        .ignoreAirBlocks(ignoreAir)
                        .build();
                Operations.complete(operation);
            }
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to paste schematic " + file.getAbsolutePath(), exception);
            return false;
        }
    }

    public void clearCuboid(World world, BlockVector3 min, BlockVector3 max) {
        if (world == null || BlockTypes.AIR == null) {
            return;
        }
        try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            CuboidRegion region = new CuboidRegion(BukkitAdapter.adapt(world), min, max);
            session.setBlocks(region, BlockTypes.AIR.getDefaultState());
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to clear plot schematic area", exception);
        }
    }

    public void placeGrassPlatform(World world, int centerX, int baseY, int centerZ, int size) {
        int extent = Math.max(1, size);
        int minX = centerX - extent / 2;
        int maxX = minX + extent - 1;
        int minZ = centerZ - extent / 2;
        int maxZ = minZ + extent - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.getChunkAt(x >> 4, z >> 4).load(true);
                Block block = world.getBlockAt(x, baseY, z);
                if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR || block.getType() == Material.VOID_AIR
                        || block.getType() == Material.GRASS_BLOCK) {
                    block.setType(Material.GRASS_BLOCK, false);
                }
            }
        }
    }
}
