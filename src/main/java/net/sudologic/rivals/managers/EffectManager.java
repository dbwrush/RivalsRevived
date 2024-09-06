package net.sudologic.rivals.managers;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EffectManager {
    private HashMap<UUID, Double> playerWarMongering = new HashMap<>();

    public void setPlayerWarMongering(UUID playerID, double amount) {
        playerWarMongering.put(playerID, amount);
    }

    public void changePlayerWarMongering(UUID playerID, double amount) {
        Faction f = Rivals.getFactionManager().getFactionByPlayer(playerID);
        if(f != null && Rivals.getPoliticsManager().getAmnestyFactions().containsKey(f.getID())) {
            amount = 0;
        }
        playerWarMongering.put(playerID, getPlayerWarMongering(playerID) + amount);
    }

    public double getPlayerWarMongering(UUID playerID) {
        return playerWarMongering.getOrDefault(playerID, 0.0);
    }

    public void applyEffects(Player player, double intensity) {
        List<PotionEffect> effects = new ArrayList<>();
        if(intensity >= 5) {
            effects.add(new PotionEffect(PotionEffectType.HUNGER, 20 * 60 * 60, (int)intensity - 2, true, true));
        }
        if(intensity >= 4) {
            effects.add(new PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 60 * 60, (int)(intensity - 2)/2, true, true));
        }
        if(intensity >= 3) {
            effects.add(new PotionEffect(PotionEffectType.SLOW, 20 * 60 * 60, (int)intensity/2, true, true));
        }
        if(intensity >= 1) {
            effects.add(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 60 * 60, (int)intensity, true, true));
        }
        player.addPotionEffects(effects);
    }

    public void update() {
        PoliticsManager polMan = Rivals.getPoliticsManager();
        ArrayList<Integer> updatedFactions = new ArrayList<>();
        for(Player p : Bukkit.getOnlinePlayers()) {
            updatePlayer(p, polMan);
            setPlayerWarMongering(p.getUniqueId(), getPlayerWarMongering(p.getUniqueId()) * .9);
            Faction f = Rivals.getFactionManager().getFactionByPlayer(p.getUniqueId());
            if(f != null && !updatedFactions.contains(f.getID())) {
                f.setWarmongering(f.getWarmongering() * .9);
                updatedFactions.add(f.getID());
            }
        }
        /*for(Faction f : Rivals.getFactionManager().getFactions()) {
            for(UUID playerID : f.getMembers()) {
                Player p = Bukkit.getPlayer(playerID);
                if(p != null) {
                    updatePlayer(p, polMan);
                }
            }
            f.setWarmongering(f.getWarmongering() * .9);
        }*/
    }

    public void updatePlayer(Player p, PoliticsManager polMan) {
        if(p == null)
            return;
        long time = System.currentTimeMillis();
        double penalty = playerWarMongering.getOrDefault(p.getUniqueId(), 0.0);
        Faction f = Rivals.getFactionManager().getFactionByPlayer(p.getUniqueId());
        if(f != null) {
            penalty = f.getWarmongering();
            if (polMan.getDenouncedFactions().getOrDefault(f.getID(), 0L) > time) {
                penalty += 1;
            }
            if (polMan.getSanctionedFactions().getOrDefault(f.getID(), 0L) > time) {
                penalty += 2;
            }
            if (polMan.getInterventionFactions().getOrDefault(f.getID(), 0L) > time) {
                penalty += 3;
            }
        }
        if(penalty <= 0) {
            return;
        }
        applyEffects(p, penalty);
    }
}
