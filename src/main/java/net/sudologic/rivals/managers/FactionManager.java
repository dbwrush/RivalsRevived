package net.sudologic.rivals.managers;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

public class FactionManager implements ConfigurationSerializable {
    private Map<Integer, Faction> factions;
    private List<Integer> factionRankings;
    private List<MemberInvite> memberInvites;
    private int crisisFaction = -1;
    private int teleportOnJoin = -1;//-1 = normal, 1 = crisis, 2 = final battle
    private long crisisStart = -1;

    public boolean isCrisis() {
        return crisisFaction != -1;
    }

    public FactionManager(Map<String, Object> serializedFactionManager) {
        //Bukkit.getLogger().log(Level.INFO, "[Rivals] Begin deserializing faction data.");
        factions = new HashMap<>();
        memberInvites = new ArrayList<>();
        List<Object> fObjects = (List<Object>) serializedFactionManager.get("factions");
        for(Object o : fObjects) {
            Faction f = new Faction((Map<String, Object>) o);
            factions.put(f.getID(), f);
        }
        List<Object> iObjects = (List<Object>) serializedFactionManager.get("memberInvites");
        int removedInvites = 0;
        for(Object o : iObjects) {
            MemberInvite i = new MemberInvite((Map<String, Object>) o);
            if((System.currentTimeMillis() / 1000L) - 604800 > i.time) {//invite older than 7 days
                removedInvites++;
            } else {
                memberInvites.add(i);
            }
        }
        crisisFaction = (Integer) serializedFactionManager.getOrDefault("crisisFaction", null);
        //Bukkit.getLogger().log(Level.INFO, "[Rivals] Removed " + removedInvites + " invites because they were more than 7 days old.");
        //Bukkit.getLogger().log(Level.INFO, "[Rivals] Finished deserializing faction data.");
    }

    public int getUnusedFactionID() {
        if(factions.size() > 0) {
            int m = (int) factions.keySet().toArray()[factions.keySet().size() - 1];
            for(int i = 0; i < m; i++) {
                if(getFactionByID(i) == null) {
                    return i;
                }
            }
            return m + 1;
        }
        return 0;
    }

    public List<Integer> getFactionRankings() {
        return factionRankings;
    }

    public void buildFactionRanks() {
        // Create a list of faction IDs
        List<Integer> factionIDs = new ArrayList<>(factions.keySet());

        // Sort the faction IDs based on the power of each faction
        Collections.sort(factionIDs, new Comparator<Integer>() {
            @Override
            public int compare(Integer id1, Integer id2) {
                double power1 = factions.get(id1).getMembers().size();
                double power2 = factions.get(id2).getMembers().size();
                // Reverse order to sort in descending order
                return Double.compare(power2, power1);
            }
        });
        // Return the sorted faction IDs
        factionRankings = factionIDs;
    }

    public void removeInvitesForFaction(Faction f) {
        List<MemberInvite> reM = new ArrayList<>();
        for(MemberInvite m : memberInvites) {
            if(m.getFaction() == f.getID()) {
                reM.add(m);
            }
        }
        for(MemberInvite m : reM) {
            memberInvites.remove(m);
        }
    }
    public FactionManager() {
        factions = new HashMap<>();
        memberInvites = new ArrayList<>();
    }

    public boolean addFaction(Faction f) {
        if(!factions.containsKey(f.getID()) && !nameAlreadyExists(f.getName())) {
            factions.put(f.getID(), f);
            factionRankings.add(factionRankings.size(), f.getID());
            return true;
        }
        return false;
    }

    public boolean removeFaction(Faction f) {
        if(factions.containsKey(f.getID())) {
            Rivals.getClaimManager().removeRegionsForFaction(f);
            factions.remove(f.getID());
            removeInvitesForFaction(f);
            factionRankings.remove((Object) f.getID());
            Rivals.getControlPointManager().clearFaction(f.getID());
            Rivals.getEffectManager().removeFaction(f);
            return true;
        }
        return false;
    }

    public Faction getFactionByID(int id) {
        return factions.get(id);
    }

    public Faction getFactionByName(String name) {
        for(Faction f : factions.values()) {
            if(f.getName().equalsIgnoreCase(name)) {
                return f;
            }
        }
        return null;
    }

    public Faction getFactionByNameImprecise(String name) {
        for(Faction f : factions.values()) {
            if(f.getName().equals(name)) {
                return f;
            }
        }
        for(int i = Math.min(name.length() - 1, 16); i > 0; i--) {
            String sub = name.substring(0, i);
            for(Faction f : factions.values()) {
                if(f.getName().equals(sub)) {
                    return f;
                }
            }
        }
        for(int i = Math.min(name.length() - 1, 16); i > 0; i--) {
            String sub = name.substring(0, i);
            for(Faction f : factions.values()) {
                if(f.getName().contains(sub)) {
                    return f;
                }
            }
        }
        return null;
    }

    public Faction getFactionByPlayer(UUID id) {
        for(Faction f : factions.values()) {
            if(f.getMembers().contains(id)) {
                return f;
            }
        }
        return null;
    }

    public void addMemberInvite(UUID id, int f) {
        memberInvites.add(new MemberInvite(f, id));
    }

    public void removeMemberInvite(UUID id, int f) {
        MemberInvite s = null;
        for(MemberInvite i : memberInvites) {
            if(i.getFaction() == f && i.getPlayer() == id) {
                s = i;
            }
        }
        if(s != null) {
            memberInvites.remove(s);
        }
    }

    public List<Integer> getInvitesForPlayer(UUID pId) {
        List list = new ArrayList();
        for(MemberInvite i : memberInvites) {
            if(i.getPlayer() == pId)
                list.add(i.getFaction());
        }
        return list;
    }

    public List<Faction> getFactions() {
        if (factions.size() == 0) {
            return new ArrayList<>();
        }
        return factions.values().stream().toList();
    }

    @Override
    public Map<String, Object> serialize() {
        HashMap<String, Object> mapSerializer = new HashMap<>();

        List<Object> fObjects = new ArrayList<>();
        for(Faction f : factions.values()) {
            fObjects.add(f.serialize());
        }
        List<Object> iObjects = new ArrayList<>();
        for(MemberInvite i : memberInvites) {
            iObjects.add(i.serialize());
        }
        mapSerializer.put("factions", fObjects);
        mapSerializer.put("memberInvites", iObjects);
        mapSerializer.put("crisisFaction", crisisFaction);
        return mapSerializer;
    }

    public boolean nameAlreadyExists(String name){
        for(Faction f : factions.values()) {
            if(f.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public void removeInvitesOver7Days() {
        List<MemberInvite> m = new ArrayList<>(memberInvites);
        for(MemberInvite i : m) {
            if(i.time > System.currentTimeMillis() + 604800000) {
                memberInvites.remove(i);
            }
        }
    }

    public class MemberInvite implements ConfigurationSerializable{
        private int faction;
        private UUID player;

        private long time;
        public MemberInvite(Map<String, Object> serialized) {
            this.faction = (int) serialized.get("faction");
            this.player = UUID.fromString((String) serialized.get("player"));
            this.time = (long) serialized.get("time");
        }
        public MemberInvite(int faction, UUID id) {
            this.faction = faction;
            this.player = id;
            this.time = System.currentTimeMillis();
        }

        public int getFaction() {
            return faction;
        }

        public UUID getPlayer() {
            return player;
        }

        public long getRemainingTime() {
            return (System.currentTimeMillis() + 604800000) - time;
        }

        @Override
        public Map<String, Object> serialize() {
            HashMap<String, Object> mapSerializer = new HashMap<>();

            mapSerializer.put("player", player.toString());
            mapSerializer.put("faction", faction);
            mapSerializer.put("time", time);

            return mapSerializer;
        }
    }

    public void beginCrisis(int f) {
        if(crisisFaction == -1) {
            crisisFaction = f;
            teleportOnJoin = 1;
            crisisStart = System.currentTimeMillis();
        }
        //iterate through all players, offline and online. If they are in crisisFaction, teleport them to the End. Otherwise, make sure they are not in the End.
        for(Player p : Bukkit.getOnlinePlayers()) {
            if(getFactionByPlayer(p.getUniqueId()) != null && getFactionByPlayer(p.getUniqueId()).getID() == f) {
                if(!p.getWorld().getName().contains("end")) {
                    p.sendMessage(ChatColor.RED + " You have been teleported to the End.");
                    p.teleport(Bukkit.getWorld("world_the_end").getSpawnLocation());
                }
            } else {
                if(p.getWorld().getName().contains("end")) {
                    p.teleport(p.getRespawnLocation());
                }
            }
        }
    }

    public void beginFinalBattle() {
        teleportOnJoin = 2;
        //set world border to shrink to 1 chunk in 1 hour
        Bukkit.getWorld("world").getWorldBorder().setSize(16, 3600000);
        //teleport crisis faction members to a random Control Point
        Faction f = getFactionByID(crisisFaction);
        Location l = Rivals.getControlPointManager().getRandomPoint();
        if(l != null) {
            for(UUID id : f.getMembers()) {
                Player p = Bukkit.getPlayer(id);
                if(p != null) {
                    p.teleport(l);
                }
            }
        }
    }

    public int getCrisisFaction() {
        return crisisFaction;
    }

    public int getTeleportOnJoin() {
        return teleportOnJoin;
    }

    public void setCrisisFaction(int f) {
        crisisFaction = f;
    }

    public long getCrisisStart() {
        return crisisStart;
    }

    public boolean canBeginFinalBattle() {
        if(crisisStart == -1) {
            return false;
        }
        return System.currentTimeMillis() - crisisStart > 3600000 * 48;//48 hours
    }
}
