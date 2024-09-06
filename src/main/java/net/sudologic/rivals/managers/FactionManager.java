package net.sudologic.rivals.managers;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.*;
import java.util.logging.Level;

public class FactionManager implements ConfigurationSerializable {
    private Map<Integer, Faction> factions;
    private List<Integer> factionRankings;
    private List<MemberInvite> memberInvites;
    private List<AllyInvite> allyInvites;
    private List<PeaceInvite> peaceInvites;
    private List<WarDeclaration> upcomingWars;

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
        allyInvites = new ArrayList<>();
        List<Object> aObjects = (List<Object>) serializedFactionManager.get("allyInvites");
        for(Object o : aObjects) {
            AllyInvite a = new AllyInvite((Map<String, Object>) o);
            if((System.currentTimeMillis() / 1000L) - 604800 > a.time) {//invite older than 7 days
                removedInvites++;
            } else {
                allyInvites.add(a);
            }
        }
        peaceInvites = new ArrayList<>();
        List<Object> pObjects = (List<Object>) serializedFactionManager.get("allyInvites");
        for(Object o : pObjects) {
            PeaceInvite a = new PeaceInvite((Map<String, Object>) o);
            if((System.currentTimeMillis() / 1000L) - 604800 > a.time) {//invite older than 7 days
                removedInvites++;
            } else {
                peaceInvites.add(a);
            }
        }
        upcomingWars = new ArrayList<>();
        List<Object> wObjects = (List<Object>) serializedFactionManager.get("upcomingWars");
        if(wObjects != null) {
            for(Object o : wObjects) {
                WarDeclaration w = new WarDeclaration((Map<String, Object>) o);
                if(w.delayOver()) {
                    factions.get(w.getFid1()).addEnemy(w.getFid2());
                } else {
                    upcomingWars.add(w);
                }
            }
        }
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

    public void updateFactionRank(Faction f) {
        int id = f.getID();
        double power = f.getPower();

        // Find the index of the faction in the rankings
        int currentIndex = factionRankings.indexOf(id);

        // If faction not found in rankings, do nothing
        if (currentIndex == -1) {
            return;
        }

        // Check if the faction's power change affects its position in the rankings
        int newIndex = currentIndex;
        for (int i = currentIndex - 1; i >= 0; i--) {
            int prevId = factionRankings.get(i);
            double prevPower = factions.get(prevId).getPower();
            if (prevPower < power) {
                newIndex = i;
            } else {
                break;
            }
        }

        // Update the rankings if the faction's position changed
        if (newIndex != currentIndex) {
            factionRankings.remove(currentIndex);
            factionRankings.add(newIndex, id);
        }
    }

    public void buildFactionRanks() {
        // Create a list of faction IDs
        List<Integer> factionIDs = new ArrayList<>(factions.keySet());

        // Sort the faction IDs based on the power of each faction
        Collections.sort(factionIDs, new Comparator<Integer>() {
            @Override
            public int compare(Integer id1, Integer id2) {
                double power1 = factions.get(id1).getPower();
                double power2 = factions.get(id2).getPower();
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

        List<AllyInvite> reA = new ArrayList<>();
        for(AllyInvite a : allyInvites) {
            if(a.invitee == f.getID() || a.inviter == f.getID()) {
                reA.add(a);
            }
        }
        for(AllyInvite a : reA) {
            allyInvites.remove(a);
        }

        List<PeaceInvite> reP = new ArrayList<>();
        for(PeaceInvite p : peaceInvites) {
            if(p.getInviter() == f.getID() || p.getInvitee() == f.getID()) {
                reP.add(p);
            }
        }
        for(PeaceInvite p : reP) {
            peaceInvites.remove(p);
        }
    }
    public FactionManager() {
        factions = new HashMap<>();
        memberInvites = new ArrayList<>();
        allyInvites = new ArrayList<>();
        peaceInvites = new ArrayList<>();
        upcomingWars = new ArrayList<>();
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
            pruneWarsForFaction(f.getID());
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

    public void addAllyInvite(int inviter, int invitee) {
        allyInvites.add(new AllyInvite(inviter, invitee));
    }

    public void removeAllyInvite(int inviter, int invitee) {
        AllyInvite a = null;
        for(AllyInvite i : allyInvites) {
            if(i.getInviter() == inviter && i.getInvitee() == invitee) {
                a = i;
            }
        }
        if(a != null) {
            allyInvites.remove(a);
        }
    }

    public void addPeaceInvite(int inviter, int invitee) {
        peaceInvites.add(new PeaceInvite(inviter, invitee));
    }

    public void removePeaceInvite(int inviter, int invitee) {
        PeaceInvite a = null;
        for(PeaceInvite i : peaceInvites) {
            if(i.getInviter() == inviter && i.getInvitee() == invitee) {
                a = i;
            }
        }
        if(a != null) {
            peaceInvites.remove(a);
        }
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

    public List<Integer> getAllyInvitesForFaction(int id) {
        List list = new ArrayList();
        for(AllyInvite i : allyInvites) {
            if(i.getInvitee() == id) {
                list.add(i.getInvitee());
            }
        }
        return list;
    }

    public List<Faction> getFactions() {
        if (factions.size() == 0) {
            return new ArrayList<>();
        }
        return factions.values().stream().toList();
    }

    public List<Integer> getPeaceInvitesForFaction(int id) {
        List list = new ArrayList();
        for(PeaceInvite i : peaceInvites) {
            if(i.getInvitee() == id) {
                list.add(i.getInvitee());
            }
        }
        return list;
    }

    public void addUpcomingWar(WarDeclaration w) {
        upcomingWars.add(w);
    }

    public boolean createWarDeclaration(int fid1, int fid2, long declareTime, int delay) {
        if(factions.containsKey(fid1) && factions.containsKey(fid2)) {
            WarDeclaration w = new WarDeclaration(fid1, fid2, declareTime, delay);
            addUpcomingWar(w);
            Faction faction1 = getFactionByID(fid1), faction2 = getFactionByID(fid2);
            faction1.sendMessageToOnlineMembers("You will be at war with " + faction2.getColor() + faction2.getName() + ChatColor.RESET + " in " + delay + " hours.");
            faction2.sendMessageToOnlineMembers("You will be at war with " + faction1.getColor() + faction1.getName() + ChatColor.RESET + " in " + delay + " hours.");
        }
        return false;
    }

    public List<Integer> getUpcoming(int fid) {
        ArrayList<Integer> upcoming = new ArrayList<>();
        for(WarDeclaration w : upcomingWars) {
            if(w.getFid1() == fid) {
                upcoming.add(w.getFid2());
            } else if(w.getFid2() == fid) {
                upcoming.add(w.getFid1());
            }
        }
        return upcoming;
    }

    public void startWars() {
        ArrayList<WarDeclaration> declarations = new ArrayList<>();
        for(WarDeclaration w : upcomingWars) {
            if(w.delayOver()) {
                w.startWar();
                declarations.add(w);
            }
        }
        upcomingWars.remove(declarations);
    }

    public void pruneWarsForFaction(int fid) {
        ArrayList<WarDeclaration> cancellations = new ArrayList<>();
        for(WarDeclaration w : upcomingWars) {
            if(w.getFid1() == fid || w.getFid2() == fid) {
                cancellations.add(w);
            }
        }
        upcomingWars.remove(cancellations);
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
        List<Object> aObjects = new ArrayList<>();
        for(AllyInvite a : allyInvites) {
            aObjects.add(a.serialize());
        }
        List<Object> pObjects = new ArrayList<>();
        for(PeaceInvite p : peaceInvites) {
            pObjects.add(p.serialize());
        }
        mapSerializer.put("factions", fObjects);
        mapSerializer.put("memberInvites", iObjects);
        mapSerializer.put("allyInvites", aObjects);
        mapSerializer.put("peaceInvites", pObjects);
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
        List<AllyInvite> a = new ArrayList<>(allyInvites);
        for(AllyInvite i : a) {
            if(i.time > System.currentTimeMillis() + 604800000) {
                allyInvites.remove(i);
            }
        }
        List<PeaceInvite> p = new ArrayList<>(peaceInvites);
        for(PeaceInvite i : p) {
            if(i.time > System.currentTimeMillis() + 604800000) {
                peaceInvites.remove(i);
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

    public class AllyInvite implements ConfigurationSerializable{
        private int inviter;
        private int invitee;
        private long time;

        public AllyInvite(Map<String, Object> serialized) {
            this.inviter = (int) serialized.get("inviter");
            this.invitee = (int) serialized.get("invitee");
            this.time = (long) serialized.get("time");
        }
        public AllyInvite(int inviter, int invitee) {
            this.inviter = inviter;
            this.invitee = invitee;
            this.time = System.currentTimeMillis();
        }

        public int getInvitee() {
            return invitee;
        }

        public int getInviter() {
            return inviter;
        }

        public long getRemainingTime() {
            return (System.currentTimeMillis() + 604800000L) - time;
        }

        @Override
        public Map<String, Object> serialize() {
            HashMap<String, Object> mapSerializer = new HashMap<>();

            mapSerializer.put("inviter", inviter);
            mapSerializer.put("invitee", invitee);
            mapSerializer.put("time", time);

            return mapSerializer;
        }
    }

    public class PeaceInvite implements ConfigurationSerializable{
        private int inviter;
        private int invitee;

        private long time;

        public PeaceInvite(Map<String, Object> serialized) {
            this.inviter = (int) serialized.get("inviter");
            this.invitee = (int) serialized.get("invitee");
            this.time = (long) serialized.get("time");
        }
        public PeaceInvite(int inviter, int invitee) {
            this.inviter = inviter;
            this.invitee = invitee;
            this.time = System.currentTimeMillis();
        }

        public int getInvitee() {
            return invitee;
        }

        public int getInviter() {
            return inviter;
        }

        public long getRemainingTime() {
            return (System.currentTimeMillis() + 604800000) - time;
        }

        @Override
        public Map<String, Object> serialize() {
            HashMap<String, Object> mapSerializer = new HashMap<>();

            mapSerializer.put("inviter", inviter);
            mapSerializer.put("invitee", invitee);
            mapSerializer.put("time", time);

            return mapSerializer;
        }
    }

    public class WarDeclaration implements ConfigurationSerializable{
        private int fid1, fid2, delay;
        private long declareTime;

        public void startWar() {
            getFactionByID(fid1).addEnemy(fid2);
        }

        public WarDeclaration(Map<String, Object> serialized) {
            this.fid1 = (int) serialized.get("fid1");
            this.fid2 = (int) serialized.get("fid2");
            this.declareTime = (long) serialized.get("declareTime");

            this.delay = (int) serialized.get("delay");
        }

        public WarDeclaration(int fid1, int fid2, long declareTime, int delay) {
            this.fid1 = fid1;
            this.fid2 = fid2;
            this.declareTime = declareTime;
            this.delay = delay;//measured in hours
        }

        public boolean delayOver() {
            return System.currentTimeMillis() > declareTime + (delay * 3600000);
        }

        public int getFid1() {
            return fid1;
        }

        public int getFid2() {
            return fid2;
        }

        public long getDeclareTime() {
            return declareTime;
        }

        public long getDelay() {
            return delay;
        }

        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> serialized = new HashMap<>();

            serialized.put("fid1", fid1);
            serialized.put("fid2", fid2);
            serialized.put("declareTime", declareTime);
            serialized.put("delay", delay);

            return serialized;
        }
    }
}
