package net.sudologic.rivals.managers;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class EffectManager implements ConfigurationSerializable {
    private static HashMap<UUID, Double> playerWarMongering = new HashMap<>();
    private static HashMap<Integer, ArrayList<PotionEffect>> factionEffects = new HashMap<>();

    public EffectManager(Map<String, Object> serialized) {
        if(serialized.containsKey("effects")) {
            factionEffects.putAll((Map<Integer, ArrayList<PotionEffect>>) serialized.get("effects"));
        }
        if(serialized.containsKey("warMongering")) {
            playerWarMongering.putAll((Map<UUID, Double>) serialized.get("warMongering"));
        }
    }

    public EffectManager() {
    }

    public void setPlayerWarMongering(UUID playerID, double amount) {
        playerWarMongering.put(playerID, amount);
    }

    public void changePlayerWarMongering(UUID playerID, double amount) {
        playerWarMongering.put(playerID, getPlayerWarMongering(playerID) + amount);
    }

    public void addFactionEffect(Faction faction, PotionEffectType effect) {
        if(!factionEffects.containsKey(faction.getID())) {
            ArrayList<PotionEffect> effects = new ArrayList<>();
            factionEffects.put(faction.getID(), effects);
        }
        List<PotionEffect> effects = factionEffects.get(faction.getID());
        //check if effects already contains an effect of same type, if so add more strength to the effect
        for(PotionEffect e : effects) {
            if(e.getType().equals(effect)) {
                effects.remove(e);
                effects.add(new PotionEffect(e.getType(), e.getDuration(), e.getAmplifier() + 1));
                return;
            }
        }
    }

    public void removeFaction(Faction faction) {
        factionEffects.remove(faction.getID());
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("effects", factionEffects);
        serialized.put("warMongering", playerWarMongering);
        return serialized;
    }

    public double getPlayerWarMongering(UUID playerID) {
        return playerWarMongering.getOrDefault(playerID, 0.0);
    }

    public void update() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            setPlayerWarMongering(p.getUniqueId(), getPlayerWarMongering(p.getUniqueId()) * .9);
            updatePlayer(p);
        }
    }

    public void updatePlayer(Player p) {
        if(p == null)
            return;
        double penalty = playerWarMongering.getOrDefault(p.getUniqueId(), 0.0);
        if(penalty <= 0) {
            return;
        }
        Faction f = Rivals.getFactionManager().getFactionByPlayer(p.getUniqueId());
        if(f != null) {
            for(PotionEffect e : factionEffects.getOrDefault(f.getID(), new ArrayList<>())) {
                if(p.hasPotionEffect(e.getType())) {
                    p.removePotionEffect(e.getType());
                }
                p.addPotionEffect(e);
            }
        }
        p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20 - penalty);
    }
}
