package net.sudologic.rivals.managers;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import net.sudologic.rivals.util.CustomCrafts;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class ControlPointManager implements ConfigurationSerializable {
    private static final int CONTROL_POINT_CAPTURE_TIME = 10;//time in hours to capture a control point
    private static final int HOURS_FOR_EYE = 24 * 7; //ticks in 1 week
    private int updatesSinceEye = 0;

    private HashMap<Location, Integer> controlPoints = new HashMap<>();//map of control points to faction ID of owner
    private HashMap<Integer, Double> controlPointProgress = new HashMap<>();//map of control points to progress towards capture
    private HashMap<Integer, Integer> controlPointHeldTime = new HashMap<>();//map of control points to time held by faction

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("controlPoints", controlPoints);
        map.put("controlPointProgress", controlPointProgress);
        map.put("controlPointHeldTime", controlPointHeldTime);
        map.put("updatesSinceEye", updatesSinceEye);
        return map;
    }

    public ControlPointManager(Map<String, Object> serialized) {
        controlPoints = (HashMap<Location, Integer>) serialized.get("controlPoints");
        controlPointProgress = (HashMap<Integer, Double>) serialized.get("controlPointProgress");
        controlPointHeldTime = (HashMap<Integer, Integer>) serialized.get("controlPointHeldTime");
        updatesSinceEye = (int) serialized.get("updatesSinceEye");
    }

    public ControlPointManager() {
    }

    public void setControlPointOwner(Location location, int factionID) {
        location = location.getBlock().getLocation();
        controlPoints.put(location, factionID);
        updateHologram(location);
    }

    public boolean isControlPoint(Location location) {
        location = location.getBlock().getLocation();
        return controlPoints.containsKey(location);
    }

    public int getControlPointOwner(Location location) {
        location = location.getBlock().getLocation();
        return controlPoints.getOrDefault(location, -1);
    }

    public void clearFaction(int factionId) {
        for (Location l : controlPoints.keySet()) {
            if (controlPoints.get(l) == factionId) {
                controlPoints.remove(l);
                controlPointProgress.remove(l.hashCode());
            }
        }
        controlPointHeldTime.remove(factionId);
    }

    public ArrayList<Location> getControlPointsForFaction(int factionID) {
        ArrayList<Location> locations = new ArrayList<>();
        for (Map.Entry<Location, Integer> entry : controlPoints.entrySet()) {
            if (entry.getValue() == factionID) {
                locations.add(entry.getKey());
            }
        }
        return locations;
    }

    public void update() {
        for (Location l : controlPoints.keySet()) {
            int factionID = controlPoints.get(l);
            if (controlPointProgress.containsKey(l.hashCode())) {
                controlPointProgress.put(l.hashCode(), controlPointProgress.get(factionID) + (1 / CONTROL_POINT_CAPTURE_TIME));
                if (controlPointProgress.get(l.hashCode()) >= 1) {
                    controlPointProgress.remove(l.hashCode());
                }
            } else {//faction has held control point long enough to get credit
                if (controlPointHeldTime.containsKey(factionID)) {
                    controlPointHeldTime.put(factionID, controlPointHeldTime.get(factionID) + 1);
                } else {
                    controlPointHeldTime.put(factionID, 1);
                }
            }
        }
        if(updatesSinceEye > HOURS_FOR_EYE) {
            updatesSinceEye = 0;
            //determine which faction has held the most control points for the longest time
            int bestFaction = -1;
            int bestTime = 0;
            for (int factionID : controlPointHeldTime.keySet()) {
                if (controlPointHeldTime.get(factionID) > bestTime) {
                    bestTime = controlPointHeldTime.get(factionID);
                    bestFaction = factionID;
                }
            }
            if (bestFaction != -1) {
                //give the faction an eye
                //find someone in the faction who has an open inventory slot, ideally in the hotbar
                Faction f = Rivals.getFactionManager().getFactionByID(bestFaction);
                if (f != null) {
                    for (UUID memberID : f.getMembers()) {
                        //get Player object's inventory
                        //look for an empty slot
                        PlayerInventory i = Bukkit.getPlayer(memberID).getInventory();
                        if (i.firstEmpty() != -1) {
                            i.addItem(CustomCrafts.getCustomItem("Guardian Eye"));
                            break;
                        }
                    }
                }
            }
        }
    }

    public void updateHologram(Location location) {
        location = location.getBlock().getLocation();
        location = location.clone().add(0, 2, 0);
        Hologram h = DHAPI.getHologram(String.valueOf(location.hashCode()));
        if (h == null) {
            h = DHAPI.createHologram(String.valueOf(location.hashCode()), location, true);
            h.setAlwaysFacePlayer(true);
        }
        int factionID = controlPoints.get(location);
        Faction f = Rivals.getFactionManager().getFactionByID(factionID);
        List<String> lines = new ArrayList<>();
        if(f != null) {
            lines.add(f.getColor() + f.getName());
        } else {
            lines.add("Unclaimed");
        }
        if (controlPointProgress.containsKey(location.hashCode())) {
            int progress = (int) (controlPointProgress.get(location.hashCode()) * 10);
            int rem = 10 - progress;
            //Display progress bar
            lines.add(f.getColor() + "█".repeat(progress) + ChatColor.WHITE + "█".repeat(rem));
            lines.add("Capturing in " + rem + " hours");
        }
    }

    public Location getRandomPoint() {
        Random r = new Random();
        List<Location> keys = new ArrayList<>(controlPoints.keySet());
        return keys.get(r.nextInt(keys.size()));
    }

    public void removeControlPoint(Location l) {
        l = l.getBlock().getLocation();
        controlPoints.remove(l);
        controlPointProgress.remove(l.hashCode());
        controlPointHeldTime.remove(l.hashCode());
        DHAPI.removeHologram(String.valueOf(l.hashCode()));
    }
}
