package net.sudologic.rivals.managers;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EffectManager {
    private final HashMap<UUID, Double> playerWarMongering = new HashMap<>();

    public void setPlayerWarMongering(UUID playerID, double amount) {
        playerWarMongering.put(playerID, amount);
    }

    public void changePlayerWarMongering(UUID playerID, double amount) {
        playerWarMongering.put(playerID, getPlayerWarMongering(playerID) + amount);
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
        p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20 - penalty);
    }
}
