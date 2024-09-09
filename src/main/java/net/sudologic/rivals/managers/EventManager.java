package net.sudologic.rivals.managers;

import com.nisovin.shopkeepers.api.events.*;
import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.Recipe;

import java.util.*;

public class EventManager implements Listener {
    private final Map<UUID, Double> combatTime;
    private final EffectManager effectManager;

    public EventManager(EffectManager effectManager) {
        combatTime = new HashMap<>();
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Location spawn = e.getEntity().getWorld().getSpawnLocation();
        Player p;
        if (e.getEntity() instanceof Player && e.getEntity().getKiller() != null) {
            p = e.getEntity().getKiller();
        }
        //get player distance from spawn
        double distance = e.getEntity().getLocation().distance(spawn);
        distance = Math.max(1, distance * distance);

        //make warmongering decrease with distance from spawn, decrease should be exponential
        effectManager.changePlayerWarMongering(e.getEntity().getKiller().getUniqueId(), 1 / distance);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        FactionManager manager = Rivals.getFactionManager();
        List<Integer> invites = manager.getInvitesForPlayer(e.getPlayer().getUniqueId());
        if(!Rivals.getScoreboardManager().getExcluded(e.getPlayer().getUniqueId()))
            Rivals.getScoreboardManager().assignScoreboard(e.getPlayer());
        if(manager.getFactionByPlayer(e.getPlayer().getUniqueId()) == null && invites.size() > 0) {
            String inviteMess = "[Rivals] You're invited to join " + manager.getFactionByID(invites.get(0)).getColor() + manager.getFactionByID(invites.get(0)).getName();
            e.getPlayer().sendMessage(inviteMess);
        } else {
            if(manager.getFactionByPlayer(e.getPlayer().getUniqueId()) == null) {
                e.getPlayer().sendMessage("[Rivals] You haven't joined a faction yet!");
                return;
            }
            e.getPlayer().sendMessage("[Rivals] Faction status:");
            Rivals.getCommand().sendFactionInfo(e.getPlayer(), manager.getFactionByPlayer(e.getPlayer().getUniqueId()), "");
            e.getPlayer().sendMessage("[Rivals] Server status:");//display currently interventioned factions & number of policy proposals
            e.getPlayer().sendMessage("[Rivals] Use /policy for more information on politics");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if(!Rivals.getScoreboardManager().getExcluded(e.getPlayer().getUniqueId()))
            Rivals.getScoreboardManager().removeScoreboard(e.getPlayer());
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) {
        FactionManager manager = Rivals.getFactionManager();
        manager.removeInvitesOver7Days();
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if(e.getSpawnReason().equals("CURED")){
            Villager v = (Villager) e.getEntity();
            for (int i = 0; i < v.getRecipes().size(); i++) {
                Recipe oldTrade = v.getRecipes().get(i);
                MerchantRecipe newTrade = new MerchantRecipe(new ItemStack(oldTrade.getResult()), Integer.MAX_VALUE);
                ArrayList<ItemStack> ingredients = new ArrayList<>();
                for (ItemStack x: v.getRecipes().get(i).getIngredients()) {
                    //why does v.getRecipes().get(i).getIngredients() work but not oldTrade.getIngredients()?
                    x.setAmount(1);
                    ingredients.add(x);
                }
                newTrade.setIngredients(ingredients);
                v.setRecipe(i,newTrade);
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if(e.getEntityType() == EntityType.PLAYER) {
            Player victim = (Player) e.getEntity();
            if(combatTime.containsKey(victim.getUniqueId())) {
                combatTime.remove(victim);
            }
            combatTime.put(victim.getUniqueId(), System.currentTimeMillis() + (double)Rivals.getSettings().get("combatTeleportDelay") * 1000);
        }
    }

    public boolean getCombat(UUID uuid) {
        if(!combatTime.containsKey(uuid)) {
            return false;
        }
        Double time = combatTime.get(uuid);
        if(time == null) {
            return false;
        }
        if(System.currentTimeMillis() > time) {
            combatTime.remove(uuid);
            return false;
        }
        return true;
    }

    public double combatTimeLeft(UUID uuid) {
        if(!combatTime.containsKey(uuid)) {
            return 0;
        }
        double time = combatTime.get(uuid);
        if(time == 0) {
            return 0;
        }
        return time - System.currentTimeMillis();
    }
}
