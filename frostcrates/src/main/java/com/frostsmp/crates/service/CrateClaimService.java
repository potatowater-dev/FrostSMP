package com.frostsmp.crates.service;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.config.MessagesConfig;
import com.frostsmp.crates.config.PluginConfig;
import com.frostsmp.crates.gui.CrateClaimGui;
import com.frostsmp.crates.model.CrateDefinition;
import com.frostsmp.crates.model.LootEntry;
import com.frostsmp.crates.util.BlockMaterialUtil;
import com.frostsmp.crates.util.CooldownManager;
import com.frostsmp.crates.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class CrateClaimService {

    private final FrostCratesPlugin plugin;
    private final PluginConfig config;
    private final KeyService keyService;
    private final LootService lootService;
    private final CooldownManager cooldownManager;
    private final HologramService hologramService;
    private final PermissionLookupService permissionLookup;
    private final MessagesConfig messages;
    private final ClaimSessionRegistry sessions = new ClaimSessionRegistry();

    public CrateClaimService(
            FrostCratesPlugin plugin,
            PluginConfig config,
            MessagesConfig messages,
            KeyService keyService,
            LootService lootService,
            CooldownManager cooldownManager,
            HologramService hologramService,
            PermissionLookupService permissionLookup
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.keyService = keyService;
        this.lootService = lootService;
        this.cooldownManager = cooldownManager;
        this.hologramService = hologramService;
        this.permissionLookup = permissionLookup;
    }

    public ClaimSessionRegistry getSessions() {
        return sessions;
    }

    public void claim(Player player, CrateDefinition definition, String cooldownKey) {
        if (sessions.isRolling(player)) {
            return;
        }
        long cooldownMs = CrateCooldownService.resolveOpenCooldownMs(player, definition);
        if (cooldownMs > 0L && cooldownManager.isOnCooldown(cooldownKey)) {
            long remaining = cooldownManager.getRemainingMs(cooldownKey);
            messages.send(player, "cooldown-active", "time", MessageUtil.formatDuration(remaining));
            return;
        }
        if (!definition.hasLoot()) {
            MessageUtil.send(player, "<red>This crate has no loot configured.</red>");
            return;
        }
        if (!keyService.tryConsume(player, definition.getId())) {
            String message = definition.getNoKeyMessage() != null
                    ? definition.getNoKeyMessage()
                    : config.getDefaultNoKeyMessage();
            Component component = MessageUtil.parse(config.getPrefix() + message);
            player.sendMessage(component);
            if (config.isActionBarNoKey()) {
                player.sendActionBar(component);
            }
            return;
        }
        hologramService.refreshKeysFor(player);
        LootEntry reward = lootService.roll(definition);
        if (reward == null) {
            keyService.give(player, definition.getId(), 1);
            hologramService.refreshKeysFor(player);
            MessageUtil.send(player, "<red>Failed to roll loot. Your key was refunded.</red>");
            return;
        }
        if (shouldSkipAnimation(player)) {
            applyOpenCooldown(cooldownKey, cooldownMs);
            lootService.giveReward(player, reward.getItem());
            lootService.maybeBroadcast(player, definition, reward);
            player.playSound(player.getLocation(), config.getClaimSuccessSound(), config.getClaimSuccessVolume(), config.getClaimSuccessPitch());
            hologramService.refreshKeysFor(player);
            return;
        }
        startClaim(player, definition, reward, cooldownKey, cooldownMs);
    }

    /**
     * Open keys for this crate instantly (sneak + right-click).
     * Stops when inventory cannot fit the next reward — leftover keys are kept.
     */
    public void claimAll(Player player, CrateDefinition definition, String cooldownKey) {
        if (sessions.isRolling(player)) {
            return;
        }
        long cooldownMs = CrateCooldownService.resolveOpenCooldownMs(player, definition);
        if (cooldownMs > 0L && cooldownManager.isOnCooldown(cooldownKey)) {
            long remaining = cooldownManager.getRemainingMs(cooldownKey);
            messages.send(player, "cooldown-active", "time", MessageUtil.formatDuration(remaining));
            return;
        }
        if (!definition.hasLoot()) {
            MessageUtil.send(player, "<red>This crate has no loot configured.</red>");
            return;
        }

        int available = keyService.get(player, definition.getId());
        if (available < 1) {
            String message = definition.getNoKeyMessage() != null
                    ? definition.getNoKeyMessage()
                    : config.getDefaultNoKeyMessage();
            Component component = MessageUtil.parse(config.getPrefix() + message);
            player.sendMessage(component);
            if (config.isActionBarNoKey()) {
                player.sendActionBar(component);
            }
            return;
        }

        int amount = Math.min(available, config.getClaimBatchMaxKeys());
        int opened = 0;
        boolean stoppedForSpace = false;

        for (int i = 0; i < amount; i++) {
            LootEntry reward = lootService.roll(definition);
            if (reward == null) {
                MessageUtil.send(player, "<red>Failed to roll loot. Remaining keys were not taken.</red>");
                break;
            }
            if (!lootService.canFullyFit(player, reward.getItem())) {
                stoppedForSpace = true;
                break;
            }
            if (!keyService.tryConsume(player, definition.getId(), 1)) {
                break;
            }
            lootService.giveReward(player, reward.getItem());
            lootService.maybeBroadcast(player, definition, reward);
            opened++;
        }

        if (opened > 0) {
            applyOpenCooldown(cooldownKey, cooldownMs);
            player.playSound(
                    player.getLocation(),
                    config.getClaimSuccessSound(),
                    config.getClaimSuccessVolume(),
                    config.getClaimSuccessPitch()
            );
            messages.send(
                    player,
                    "claim-batch-opened",
                    "amount", String.valueOf(opened),
                    "crate", definition.getDisplayName()
            );
        }
        if (stoppedForSpace) {
            messages.send(
                    player,
                    "claim-batch-inventory-full",
                    "amount", String.valueOf(opened),
                    "crate", definition.getDisplayName()
            );
        }
        hologramService.refreshKeysFor(player);
    }

    private boolean shouldSkipAnimation(Player player) {
        if (!config.isClaimAnimationEnabled()) {
            return true;
        }
        if (!config.isClaimAnimationAllowSkipPermission()) {
            return false;
        }
        if (permissionLookup.isLuckPermsAvailable()) {
            return permissionLookup.hasPermission(player.getUniqueId(), "frostsmp.crates.skip-animation");
        }
        return player.hasPermission("frostsmp.crates.skip-animation");
    }

    public void startClaim(Player player, CrateDefinition definition, LootEntry reward, String cooldownKey, long cooldownMs) {
        applyOpenCooldown(cooldownKey, cooldownMs);
        CrateClaimGui gui = new CrateClaimGui(definition);
        sessions.start(player, gui, definition, reward);
        player.openInventory(gui.getInventory());

        Component rollingBar = MessageUtil.parse(BlockMaterialUtil.rollingActionBarTag(definition.getDisplayBlock()));
        player.sendActionBar(rollingBar);

        List<ItemStack> scrollPool = buildScrollPool(definition, reward);
        scrollPool.add(reward.getItem().clone());
        int rewardIndex = scrollPool.size() - 1;
        int pointerIndex = CrateClaimGui.POINTER_SLOT - CrateClaimGui.SPIN_START;
        int targetOffset = Math.max(0, rewardIndex - pointerIndex);
        int startOffset = Math.max(0, targetOffset - config.getClaimScrollDistance());
        int totalTicks = config.getClaimDurationTicks();

        gui.syncSpinRow(player, readWindow(scrollPool, startOffset));

        BukkitRunnable animation = new BukkitRunnable() {
            int tick = 0;
            double lastProgress = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    sessions.end(player);
                    cancel();
                    return;
                }
                ClaimSessionRegistry.Session session = sessions.get(player);
                if (session == null || !session.isRolling()) {
                    cancel();
                    return;
                }
                CrateClaimGui openGui = session.getGui();
                ensureClaimGuiOpen(player, session, openGui);

                if (tick >= totalTicks) {
                    openGui.syncSpinRow(player, readWindow(scrollPool, targetOffset));
                    openGui.syncHighlightWinner(player);
                    completeClaim(player, definition, reward, openGui);
                    cancel();
                    return;
                }

                player.sendActionBar(rollingBar);

                double progress = (double) tick / totalTicks;
                double eased = easeOutQuint(progress);
                int currentOffset = startOffset + (int) Math.round((targetOffset - startOffset) * eased);
                openGui.syncSpinRow(player, readWindow(scrollPool, currentOffset));

                double velocity = eased - lastProgress;
                lastProgress = eased;
                float pitch = 0.6f + (float) (velocity * 20);
                float volume = 0.25f + (float) Math.min(0.35, velocity * 3);
                if (velocity > 0.001) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch);
                }

                tick++;
            }
        };
        ClaimSessionRegistry.Session session = sessions.get(player);
        if (session != null) {
            session.setAnimationTask(animation);
        }
        animation.runTaskTimer(plugin, 2L, 1L);
    }

    private void completeClaim(Player player, CrateDefinition definition, LootEntry reward, CrateClaimGui gui) {
        ClaimSessionRegistry.Session session = sessions.get(player);
        if (session == null || !session.isRolling()) {
            return;
        }
        boolean granted = tryGrantReward(player, session);
        if (granted) {
            lootService.maybeBroadcast(player, definition, reward);
            player.playSound(player.getLocation(), config.getClaimSuccessSound(), config.getClaimSuccessVolume(), config.getClaimSuccessPitch());
            hologramService.refreshKeysFor(player);
            player.sendActionBar(MessageUtil.parse(definition.getDisplayName()));
        }
        session.finishRolling();
        session.setSuppressReopen(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.closeInventory();
                }
                sessions.end(player);
            }
        }.runTaskLater(plugin, 40L);
    }

    /** Force the claim GUI back open if the client dropped it mid-roll. */
    public void ensureClaimGuiOpen(Player player, ClaimSessionRegistry.Session session, CrateClaimGui gui) {
        if (player == null || session == null || gui == null) {
            return;
        }
        if (!session.isRolling() || session.isSuppressReopen()) {
            return;
        }
        if (gui.isOpenFor(player)) {
            return;
        }
        player.openInventory(gui.getInventory());
    }

    /**
     * Player closed the rolling GUI (escape / desync). Reopen once or twice;
     * if it stays closed (spam escape / client refuse), settle and grant once.
     */
    public void handleRollingInventoryClose(Player player) {
        ClaimSessionRegistry.Session session = sessions.get(player);
        if (session == null || !session.isRolling() || session.isSuppressReopen()) {
            return;
        }

        int attempts = session.incrementCloseAttempts();
        // Spam escape: stop fighting and deliver the pending reward exactly once.
        if (attempts >= 3) {
            settleEarly(player, session);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                ClaimSessionRegistry.Session live = sessions.get(player);
                if (live == null || !live.isRolling() || live.isSuppressReopen()) {
                    return;
                }
                ensureClaimGuiOpen(player, live, live.getGui());
                // If client still refused reopen, settle with single grant.
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ClaimSessionRegistry.Session again = sessions.get(player);
                        if (again == null || !again.isRolling() || again.isSuppressReopen()) {
                            return;
                        }
                        if (!again.getGui().isOpenFor(player)) {
                            settleEarly(player, again);
                        }
                    }
                }.runTaskLater(plugin, 2L);
            }
        }.runTask(plugin);
    }

    private void settleEarly(Player player, ClaimSessionRegistry.Session session) {
        if (session == null || !session.isRolling()) {
            return;
        }
        session.setSuppressReopen(true);
        session.cancelAnimation();
        boolean granted = tryGrantReward(player, session);
        if (granted) {
            lootService.maybeBroadcast(player, session.getDefinition(), session.getReward());
            if (player.isOnline()) {
                player.playSound(
                        player.getLocation(),
                        config.getClaimSuccessSound(),
                        config.getClaimSuccessVolume(),
                        config.getClaimSuccessPitch()
                );
                player.sendActionBar(MessageUtil.parse(session.getDefinition().getDisplayName()));
            }
            hologramService.refreshKeysFor(player);
        }
        if (player.isOnline()) {
            player.closeInventory();
        }
        session.finishRolling();
        sessions.end(player);
    }

    /** Grants the session reward at most once (escape spam / double complete safe). */
    private boolean tryGrantReward(Player player, ClaimSessionRegistry.Session session) {
        if (player == null || session == null || session.getReward() == null) {
            return false;
        }
        if (!session.tryMarkRewardGranted()) {
            return false;
        }
        lootService.giveReward(player, session.getReward().getItem());
        return true;
    }

    private void applyOpenCooldown(String cooldownKey, long cooldownMs) {
        if (cooldownMs > 0L) {
            cooldownManager.setCooldown(cooldownKey, cooldownMs);
        }
    }

    /** Ends an active roll immediately — used when the player enters combat. */
    public void interruptForCombat(Player player) {
        ClaimSessionRegistry.Session session = sessions.get(player);
        if (session == null || !session.isRolling()) {
            return;
        }
        finishSession(player, session);
    }

    public void handlePlayerQuit(Player player) {
        ClaimSessionRegistry.Session session = sessions.get(player);
        if (session != null && session.isRolling()) {
            session.setSuppressReopen(true);
            tryGrantReward(player, session);
            session.finishRolling();
        }
        sessions.end(player);
    }

    public void cancelAllSessions(boolean refundKeys) {
        for (java.util.UUID playerId : new java.util.ArrayList<>(sessions.getActivePlayers())) {
            Player player = plugin.getServer().getPlayer(playerId);
            ClaimSessionRegistry.Session session = sessions.get(playerId);
            if (session == null) {
                sessions.end(playerId);
                continue;
            }
            session.cancelAnimation();
            if (refundKeys && player != null) {
                keyService.give(player, session.getDefinition().getId(), 1);
                hologramService.refreshKeysFor(player);
            } else if (session.isRolling() && player != null) {
                tryGrantReward(player, session);
            }
            session.setSuppressReopen(true);
            session.finishRolling();
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
            sessions.end(playerId);
        }
    }

    private void finishSession(Player player, ClaimSessionRegistry.Session session) {
        session.setSuppressReopen(true);
        session.cancelAnimation();
        session.finishRolling();
        tryGrantReward(player, session);
        if (player.isOnline()) {
            player.closeInventory();
        }
        sessions.end(player);
    }

    private ItemStack[] readWindow(List<ItemStack> pool, int offset) {
        ItemStack[] row = new ItemStack[7];
        for (int i = 0; i < row.length; i++) {
            int index = offset + i;
            row[i] = index >= 0 && index < pool.size() ? pool.get(index).clone() : new ItemStack(org.bukkit.Material.BARRIER);
        }
        return row;
    }

    private List<ItemStack> buildScrollPool(CrateDefinition definition, LootEntry reward) {
        List<ItemStack> pool = new ArrayList<>();
        for (int cycle = 0; cycle < 10; cycle++) {
            for (LootEntry entry : definition.getLoot()) {
                for (int i = 0; i < Math.min(entry.getWeight(), 5); i++) {
                    pool.add(entry.getItem().clone());
                }
            }
        }
        if (pool.isEmpty()) {
            for (int i = 0; i < 32; i++) {
                pool.add(reward.getItem().clone());
            }
        }
        Collections.shuffle(pool, ThreadLocalRandom.current());
        return pool;
    }

    private double easeOutQuint(double t) {
        return 1 - Math.pow(1 - t, 5);
    }
}
