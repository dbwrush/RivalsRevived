package net.sudologic.rivals.resources;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ResourceSpawner implements ConfigurationSerializable {
    private Location location;
    private double chance;
    private int maxAmount = 64;
    private ItemStack item;

    public ResourceSpawner(Location location) {
        this.location = location;
        Biome b = location.getBlock().getBiome();
        Material m = null;
        switch (b) {
            case FOREST, TAIGA, SWAMP:
                m = Material.COAL;
                maxAmount = 160;
                break;
            case SNOWY_SLOPES, GROVE, CHERRY_GROVE, MEADOW, FROZEN_PEAKS, JAGGED_PEAKS, STONY_PEAKS:
                m = Material.RAW_IRON;
                maxAmount = 60;
                break;
            case BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS:
                m = Material.RAW_GOLD;
                maxAmount = 40;
                break;
            case DEEP_OCEAN, DEEP_COLD_OCEAN, DEEP_LUKEWARM_OCEAN:
                m = Material.DIAMOND;
                maxAmount = 20;
                break;
            case SAVANNA, SAVANNA_PLATEAU, WINDSWEPT_SAVANNA:
                m = Material.REDSTONE;
                maxAmount = 40;
                break;
            case JUNGLE, BAMBOO_JUNGLE, SPARSE_JUNGLE:
                m = Material.JUNGLE_LOG;
                maxAmount = 60;
                break;
            case BIRCH_FOREST, OLD_GROWTH_BIRCH_FOREST:
                m = Material.BIRCH_LOG;
                maxAmount = 60;
                break;
            case DARK_FOREST:
                m = Material.DARK_OAK_LOG;
                maxAmount = 60;
                break;
            case OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA, SNOWY_TAIGA:
                m = Material.SPRUCE_LOG;
                maxAmount = 60;
                break;
            case DESERT:
                m = Material.SAND;
                maxAmount = 60;
                break;
            case ICE_SPIKES, FROZEN_OCEAN, FROZEN_RIVER, DEEP_FROZEN_OCEAN:
                m = Material.ICE;
                maxAmount = 60;
                break;
            case BASALT_DELTAS:
                m = Material.MAGMA_BLOCK;
                maxAmount = 60;
                break;
            case NETHER_WASTES:
                m = Material.QUARTZ;
                maxAmount = 60;
                break;
            case SOUL_SAND_VALLEY:
                m = Material.SOUL_SAND;
                maxAmount = 60;
                break;
        }
        if(m == null) {
            switch (location.getWorld().getEnvironment()) {
                case NORMAL -> m = Material.COAL;
                case NETHER -> m = Material.RAW_GOLD;
                case THE_END -> m = Material.END_STONE;
            }
        }
        item = new ItemStack(m);
        chance = Math.random() * 0.1 + 0.9;

        for(Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("[Rivals] New " + m.name() + " spawner at " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        }

    }

    public void spawnResource() {
        if(location.isWorldLoaded() && Math.random() < chance) {
            item.setAmount((int) Math.max(64 * chance, 1));
            location.getWorld().dropItem(location, item);
            chance *= 0.99 + Math.random() * 0.01;
        }
    }

    public Location getLocation() {
        return location;
    }

    public String getMaterial() {
        if(item.getItemMeta() == null)
            return "";
        return item.getItemMeta().getDisplayName();
    }

    public double getChance() {
        return chance;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("amount", maxAmount);
        serialized.put("location", location);
        serialized.put("chance", chance);
        serialized.put("item", item);

        return serialized;
    }

    public ResourceSpawner(Map<String, Object> serialized) {
        this.location = (Location) serialized.get("location");
        this.chance = (double) serialized.get("chance");
        this.item = (ItemStack) serialized.get("item");
        if(serialized.containsKey("amount")) {
            this.maxAmount = (int) serialized.get("amount");
        } else {
            this.maxAmount = 64;
        }
    }
}
