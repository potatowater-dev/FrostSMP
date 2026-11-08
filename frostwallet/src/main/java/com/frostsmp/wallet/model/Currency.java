package com.frostsmp.wallet.model;

import java.util.List;

public record Currency(String id, String name, List<Denomination> denominations) {

    public Denomination match(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        for (Denomination denomination : denominations) {
            if (denomination.template().isSimilar(item)) {
                return denomination;
            }
        }
        return null;
    }
}
