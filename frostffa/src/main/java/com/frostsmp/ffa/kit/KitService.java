package com.frostsmp.ffa.kit;

import com.frostsmp.ffa.arena.ArenaService;
import com.frostsmp.ffa.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public final class KitService {

    private final PluginConfig config;
    private final ArenaService arena;

    public KitService(PluginConfig config, ArenaService arena) {
        this.config = config;
        this.arena = arena;
    }

    public String give(Player player) {
        if (config.clearInventory()) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setItemInOffHand(null);
        }
        boolean booster = player.hasPermission("frostsmp.ffa.kit.booster");
        boolean rank = player.hasPermission("frostsmp.ffa.kit.rank");
        applyBase(player);
        if (booster) {
            player.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING));
        }
        if (rank) {
            ItemStack sword = player.getInventory().getItem(0);
            if (sword != null && sword.getType() == Material.DIAMOND_SWORD) {
                sword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1);
            }
        }
        player.updateInventory();
        if (booster && rank) {
            return "rank";
        }
        if (booster) {
            return "booster";
        }
        if (rank) {
            return "rank";
        }
        return "base";
    }

    public boolean shouldAutoGive(Player player) {
        return arena.contains(player) || (player.getWorld() != null
                && player.getWorld().getName().equals(arena.worldName()));
    }

    private void applyBase(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setHelmet(armor(Material.DIAMOND_HELMET));
        inv.setChestplate(armor(Material.DIAMOND_CHESTPLATE));
        inv.setLeggings(armor(Material.DIAMOND_LEGGINGS));
        inv.setBoots(armor(Material.DIAMOND_BOOTS));

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        sword.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        inv.setItem(0, sword);

        ItemStack bow = new ItemStack(Material.BOW);
        bow.addUnsafeEnchantment(Enchantment.POWER, 5);
        bow.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        bow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
        inv.setItem(1, bow);

        inv.setItem(2, new ItemStack(Material.ENDER_PEARL, 16));
        inv.setItem(3, new ItemStack(Material.GOLDEN_APPLE, 8));
        inv.setItem(4, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
        inv.setItem(5, new ItemStack(Material.COOKED_BEEF, 64));
        inv.setItem(6, splash(PotionType.STRONG_HEALING, 2));
        inv.setItem(7, splash(PotionType.STRONG_SWIFTNESS, 1));
        inv.setItem(8, new ItemStack(Material.TOTEM_OF_UNDYING));
        inv.setItemInOffHand(new ItemStack(Material.SHIELD));
        inv.addItem(new ItemStack(Material.ARROW, 64));
        inv.addItem(new ItemStack(Material.COBBLESTONE, 64));
        inv.addItem(new ItemStack(Material.WATER_BUCKET));
        inv.addItem(new ItemStack(Material.DIAMOND_PICKAXE));
    }

    private static ItemStack armor(Material material) {
        ItemStack item = new ItemStack(material);
        item.addUnsafeEnchantment(Enchantment.PROTECTION, 4);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        return item;
    }

    private static ItemStack splash(PotionType type, int amount) {
        ItemStack item = new ItemStack(Material.SPLASH_POTION, amount);
        if (item.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(type);
            item.setItemMeta(meta);
        }
        return item;
    }
}
