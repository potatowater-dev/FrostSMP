package com.frostsmp.combat.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;

public final class MaceRecipeRemover {

    private MaceRecipeRemover() {
    }

    public static void remove(JavaPlugin plugin) {
        int removed = 0;
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe.getResult().getType() == Material.MACE) {
                iterator.remove();
                removed++;
            }
        }
        if (Bukkit.removeRecipe(NamespacedKey.minecraft("mace"))) {
            removed++;
        }
        if (removed > 0) {
            plugin.getLogger().info("Removed " + removed + " mace crafting recipe(s).");
        }
    }
}
