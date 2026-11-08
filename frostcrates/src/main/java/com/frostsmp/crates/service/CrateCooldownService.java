package com.frostsmp.crates.service;

import com.frostsmp.crates.model.CrateDefinition;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class CrateCooldownService {

    private CrateCooldownService() {
    }

    public static String cooldownKey(UUID playerId, String crateId) {
        return playerId + ":crate:" + crateId.toLowerCase();
    }

    public static long resolveOpenCooldownMs(Player player, CrateDefinition definition) {
        if (player.hasPermission("frostsmp.crates.cooldown.bypass")) {
            return 0L;
        }
        return Math.max(0L, definition.getOpenCooldownMs());
    }
}
