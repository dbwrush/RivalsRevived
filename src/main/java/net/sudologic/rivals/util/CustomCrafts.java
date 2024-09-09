package net.sudologic.rivals.util;

import net.sudologic.rivals.Rivals;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CustomCrafts {
    private static final Map<String, ItemStack> customItems = new HashMap<>();

    public static void registerCrafts() {
        addUncraftableItem("Guardian Eye", "guardian_eye", Material.ENDER_EYE, 1);
        addCraftableItem("Heavy Core Eye", "heavy_core_eye", Material.ENDER_EYE, 2,
                new String[] {"ABA", "BCB", "ABA"},
                new Material[] {Material.IRON_BLOCK, Material.HEAVY_CORE, Material.BEACON});
        addUncraftableItem("Totem Eye", "totem_eye", Material.ENDER_EYE, 3);
        addUncraftableItem("Wither Eye", "wither_eye", Material.ENDER_EYE, 4);
        addUncraftableItem("Warden Eye", "warden_eye", Material.ENDER_EYE, 5);
        addUncraftableItem("Evoker Eye", "evoker_eye", Material.ENDER_EYE, 6);
        addCraftableItem("Blaze Eye", "blaze_eye", Material.ENDER_EYE, 7,
                new String[] {"ABA", "BCB", "ABA"},
                new Material[] {Material.BLAZE_POWDER, Material.GOLDEN_APPLE, Material.NETHERITE_BLOCK});
    }

    public static ItemStack addUncraftableItem(String displayName, String name, Material material, int customModelData) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Bukkit.getLogger().log(Level.INFO, "ItemMeta is null for " + name + ", skipping.");
            return item;
        }
        meta.setDisplayName(displayName);
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        customItems.put(name, item);
        return item;
    }

    public static void addCraftableItem(String displayName, String name, Material material, int customModelData, String[] recipe, Material[] ingredients) {
        NamespacedKey key = new NamespacedKey(Rivals.getPlugin(), name);
        if (Bukkit.getRecipe(key) != null) {
            Bukkit.removeRecipe(key);
        }
        ItemStack item = addUncraftableItem(displayName, name, material, customModelData);
        ShapedRecipe shapedRecipe = new ShapedRecipe(key, item);
        ArrayList<Character> letters = new ArrayList<>();
        for (String row : recipe) {
            for (char c : row.toCharArray()) {
                if (c != ' ' && !letters.contains(c)) {
                    letters.add(c);
                }
            }
        }
        shapedRecipe.shape(recipe);
        for (int i = 0; i < ingredients.length; i++) {
            shapedRecipe.setIngredient(letters.get(i), ingredients[i]);
        }
        Bukkit.addRecipe(shapedRecipe);
    }

    public static ItemStack getCustomItem(String name) {
        return customItems.getOrDefault(name, null) != null ? customItems.get(name).clone() : null;
    }

    public static void showTypes(Player p) {
        for (String key : customItems.keySet()) {
            p.sendMessage(key);
        }
    }
}
