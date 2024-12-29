package net.sudologic.rivals.managers;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.logging.Level;

public class ShopManager implements ConfigurationSerializable {
    private String mainRegionString;
    private ArrayList<String> shopSubregionStrings;
    private Map<Integer, String> regionAssignments;
    private RegionContainer container;
    private String shopWorldName;

    public String getMainRegionString() {
        return mainRegionString;
    }

    public void setMainRegionString(String mainRegionString) {
        this.mainRegionString = mainRegionString;
    }

    public ShopManager(Map<String, Object> serializedShopManager) {
        //Bukkit.getLogger().log(Level.INFO, "[Rivals] Begin deserializing shop data.");
        container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        this.shopWorldName = (String) serializedShopManager.get("shopWorldName");
        this.mainRegionString = (String) serializedShopManager.get("mainRegionString");
        this.shopSubregionStrings = (ArrayList<String>) serializedShopManager.get("shopSubregionStrings");
        this.regionAssignments = (Map<Integer, String>) serializedShopManager.get("regionAssignments");
        this.shopWorldName = (String) serializedShopManager.get("shopWorldName");
        //Bukkit.getLogger().log(Level.INFO, "[Rivals] Finish deserializing shop data.");
    }

    public boolean setupShop(Faction f) {
        if(regionAssignments == null) {
            regionAssignments = new HashMap<>();
        }
        if(regionAssignments.containsKey(f.getID())) {//faction already has a shop
            return false;
        }
        ArrayList<String> availableRegions = new ArrayList<>(shopSubregionStrings);
        availableRegions.removeAll(regionAssignments.values());
        if(availableRegions.size() == 0) {//no regions available
            return false;
        }
        if(assignFactionToShop(f.getID(), availableRegions.get(0))) {
            Bukkit.getLogger().log(Level.INFO, "[Rivals] Shop assigned to " + f.getName());
            Shopkeeper s = getShopkeeperForFaction(f);
            if (s != null) {
                s.setName(f.getName() + "'s Shop");
            } else {
                //for safety, remove the faction from the shopkeeper list
                removeShop(f);
            }
        }
        return true;
    }

    public boolean removeShop(Faction f) {
        getShopkeeperForFaction(f).setName("Available Shop");
        if(regionAssignments.containsKey(f.getID())) {
            ClaimManager.unsetFacitonAsRegionMember(f, getRegionForFaction(f));
            regionAssignments.remove(f.getID());
            return true;
        }
        return false;
    }

    public Shopkeeper getShopkeeperForFaction(Faction f) {
        String region = getRegionIDForFaction(f.getID());

        if(region != null) {
            //Bukkit.getLogger().log(Level.INFO, "[Rivals] region is not null.");
            return getShopkeeperForRegion(region);
        }
        //Bukkit.getLogger().log(Level.INFO, "[Rivals] region is null.");
        return null;
    }

    public Shopkeeper getShopkeeperForRegion(String regionID) {
        RegionManager manager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(shopWorldName)));
        if(!manager.hasRegion(regionID)) {
            return null;
        }
        //Bukkit.getLogger().log(Level.INFO, "[Rivals] Region " + regionID + " exists.");
        ProtectedRegion region = manager.getRegion(regionID);
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        //Bukkit.getLogger().log(Level.INFO, "[Rivals] Min is " + min.toString());
        //Bukkit.getLogger().log(Level.INFO, "[Rivals] Max is " + max.toString());

        Shopkeeper shopkeeper = null;
        Collection shopkeepers = ShopkeepersAPI.getShopkeeperRegistry().getShopkeepersInWorld(shopWorldName);
        //Bukkit.getLogger().log(Level.INFO, "[Rivals] Shopkeepers in world: " + shopkeepers.size());
        for(Object o : shopkeepers) {
            //Bukkit.getLogger().log(Level.INFO, "[Rivals] Checking shopkeeper at " + ((Shopkeeper) o).getLocation().toString());
            Shopkeeper s = (Shopkeeper) o;
            Location l = s.getLocation();
            if(l.getBlockX() >= min.getBlockX() &&
                    l.getBlockX() <= max.getBlockX() &&
                    l.getBlockY() >= min.getBlockY() &&
                    l.getBlockY() <= max.getBlockY() &&
                    l.getBlockZ() >= min.getBlockZ() &&
                    l.getBlockZ() <= max.getBlockZ()) {
                shopkeeper = s;
                break;
            }
        }

        if (shopkeeper == null) {
            //Bukkit.getLogger().log(Level.INFO, "[Rivals] Shopkeeper not found.");
        }

        return shopkeeper;
    }

    public ProtectedRegion getRegionForShopkeeper(Shopkeeper shopkeeper) {
        Location l = shopkeeper.getLocation();
        Faction f = getFactionForShopLocation(l);
        if(f != null) {
            return getRegionForFaction(f);
        }
        return null;
    }

    public ProtectedRegion getRegionForFaction(Faction f) {
        String id = getRegionIDForFaction(f.getID());
        RegionManager manager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(shopWorldName)));
        return manager.getRegion(id);
    }

    public ShopManager() {
        container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        shopSubregionStrings = new ArrayList<>();
        regionAssignments = new HashMap<>();
        mainRegionString = new String();
        shopWorldName = Bukkit.getWorlds().get(0).getName();
    }

    public Map<String, Object> serialize() {
        HashMap<String, Object> mapSerializer = new HashMap<>();
        mapSerializer.put("mainRegionString", mainRegionString);
        mapSerializer.put("shopSubregionStrings", shopSubregionStrings);
        mapSerializer.put("regionAssignments", regionAssignments);
        mapSerializer.put("shopWorldName", shopWorldName);
        return mapSerializer;
    }

    public boolean assignFactionToShop(int id, String regionString) {
        if(shopSubregionStrings.contains(regionString)) {
            if(!regionAssignments.values().contains(regionString)) {
                regionAssignments.put(id, regionString);
                RegionManager manager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(shopWorldName)));
                ClaimManager.setFactionAsRegionMember(Rivals.getFactionManager().getFactionByID(id), manager.getRegion(regionString));
                return true;
            }
        }
        return false;
    }

    public String getRegionIDForFaction(int id) {
        if(regionAssignments == null) {
            regionAssignments = new HashMap<>();
        }
        if(regionAssignments.containsKey(id)) {
            return regionAssignments.get(id);
        }
        return null;
    }

    public int addSubregions() {
        RegionManager manager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(shopWorldName)));
        if(manager.hasRegion(mainRegionString)) {
            ApplicableRegionSet set = manager.getApplicableRegions(manager.getRegion(mainRegionString));
            shopSubregionStrings = new ArrayList<>();
            for(ProtectedRegion r : set.getRegions()) {
                if(!shopSubregionStrings.contains(r.getId())) {
                    shopSubregionStrings.add(r.getId());
                }
            }
            if(shopSubregionStrings.contains(mainRegionString)) {
                shopSubregionStrings.remove(mainRegionString);
            }
            return shopSubregionStrings.size();
        }
        return 0;
    }

    public Faction getFactionForShopRegion(String id) {
        if(regionAssignments == null) {
            regionAssignments = new HashMap<>();
        }
        if(regionAssignments.containsValue(id)) {
            for(Integer i : regionAssignments.keySet()) {
                if(regionAssignments.get(i).equals(id)) {
                    return Rivals.getFactionManager().getFactionByID(i);
                }
            }
        }
        return null;
    }

    public Faction getFactionForShopLocation(Location l) {
        ApplicableRegionSet set = getRegionsForLocation(l);
        for(ProtectedRegion r : set.getRegions()) {
            if(shopSubregionStrings.contains(r.getId())) {
                return getFactionForShopRegion(r.getId());
            }
        }
        return null;
    }

    public ApplicableRegionSet getRegionsForLocation(Location l) {
        RegionManager rManager = container.get(BukkitAdapter.adapt(l.getWorld()));
        return rManager.getApplicableRegions(BlockVector3.at(l.getX(), l.getY(), l.getZ()));
    }

}
