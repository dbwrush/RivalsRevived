package net.sudologic.rivals;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

public class Faction implements ConfigurationSerializable {
    private int factionID;
    private String factionName;
    private List<UUID> members;
    private Map<String, Location> homes;
    private ChatColor color;
    private List<String> regions;

    @Override
    public Map<String, Object> serialize() {
        HashMap<String, Object> mapSerializer = new HashMap<>();

        mapSerializer.put("factionID", factionID);
        mapSerializer.put("factionName", factionName.toString());
        List<String> memberStrings = new ArrayList<>();
        for(UUID uuid : members) {
            memberStrings.add(uuid.toString());
        }
        mapSerializer.put("members", memberStrings);
        mapSerializer.put("regions", regions);
        mapSerializer.put("color", color.getChar());
        mapSerializer.put("homes", homes);

        return mapSerializer;
    }

    public Faction(Map<String, Object> serializedFaction) {
        this.factionID = (int) serializedFaction.get("factionID");
        this.factionName = (String) serializedFaction.get("factionName");
        List<String> memberStrings = (List<String>) serializedFaction.get("members");
        this.members = new ArrayList<>();
        for(String s : memberStrings) {
            members.add(UUID.fromString(s));
        }
        this.regions = (List<String>) serializedFaction.get("regions");
        this.color = ChatColor.getByChar((String) serializedFaction.get("color"));
        this.homes = (Map<String, Location>) serializedFaction.get("homes");
    }

    public Faction(UUID firstPlayer, String name, int id) {
        factionID = id;
        factionName = name;
        members = new ArrayList<>();
        members.add(firstPlayer);
        regions = new ArrayList<>();
        color = ChatColor.values()[(int) (Math.random() * ChatColor.values().length)];
        if(color.equals(ChatColor.MAGIC) || color.equals(ChatColor.BLACK)) {
            color = ChatColor.RESET;
        }
        homes = new HashMap<>();
    }

    public Map<String, Location> getHomes() {
        return homes;
    }

    public Location getHome(String s) {
        return homes.get(s);
    }

    public boolean setHome(String s, Location location) {
        if(homes.containsKey(s)) {
            return false;
        }
        if(homes.size() < getMaxHomes()) {
            homes.put(s, location);
            return true;
        }
        return false;
    }

    public boolean delHome(String s) {
        if(homes.containsKey(s)) {
            homes.remove(s);
            return true;
        }
        return false;
    }

    public int getMaxHomes() {
        return 1 + members.size() / 5;
    }

    public UUID getLeader() {
        return members.get(0);
    }

    public boolean setLeader(UUID uuid) {
        if(members.contains(uuid)) {
            members.remove(uuid);
            members.add(0, uuid);
            return true;
        }
        return false;
    }

    public void setName(String name) {
        factionName = name;
    }

    public void setColor(ChatColor color) {
        if(color.equals(ChatColor.MAGIC) || color.equals(ChatColor.BLACK)) {
            color = ChatColor.RESET;
        }
        this.color = color;
    }

    public ChatColor getColor() {
        return color;
    }


    public boolean addMember(UUID member) {
        if(!members.contains(member)) {
            sendMessageToOnlineMembers(Bukkit.getPlayer(member).getName() + ChatColor.LIGHT_PURPLE + " has joined your faction.");
            members.add(member);
            Rivals.getClaimManager().updateFactionMembers(this);
            return true;
        }
        return false;
    }

    public int countOnlineMembers() {
        int count = 0;
        for(UUID id : members) {
            if(Bukkit.getOfflinePlayer(id).isOnline()) {
                count++;
            }
        }
        return count;
    }

    public boolean removeMember(UUID member) {
        if(members.contains(member)) {
            members.remove(member);
            Rivals.getClaimManager().updateFactionMembers(this);
            if(members.size() == 0) {
                for(Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + ChatColor.RESET + factionName + ChatColor.LIGHT_PURPLE + " has disbanded because all its players have left.");
                }
                Rivals.getFactionManager().removeFaction(this);
                return true;
            }
            sendMessageToOnlineMembers(Bukkit.getPlayer(member).getName() + " has left your faction.");
            return true;
        }
        return false;
    }

    public List<UUID> getMembers() {
        return members;
    }
    public int getID() {
        return factionID;
    }

    public String getName() {
        return factionName;
    }

    public boolean addClaim(Chunk c) {
        if(Rivals.getClaimManager().createClaim(c, this)) {
            regions.add(getClaimName(c));
            return true;
        }
        return false;
    }

    public boolean removeClaim(Chunk c) {
        if(Rivals.getClaimManager().removeClaim(c, this)) {
            regions.remove(getClaimName(c));
            return true;
        }
        return false;
    }

    public List<String> getRegions() {
        return regions;
    }

    public String getClaimName(Chunk c) {
        return "rfclaims_" + c.getWorld().getName() + "_" + factionID + "_" + c.getX() + "_" + c.getZ();
        /*
        0 - rfclaims
        1 - world
        2 - factionID
        3 - x
        4 - z
        5+- trusted factions
         */
    }

    public void sendMessageToOnlineMembers(String s) {
        for(UUID id : members) {
            if(Bukkit.getOfflinePlayer(id).isOnline()) {
                Bukkit.getPlayer(id).sendMessage("[Rivals] " + s);
            }
        }
    }
}
