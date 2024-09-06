package net.sudologic.rivals.util;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import net.sudologic.rivals.managers.FactionManager;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Policy implements ConfigurationSerializable {
    private int id;
    private int target;
    private int proposedBy;
    private long proposedTime;
    private int time;
    private String settingName;
    private String mandate;
    private String settingValue;
    private float budget;
    private PolicyType type;
    private int support;
    private ArrayList<Integer> supporters;
    public enum PolicyType {
        denounce, //target, time
        sanction, //target, time
        unsanction, //target
        unintervention, //target
        intervention, //target, time
        setting, //settingName, newValue
        custodian, //target, time
        budget, //amount
        mandate, //text
        amnesty //target, time
    }

    public int getTarget() {
        return target;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProposedBy() {
        return proposedBy;
    }

    public long getProposedTime() {
        return proposedTime;
    }

    public long getTime() {
        return time;
    }

    public String getSettingName() {
        return settingName;
    }

    public String getMandate() {
        return mandate;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public float getBudget() {
        return budget;
    }

    public PolicyType getType() {
        return type;
    }

    public int getSupport() {
        return support;
    }

    public Policy(PolicyType type, Object one, Object two, int proposedBy) {
        this.type = type;
        this.proposedTime = System.currentTimeMillis();
        switch (type) {
            case denounce, sanction, intervention, custodian, amnesty -> {
                this.target = (int) one;
                this.time = (int) two;
            }
            case unsanction -> this.target = (int) one;
            case setting -> {
                this.settingName = (String) one;
                this.settingValue = (String) two;
            }
            case budget -> this.budget = (float) one;
            case mandate -> this.mandate = (String) one;
        }
        this.supporters = new ArrayList<>();
        this.support = 0;
        this.proposedBy = proposedBy;
    }

    public void vote(int f, int amount) {
        support += amount;
        if(amount > 0 && !supporters.contains(f)) {
            supporters.add(f);
        }
    }

    public ArrayList<Integer> getSupporters() {
        return supporters;
    }

    public int getNumSupporters() {
        return supporters.size();
    }

    public Policy(Map<String, Object> serialized) {
        this.type = PolicyType.valueOf((String) serialized.get("type")); //(PolicyType) serialized.get("type");
        this.proposedBy = (int) serialized.get("proposedBy");
        this.proposedTime = (long) serialized.get("proposedTime");
        this.time = (int) serialized.getOrDefault("time", 0);
        this.settingName = (String) serialized.getOrDefault("settingName", null);
        this.mandate = (String) serialized.getOrDefault("mandate", null);
        this.settingValue = (String) serialized.getOrDefault("settingValue", null);
        try {
            this.budget = (float) serialized.getOrDefault("budget", 0f);
        } catch (Exception e) {
            this.budget = (float) (double)serialized.getOrDefault("budget", 0d);
        }
        this.supporters = (ArrayList<Integer>) serialized.getOrDefault("supporters", new ArrayList<>());
        this.id = (int) serialized.get("id");
        this.support = (int) serialized.get("support");
    }
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("type", type.toString());
        serialized.put("proposedBy", proposedBy);
        serialized.put("proposedTime", proposedTime);
        serialized.put("time", time);
        serialized.put("settingName", settingName);
        serialized.put("settingValue", settingValue);
        serialized.put("mandate", mandate);
        serialized.put("budget", budget);
        serialized.put("id", id);
        serialized.put("support", support);
        serialized.put("supporters", supporters);

        return serialized;
    }

    public long getTimeLeft() {
        return (proposedTime + Rivals.getPoliticsManager().getVotePassTime()) - System.currentTimeMillis();
    }

    public Faction getTargetFaction() {
        return Rivals.getFactionManager().getFactionByID(target);
    }

    public int getId() {
        return id;
    }
}