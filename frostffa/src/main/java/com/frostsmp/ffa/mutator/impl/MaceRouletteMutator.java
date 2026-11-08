package com.frostsmp.ffa.mutator.impl;

import com.frostsmp.ffa.arena.MembershipCache;
import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.mutator.Mutator;
import com.frostsmp.ffa.mutator.MutatorId;
import com.frostsmp.ffa.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MaceRouletteMutator implements Mutator, Listener {

    private static final String TOKEN_ITEM =
            "minecraft:nether_star[custom_name={\"text\":\"\",\"extra\":["
                    + "{\"text\":\"K\",\"bold\":true,\"italic\":false,\"color\":\"#c77dff\"},"
                    + "{\"text\":\"i\",\"bold\":true,\"italic\":false,\"color\":\"#cd85fa\"},"
                    + "{\"text\":\"l\",\"bold\":true,\"italic\":false,\"color\":\"#d391f5\"},"
                    + "{\"text\":\"l\",\"bold\":true,\"italic\":false,\"color\":\"#da9df1\"},"
                    + "{\"text\":\" \",\"bold\":true,\"italic\":false,\"color\":\"#e0aaec\"},"
                    + "{\"text\":\"T\",\"bold\":true,\"italic\":false,\"color\":\"#e6b6e7\"},"
                    + "{\"text\":\"o\",\"bold\":true,\"italic\":false,\"color\":\"#edc2e2\"},"
                    + "{\"text\":\"k\",\"bold\":true,\"italic\":false,\"color\":\"#f3ceDD\"},"
                    + "{\"text\":\"e\",\"bold\":true,\"italic\":false,\"color\":\"#f9dbd8\"},"
                    + "{\"text\":\"n\",\"bold\":true,\"italic\":false,\"color\":\"#ffd6ff\"}"
                    + "]},custom_data={coin:1b}]";

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final MembershipCache membership;
    private final NamespacedKey maceKey;
    private final MaceRouletteDoubleJump doubleJump;

    private boolean active;
    private UUID holderId;
    private long holderLastMoveMs;
    private int tick;

    public MaceRouletteMutator(JavaPlugin plugin, PluginConfig config, MembershipCache membership) {
        this.plugin = plugin;
        this.config = config;
        this.membership = membership;
        this.maceKey = new NamespacedKey(plugin, "mace_roulette");
        this.doubleJump = new MaceRouletteDoubleJump(plugin, config, () -> holderId);
    }

    @Override
    public String id() {
        return MutatorId.MACE_ROULETTE.id();
    }

    @Override
    public String displayName() {
        return MutatorId.MACE_ROULETTE.displayName();
    }

    @Override
    public void start() {
        active = true;
        holderId = null;
        holderLastMoveMs = 0L;
        tick = 0;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        doubleJump.start();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            purgeAllMaces(null);
            assignRandom(null);
        });
    }

    @Override
    public void stop() {
        active = false;
        HandlerList.unregisterAll(this);
        doubleJump.stop();
        UUID previous = holderId;
        holderId = null;
        holderLastMoveMs = 0L;
        purgeAllMaces(null);
        if (previous != null) {
            Player holder = Bukkit.getPlayer(previous);
            if (holder != null) {
                holder.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
    }

    @Override
    public void tick() {
        if (!active) {
            return;
        }
        doubleJump.tick();
        tick++;
        if (tick % 20 != 0) {
            return;
        }
        purgeAllMaces(holderId);
        if (holderId == null) {
            assignRandom(null);
            return;
        }
        Player holder = Bukkit.getPlayer(holderId);
        if (holder == null || !holder.isOnline() || !membership.isInArena(holder)) {
            UUID previous = holderId;
            holderId = null;
            purgeAllMaces(null);
            assignRandom(previous);
            return;
        }
        ensureGlowing(holder);
        if (isAfkHolder(holder)) {
            MessageUtil.broadcast(orPrefixed(
                    "mace-roulette-afk",
                    "<gold>Mace Roulette:</gold> <white>{player}</white> <gray>was AFK — passing the mace.</gray>",
                    "player", holder.getName()
            ));
            stripGear(holder);
            UUID previous = holderId;
            holderId = null;
            holderLastMoveMs = 0L;
            doubleJump.syncHolder();
            purgeAllMaces(null);
            assignRandom(previous);
            return;
        }
        if (!hasRouletteMace(holder)) {
            if (!giveGear(holder)) {
                stripGear(holder);
                UUID previous = holderId;
                holderId = null;
                purgeAllMaces(null);
                assignRandom(previous);
            }
        }
    }

    @Override
    public void onEnter(Player player) {
        // Double jump is holder-only; sync happens on assign / tick.
    }

    @Override
    public void onExit(Player player) {
        doubleJump.onExit(player);
        if (!active || holderId == null || !holderId.equals(player.getUniqueId())) {
            return;
        }
        stripGear(player);
        holderId = null;
        holderLastMoveMs = 0L;
        doubleJump.syncHolder();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            purgeAllMaces(null);
            assignRandom(player.getUniqueId());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHolderMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!active || holderId == null || event.getTo() == null) {
            return;
        }
        if (!holderId.equals(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        holderLastMoveMs = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!active) {
            return;
        }
        if (isMace(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!active) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (isMace(stack)) {
            event.setCancelled(true);
            event.getItem().remove();
            return;
        }
        if (stripNestedMaces(stack)) {
            event.getItem().setItemStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!active) {
            return;
        }
        ItemStack stack = event.getEntity().getItemStack();
        if (isMace(stack)) {
            event.setCancelled(true);
            return;
        }
        if (stripNestedMaces(stack)) {
            event.getEntity().setItemStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (!active) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (isMace(stack)) {
            event.setCancelled(true);
            event.getItem().remove();
            return;
        }
        if (stripNestedMaces(stack)) {
            event.getItem().setItemStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!active) {
            return;
        }
        ItemStack stack = event.getItem();
        if (isMace(stack)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!active) {
            return;
        }
        purgePlayer(event.getPlayer(), isHolder(event.getPlayer()) ? 1 : 0);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!active) {
            return;
        }
        purgeChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!active || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (!isHolder(player) && (isMace(current) || isMace(cursor))) {
            event.setCancelled(true);
            if (isMace(current)) {
                event.setCurrentItem(null);
            }
            if (isMace(cursor)) {
                player.setItemOnCursor(null);
            }
            return;
        }
        if (!isHolder(player)) {
            return;
        }
        boolean involves = isRouletteMace(current) || isRouletteMace(cursor);
        if (!involves) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        InventoryType topType = top.getType();
        boolean storageView = topType != InventoryType.CRAFTING && topType != InventoryType.PLAYER;
        if (event.isShiftClick() && storageView) {
            event.setCancelled(true);
            return;
        }
        Inventory clicked = event.getClickedInventory();
        if (clicked != null && clicked.equals(top) && storageView) {
            event.setCancelled(true);
            return;
        }
        if (event.getHotbarButton() >= 0 && storageView) {
            ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
            if (isRouletteMace(hotbar) || isRouletteMace(current)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!active || !(event.getWhoClicked() instanceof Player player) || !isHolder(player)) {
            return;
        }
        if (!isRouletteMace(event.getOldCursor()) && event.getNewItems().values().stream().noneMatch(this::isRouletteMace)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        InventoryType topType = top.getType();
        if (topType == InventoryType.CRAFTING || topType == InventoryType.PLAYER) {
            return;
        }
        int topSize = top.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!active || !(event.getPlayer() instanceof Player player) || !isHolder(player)) {
            return;
        }
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            event.setCancelled(true);
            MessageUtil.send(player, config.prefixed("mace-roulette-no-echest"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!active || !isHolder(event.getPlayer())) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.ENDER_CHEST) {
            event.setCancelled(true);
            MessageUtil.send(event.getPlayer(), config.prefixed("mace-roulette-no-echest"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (!active) {
            return;
        }
        event.getDrops().removeIf(MaceRouletteMutator::isMace);
        event.getDrops().replaceAll(stack -> {
            if (stack != null) {
                stripNestedMaces(stack);
            }
            return stack;
        });
        if (holderId == null || !holderId.equals(event.getEntity().getUniqueId())) {
            return;
        }

        Player victim = event.getEntity();
        stripGear(victim);

        Player killer = victim.getKiller();
        boolean validKiller = killer != null && !killer.equals(victim) && membership.isInArena(killer);
        if (validKiller) {
            giveKillTokens(killer, config.maceRouletteKillTokens());
        }

        holderId = null;
        purgeAllMaces(null);

        if (validKiller && canFitGear(killer)) {
            assign(killer, killer, victim, false);
            return;
        }

        Player next = pickRandom(victim.getUniqueId());
        if (next != null) {
            assign(next, validKiller ? killer : null, victim, validKiller);
        } else {
            doubleJump.syncHolder();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!active || holderId == null || !holderId.equals(event.getPlayer().getUniqueId())) {
            return;
        }
        stripGear(event.getPlayer());
        UUID previous = holderId;
        holderId = null;
        holderLastMoveMs = 0L;
        doubleJump.syncHolder();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            purgeAllMaces(null);
            assignRandom(previous);
        });
    }

    private void assignRandom(UUID exclude) {
        Player next = pickRandom(exclude);
        if (next == null) {
            holderId = null;
            purgeAllMaces(null);
            return;
        }
        assign(next, null, null, false);
    }

    private Player pickRandom(UUID exclude) {
        if (!active) {
            return null;
        }
        List<Player> candidates = new ArrayList<>();
        for (UUID id : membership.members()) {
            if (exclude != null && exclude.equals(id)) {
                continue;
            }
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline() || player.isDead() || !membership.isInArena(player)) {
                continue;
            }
            if (canFitGear(player)) {
                candidates.add(player);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private void assign(Player player, Player killer, Player victim, boolean inventoryFull) {
        if (!active || player == null || !player.isOnline() || player.isDead()) {
            return;
        }
        if (holderId != null && holderId.equals(player.getUniqueId()) && hasRouletteMace(player)) {
            ensureGlowing(player);
            purgeAllMaces(holderId);
            return;
        }
        if (holderId != null) {
            Player previous = Bukkit.getPlayer(holderId);
            if (previous != null) {
                stripGear(previous);
            }
        }
        holderId = null;
        purgeAllMaces(null);
        if (!giveGear(player)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> assignRandom(player.getUniqueId()));
            return;
        }
        holderId = player.getUniqueId();
        holderLastMoveMs = System.currentTimeMillis();
        ensureGlowing(player);
        doubleJump.syncHolder();
        announceHolder(player, killer, victim, inventoryFull);
        MessageUtil.showTitle(
                player,
                firstNonBlank(config.msg("mace-roulette-you-title"), "<gold>MACE ROULETTE</gold>"),
                firstNonBlank(config.msg("mace-roulette-you-subtitle"), "<white>You are the mace holder.</white>"),
                config.titleFadeInTicks(),
                config.titleStayTicks(),
                config.titleFadeOutTicks()
        );
    }

    private void announceHolder(Player holder, Player killer, Player victim, boolean inventoryFull) {
        if (killer != null && victim != null && inventoryFull) {
            MessageUtil.broadcast(orPrefixed("mace-roulette-passed",
                    "<gold>Mace Roulette:</gold> <white>{killer}</white> <gray>killed</gray> <white>{victim}</white> <gray>but their inventory was full.</gray> <white>{player}</white> <gray>now holds the mace.</gray>",
                    "killer", killer.getName(),
                    "victim", victim.getName(),
                    "player", holder.getName()));
            return;
        }
        if (killer != null && victim != null) {
            MessageUtil.broadcast(orPrefixed("mace-roulette-taken",
                    "<gold>Mace Roulette:</gold> <white>{killer}</white> <gray>killed</gray> <white>{victim}</white> <gray>and took the mace.</gray>",
                    "killer", killer.getName(),
                    "victim", victim.getName()));
            return;
        }
        MessageUtil.broadcast(config.prefixed("mace-roulette-holder", "player", holder.getName()));
    }

    private boolean isAfkHolder(Player holder) {
        int seconds = config.maceRouletteAfkSeconds();
        if (seconds <= 0 || holderLastMoveMs <= 0L) {
            return false;
        }
        return System.currentTimeMillis() - holderLastMoveMs >= seconds * 1000L;
    }

    private String orPrefixed(String key, String fallback, String... kv) {
        String body = config.msg(key);
        if (body == null || body.isBlank()) {
            body = fallback;
        }
        return config.plugin().getConfig().getString("messages.prefix", "") + MessageUtil.format(body, kv);
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean giveGear(Player player) {
        if (!canFitGear(player)) {
            return false;
        }
        ItemStack mace = createMace();
        Map<Integer, ItemStack> leftoverMace = player.getInventory().addItem(mace);
        if (!leftoverMace.isEmpty()) {
            stripGear(player);
            return false;
        }
        return true;
    }

    private boolean canFitGear(Player player) {
        ItemStack mace = createMace();
        ItemStack[] snapshot = player.getInventory().getStorageContents();
        ItemStack[] copy = new ItemStack[snapshot.length];
        for (int i = 0; i < snapshot.length; i++) {
            copy[i] = snapshot[i] == null ? null : snapshot[i].clone();
        }
        return firstFit(copy, mace);
    }

    private static boolean firstFit(ItemStack[] contents, ItemStack toAdd) {
        int remaining = toAdd.getAmount();
        int max = toAdd.getMaxStackSize();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack slot = contents[i];
            if (slot == null || slot.getType().isAir()) {
                int place = Math.min(remaining, max);
                ItemStack placed = toAdd.clone();
                placed.setAmount(place);
                contents[i] = placed;
                remaining -= place;
            } else if (slot.isSimilar(toAdd) && slot.getAmount() < max) {
                int space = max - slot.getAmount();
                int place = Math.min(remaining, space);
                slot.setAmount(slot.getAmount() + place);
                remaining -= place;
            }
        }
        return remaining <= 0;
    }

    private void stripGear(Player player) {
        purgePlayer(player, 0);
        player.removePotionEffect(PotionEffectType.GLOWING);
    }

    private void ensureGlowing(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING,
                40,
                0,
                false,
                false,
                true
        ));
    }

    private void giveKillTokens(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        ItemStack template = Bukkit.getItemFactory().createItemStack(TOKEN_ITEM);
        for (int i = 0; i < amount; i++) {
            ItemStack token = template.clone();
            token.setAmount(1);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(token);
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
    }

    private ItemStack createMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();
        meta.displayName(MessageUtil.parse("<gold><bold>Mace Roulette</bold></gold>"));
        meta.lore(List.of(
                MessageUtil.parse("<gray>Soulbound — cannot drop or store.</gray>"),
                MessageUtil.parse("<gray>Passes to your killer on death.</gray>")
        ));
        meta.addEnchant(Enchantment.WIND_BURST, 1, true);
        meta.getPersistentDataContainer().set(maceKey, PersistentDataType.BYTE, (byte) 1);
        mace.setItemMeta(meta);
        return mace;
    }

    private boolean isHolder(Player player) {
        return holderId != null && holderId.equals(player.getUniqueId());
    }

    private boolean hasRouletteMace(Player player) {
        if (isRouletteMace(player.getItemOnCursor())) {
            return true;
        }
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isRouletteMace(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRouletteMace(ItemStack stack) {
        if (stack == null || stack.getType() != Material.MACE || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(maceKey, PersistentDataType.BYTE);
    }

    private static boolean isMace(ItemStack stack) {
        return stack != null && stack.getType() == Material.MACE;
    }

    private void purgeAllMaces(UUID keepHolder) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int allowed = keepHolder != null && keepHolder.equals(player.getUniqueId()) ? 1 : 0;
            purgePlayer(player, allowed);
        }
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                ItemStack stack = item.getItemStack();
                if (isMace(stack)) {
                    item.remove();
                } else if (stripNestedMaces(stack)) {
                    item.setItemStack(stack);
                }
            }
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
                ItemStack stack = frame.getItem();
                if (isMace(stack)) {
                    frame.setItem(new ItemStack(Material.AIR));
                } else if (stripNestedMaces(stack)) {
                    frame.setItem(stack);
                }
            }
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player) {
                    continue;
                }
                if (entity instanceof LivingEntity living) {
                    purgeEquipment(living);
                }
                if (entity instanceof InventoryHolder holder) {
                    purgeInventory(holder.getInventory(), 0);
                }
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                purgeChunk(chunk);
            }
        }
    }

    private void purgeChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof InventoryHolder holder) {
                if (purgeInventory(holder.getInventory(), 0).changed()) {
                    state.update();
                }
            }
        }
    }

    private void purgePlayer(Player player, int allowed) {
        allowed -= purgeInventory(player.getInventory(), allowed).kept();
        ItemStack cursor = player.getItemOnCursor();
        if (isMace(cursor)) {
            if (allowed > 0) {
                if (cursor.getAmount() > 1) {
                    cursor.setAmount(1);
                }
                ensureRouletteTag(cursor);
                player.setItemOnCursor(cursor);
            } else {
                player.setItemOnCursor(null);
            }
        } else if (stripNestedMaces(cursor)) {
            player.setItemOnCursor(cursor);
        }
        purgeInventory(player.getEnderChest(), 0);
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top != player.getInventory()) {
            purgeInventory(top, 0);
        }
    }

    private PurgeCount purgeInventory(Inventory inventory, int allowed) {
        int remaining = allowed;
        boolean changed = false;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (isMace(stack)) {
                if (remaining > 0) {
                    if (stack.getAmount() > 1) {
                        stack.setAmount(1);
                        changed = true;
                    }
                    ensureRouletteTag(stack);
                    inventory.setItem(i, stack);
                    remaining--;
                } else {
                    inventory.setItem(i, null);
                    changed = true;
                }
                continue;
            }
            if (stripNestedMaces(stack)) {
                inventory.setItem(i, stack);
                changed = true;
            }
        }
        return new PurgeCount(allowed - remaining, changed);
    }

    private void purgeEquipment(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                ItemStack stack = equipment.getItem(slot);
                if (isMace(stack)) {
                    equipment.setItem(slot, null);
                } else if (stripNestedMaces(stack)) {
                    equipment.setItem(slot, stack);
                }
            } catch (IllegalArgumentException ignored) {
                // Slot not valid for this entity.
            }
        }
    }

    private void ensureRouletteTag(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || meta.getPersistentDataContainer().has(maceKey, PersistentDataType.BYTE)) {
            return;
        }
        meta.getPersistentDataContainer().set(maceKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
    }

    private boolean stripNestedMaces(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        boolean changed = false;
        if (meta instanceof BundleMeta bundle) {
            List<ItemStack> next = new ArrayList<>();
            for (ItemStack inner : bundle.getItems()) {
                if (inner == null || isMace(inner)) {
                    changed = true;
                    continue;
                }
                if (stripNestedMaces(inner)) {
                    changed = true;
                }
                next.add(inner);
            }
            if (changed) {
                bundle.setItems(next);
                stack.setItemMeta(bundle);
            }
        }
        if (meta instanceof BlockStateMeta blockMeta && blockMeta.hasBlockState()) {
            BlockState nestedState = blockMeta.getBlockState();
            if (nestedState instanceof InventoryHolder holder
                    && purgeInventory(holder.getInventory(), 0).changed()) {
                blockMeta.setBlockState(nestedState);
                stack.setItemMeta(blockMeta);
                changed = true;
            }
        }
        return changed;
    }

    private record PurgeCount(int kept, boolean changed) {
    }
}
