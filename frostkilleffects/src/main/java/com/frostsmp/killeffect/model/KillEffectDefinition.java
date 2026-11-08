package com.frostsmp.killeffect.model;

import com.frostsmp.killeffect.item.KillEffectItem;
import org.bukkit.inventory.ItemStack;

public record KillEffectDefinition(
        String id,
        String name,
        String permission,
        ItemStack claimItem
) {
    public boolean matches(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        String tagged = KillEffectItem.effectId(item);
        if (tagged != null) {
            return id.equalsIgnoreCase(tagged);
        }
        // Untagged legacy vouchers: require same type + display name (never bare material).
        if (claimItem == null || item.getType() != claimItem.getType()) {
            return false;
        }
        if (!item.hasItemMeta() || !claimItem.hasItemMeta()) {
            return false;
        }
        var itemMeta = item.getItemMeta();
        var claimMeta = claimItem.getItemMeta();
        if (itemMeta == null || claimMeta == null || !itemMeta.hasDisplayName() || !claimMeta.hasDisplayName()) {
            return false;
        }
        return itemMeta.displayName().equals(claimMeta.displayName());
    }

    public ItemStack createClaimStack(int amount) {
        ItemStack stack = KillEffectItem.stamp(claimItem.clone(), id);
        stack.setAmount(Math.max(1, Math.min(64, amount)));
        return stack;
    }
}
