package com.frostsmp.chat.service;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnderChestSnapshotService {

    public enum ViewKind {
        ENDER_CHEST,
        INVENTORY
    }

    public record Snapshot(String ownerName, ItemStack[] contents, long expiresAtMs, ViewKind kind) {
    }

    public static final class SnapshotHolder implements InventoryHolder {
        private final String snapshotId;

        public SnapshotHolder(String snapshotId) {
            this.snapshotId = snapshotId;
        }

        public String snapshotId() {
            return snapshotId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final JavaPlugin plugin;
    private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
    private final long expiryMillis;

    public EnderChestSnapshotService(JavaPlugin plugin) {
        this.plugin = plugin;
        long seconds = Math.max(10L, plugin.getConfig().getLong("snapshot-expiry-seconds", 120L));
        this.expiryMillis = seconds * 1000L;
    }

    public String createSnapshot(String ownerName, ItemStack[] contents) {
        return createSnapshot(ownerName, contents, ViewKind.ENDER_CHEST);
    }

    public String createSnapshot(String ownerName, ItemStack[] contents, ViewKind kind) {
        cleanupExpired();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null ? null : contents[i].clone();
        }
        snapshots.put(id, new Snapshot(ownerName, copy, System.currentTimeMillis() + expiryMillis, kind));
        return id;
    }

    public Inventory getView(String id) {
        Snapshot snapshot = snapshots.get(id);
        if (snapshot == null || snapshot.expiresAtMs() <= System.currentTimeMillis()) {
            snapshots.remove(id);
            return null;
        }
        if (snapshot.kind() == ViewKind.INVENTORY) {
            Inventory inventory = Bukkit.createInventory(new SnapshotHolder(id), 45, snapshot.ownerName() + "'s Inventory");
            inventory.setContents(fit(snapshot.contents(), inventory.getSize()));
            return inventory;
        }
        Inventory inventory = Bukkit.createInventory(new SnapshotHolder(id), InventoryType.ENDER_CHEST, snapshot.ownerName() + "'s Ender Chest");
        inventory.setContents(fit(snapshot.contents(), inventory.getSize()));
        return inventory;
    }

    private static ItemStack[] fit(ItemStack[] contents, int size) {
        ItemStack[] fitted = new ItemStack[size];
        if (contents != null) {
            System.arraycopy(contents, 0, fitted, 0, Math.min(contents.length, size));
        }
        return fitted;
    }

    public static boolean isSnapshotView(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof SnapshotHolder;
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        snapshots.entrySet().removeIf(entry -> entry.getValue().expiresAtMs() <= now);
    }
}
