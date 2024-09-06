package net.sudologic.rivals.managers;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import net.sudologic.rivals.util.Policy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.*;

public class PoliticsManager implements ConfigurationSerializable {
    private int custodian = -1;
    private float custodianBudget = 0;
    private String custodianMandate;
    private HashMap<Integer, Long> sanctionedFactions;//this faction has reduced power
    private HashMap<Integer, Long> denouncedFactions;//this faction is given a warning
    private HashMap<Integer, Long> interventionFactions;//all factions are at war with this faction
    private HashMap<Integer, Long> amnestyFactions;//this faction is given amnesty
    private long custodianEnd = 0;
    private Map<Integer, Policy> proposed;

    public PoliticsManager() {
        sanctionedFactions = new HashMap<>();
        denouncedFactions = new HashMap<>();
        interventionFactions = new HashMap<>();
        amnestyFactions = new HashMap<>();
        proposed = new HashMap<>();
    }
    public PoliticsManager(Map<String, Object> serialized) {
        proposed = new HashMap<>();
        for(Object o : (ArrayList<Object>) serialized.get("proposed")) {
            Policy p = new Policy((Map<String, Object>) o);
            proposed.put(p.getId(), p);
        }
        custodian = (int) serialized.get("custodian");
        custodianBudget = (float) (double)serialized.get("custodianBudget");
        custodianMandate = (String) serialized.get("custodianMandate");
        sanctionedFactions = (HashMap<Integer, Long>) serialized.get("sanctionedFactions");
        denouncedFactions = (HashMap<Integer, Long>) serialized.get("denouncedFactions");
        interventionFactions = (HashMap<Integer, Long>) serialized.get("interventionFactions");
        if(serialized.containsKey("amnestyFactions")) {
            amnestyFactions = (HashMap<Integer, Long>) serialized.get("amnestyFactions");
        } else {
            amnestyFactions = new HashMap<>();
        }
    }
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        ArrayList<Object> pObjects = new ArrayList<>();
        for(Policy p : proposed.values()) {
            pObjects.add(p.serialize());
        }
        serialized.put("proposed", pObjects);
        serialized.put("custodian", custodian);
        serialized.put("custodianBudget", custodianBudget);
        serialized.put("custodianMandate", custodianMandate);
        serialized.put("sanctionedFactions", sanctionedFactions);
        serialized.put("denouncedFactions", denouncedFactions);
        serialized.put("interventionFactions", interventionFactions);
        serialized.put("amnestyFactions", amnestyFactions);

        return serialized;
    }

    public void implement(Policy p) {
        String announce = "";
        switch(p.getType()) {
            case budget -> {
                custodianBudget = p.getBudget();
                announce = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Influence will be taxed at " + (custodianBudget) * 100 + "% to the Custodian" + ChatColor.RESET;
            }
            case custodian -> {
                custodian = p.getTarget();
                custodianEnd = p.getTime() * 3600000 + System.currentTimeMillis();
                announce = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + Rivals.getFactionManager().getFactionByID(custodian).getName() + " is now the Custodian" + ChatColor.RESET;
            }
            case mandate -> {
                custodianMandate = p.getMandate();
                announce = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " The Custodian's mandate has been set to " + custodianMandate + ChatColor.RESET;
            }
            case setting -> {
                Rivals.changeSetting(p.getSettingName(), p.getSettingValue());
                announce = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " The setting " + p.getSettingName() + " has been set to " + p.getSettingValue() + ChatColor.RESET;
            }
            case unsanction -> {
                sanctionedFactions.remove(p.getTarget());
                announce = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + Rivals.getFactionManager().getFactionByID(p.getTarget()).getName() + " has been unsanctioned" + ChatColor.RESET;
            }
            case denounce -> {
                denouncedFactions.put(p.getTarget(), p.getTime() * 3600000 + System.currentTimeMillis());
                announce = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + Rivals.getFactionManager().getFactionByID(p.getTarget()).getName() + " has been denounced" + ChatColor.RESET;
            }
            case sanction -> {
                sanctionedFactions.put(p.getTarget(), p.getTime() * 3600000 + System.currentTimeMillis());
                announce = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + Rivals.getFactionManager().getFactionByID(p.getTarget()).getName() + " has been sanctioned" + ChatColor.RESET;
            }
            case intervention -> {
                interventionFactions.put(p.getTarget(), p.getTime() * 3600000 + System.currentTimeMillis());
                announce = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + Rivals.getFactionManager().getFactionByID(p.getTarget()).getName() + " is now under intervention" + ChatColor.RESET;
            }
            case unintervention -> {
                interventionFactions.remove(p.getTarget());
                announce = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + Rivals.getFactionManager().getFactionByID(p.getTarget()).getName() + " is no longer under intervention" + ChatColor.RESET;
            }
            case amnesty -> {
                amnestyFactions.put(p.getTarget(), p.getTime() * 3600000 + System.currentTimeMillis());
                announce = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + Rivals.getFactionManager().getFactionByID(p.getTarget()).getName() + " has been given amnesty" + ChatColor.RESET;
            }
        }
        for(Player pl : Bukkit.getOnlinePlayers()) {
            pl.sendMessage(announce);
        }
    }

    public void displayPolicy(String[] sel, Player player) {
        String reply = "";
        switch (sel[0]) {
            case "budget" -> reply = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " The Custodian's budget is currently " + (custodianBudget * 100) + "%" + ChatColor.RESET;
            case "custodian" -> {
                if(custodian != -1) {
                    reply = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + Rivals.getFactionManager().getFactionByID(custodian).getName() + " is the current Custodian" + ChatColor.RESET;
                } else {
                    reply = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " There is no current Custodian" + ChatColor.RESET;
                }
            }
            case "mandate" -> {
                if(custodianMandate != "") {
                    reply = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + custodianMandate + " is the current mandate" + ChatColor.RESET;
                } else {
                    reply = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " There is no current mandate" + ChatColor.RESET;
                }
            }
            case "setting" -> {
                if(Rivals.getSettings().contains(sel[1])) {
                    reply = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " The current " + sel[1] + " is: " + Rivals.getSettings().get(sel[1]).toString() + ChatColor.RESET;
                } else {
                    reply = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " No setting with that name" + ChatColor.RESET;
                }
            }
            case "sanctioned" -> {
                reply = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " The current sanctioned factions are: " + ChatColor.RESET;
                for(int f : sanctionedFactions.keySet()) {
                    reply += Rivals.getFactionManager().getFactionByID(f).getName() + " ";
                }
            }
            case "intervention" -> {
                reply = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " The current factions under intervention are: " + ChatColor.RESET;
                for(int f : interventionFactions.keySet()) {
                    reply += Rivals.getFactionManager().getFactionByID(f).getName() + " ";
                }
            }
        }
        if(reply.equals("")) {
            reply = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Invalid request." + ChatColor.RESET;
        }
        player.sendMessage(reply);
    }

    public boolean stopProposal(int id) {
        if(proposed.containsKey(id)) {
            proposed.remove(id);
            return true;
        }
        return false;
    }

    public void update() {
        long time = System.currentTimeMillis();
        if(custodianEnd < time) {
            custodian = -1;
        }
        for(Faction f : Rivals.getFactionManager().getFactions()) {
            f.payInfluence();
        }
        int taxRev = 0;
        if(custodian != -1) {
            for (Faction f : Rivals.getFactionManager().getFactions()) {
                int am = f.taxInfluence(custodianBudget);
                taxRev += am;
                f.sendMessageToOnlineMembers(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You have paid " + am + " influence to the custodian" + ChatColor.RESET);
            }
            Rivals.getFactionManager().getFactionByID(custodian).addInfluence(taxRev);
            Rivals.getFactionManager().getFactionByID(custodian).sendMessageToOnlineMembers(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You have received " + taxRev + " influence from taxes" + ChatColor.RESET);
        }
        for(int f : sanctionedFactions.keySet()) {
            if(sanctionedFactions.get(f) < time) {
                sanctionedFactions.remove(f);
            }
        }
        for(int f : denouncedFactions.keySet()) {
            if(denouncedFactions.get(f) < time) {
                denouncedFactions.remove(f);
            }
        }
        for(int f : interventionFactions.keySet()) {
            if(interventionFactions.get(f) < time) {
                interventionFactions.remove(f);
            }
        }
        for(int f : amnestyFactions.keySet()) {
            if(amnestyFactions.get(f) < time) {
                amnestyFactions.remove(f);
            }
        }
        long a = (System.currentTimeMillis() - ((int)Rivals.getSettings().get("votePassTime") * 3600000l));//time in hours for vote to pass
        Collection<Policy> props = proposed.values();
        List<Policy> toRemove = new ArrayList<>();
        for(Policy p : props) {
            if(p.getProposedTime() < a) {
                if(p.getSupport() > (double)Rivals.getSettings().get("votePassRatio") && p.getNumSupporters() > (int)Rivals.getSettings().get("minVotes")) {
                    implement(p);
                }
                Rivals.getFactionManager().getFactionByID(p.getProposedBy()).sendMessageToOnlineMembers("Your proposal, ID: " + p.getId() + " has been rejected.");
                toRemove.add(p);
            }
        }
        props.removeAll(toRemove);
    }

    public long getVotePassTime() {
        return (int)Rivals.getSettings().get("votePassTime") * 3600000l;
    }

    public Map<Integer, Policy> getProposed() {
        return proposed;
    }

    public Policy propose(Policy policy) {
        if(proposed.size() < 2048) {
            policy.setId((int) (Math.random() * 2048));
            if(proposed.keySet().contains(policy.getId())) {
                for(int i = 0; i < 2048; i++) {
                    if (!proposed.keySet().contains(i)) {
                        policy.setId(i);
                        break;
                    }
                }
            }
            proposed.put(policy.getId(), policy);
            return policy;
        }
        return null;
    }

    public int getCustodian() {
        return custodian;
    }

    public float getCustodianBudget() {
        return custodianBudget;
    }

    public String getCustodianMandate() {
        return custodianMandate;
    }

    public HashMap<Integer, Long> getSanctionedFactions() {
        return sanctionedFactions;
    }

    public HashMap<Integer, Long> getDenouncedFactions() {
        return denouncedFactions;
    }

    public HashMap<Integer, Long> getInterventionFactions() {
        return interventionFactions;
    }

    public HashMap<Integer, Long> getAmnestyFactions() {
        return amnestyFactions;
    }

    public long getCustodianEnd() {
        return custodianEnd;
    }
}
