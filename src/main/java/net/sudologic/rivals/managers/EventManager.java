package net.sudologic.rivals.managers;

import com.nisovin.shopkeepers.api.events.*;
import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

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
        if (e.getEntity() instanceof Player && e.getEntity().getKiller() != null) {
            Location spawn = e.getEntity().getWorld().getSpawnLocation();
            //get player distance from spawn
            double distance = e.getEntity().getLocation().distance(spawn);
            distance = Math.max(1, distance * distance);

            //make warmongering decrease with distance from spawn, decrease should be exponential
            effectManager.changePlayerWarMongering(e.getEntity().getKiller().getUniqueId(), 1 / distance);
        } else if(e.getEntity() instanceof EnderDragon) {
            Rivals.getFactionManager().beginCrisis(Rivals.getFactionManager().getFactionByPlayer(e.getEntity().getKiller().getUniqueId()).getID());
        }
    }

    @EventHandler
    public void onBannerPlace(BlockPlaceEvent e) {
        if(e.getBlock().getType().toString().contains("BANNER")) {
            if(Rivals.getControlPointManager().isControlPoint(e.getBlock().getLocation())) {
                //check if player is in a faction
                Faction f = Rivals.getFactionManager().getFactionByPlayer(e.getPlayer().getUniqueId());
                if(f == null) {
                    e.getPlayer().sendMessage("[Rivals] You must be in a faction to place a banner here.");
                    e.setCancelled(true);
                    return;
                }
                //otherwise give the player's faction control of the control point
                Rivals.getControlPointManager().setControlPointOwner(e.getBlock().getLocation(), f.getID());
            }
        }
    }

    @EventHandler
    public void onFrameInteract(PlayerInteractEvent e) {//detect players adding eyes of ender to portal frames
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            if (b != null && b.getType() == Material.END_PORTAL_FRAME) {
                Player p = e.getPlayer();
                if(e.getItem() != null && e.getItem().getType() == Material.ENDER_EYE && !((EndPortalFrame)b).hasEye()) {
                    //Player placed an eye of ender in the portal frame
                    ItemMeta item = e.getItem().getItemMeta();
                    if(item == null) {
                        p.sendMessage("[Rivals] This Eye of Ender is not recognized by the portal.");
                        e.setCancelled(true);
                        return;
                    }
                    Faction f = Rivals.getFactionManager().getFactionByPlayer(p.getUniqueId());
                    if(f == null) {
                        p.sendMessage("[Rivals] You must be in a faction to place an Eye of Ender in the portal.");
                        e.setCancelled(true);
                        return;
                    }
                    int i = 0;
                    if(item.hasCustomModelData()) {
                        i = item.getCustomModelData();
                    }
                    switch (i) {
                        case 0:
                            p.sendMessage("[Rivals] This Eye of Ender is not recognized by the portal.");
                            e.setCancelled(true);
                            return;
                        case 1: //Guardian Eye
                            effectManager.addFactionEffect(f, PotionEffectType.RESISTANCE);
                            break;
                        case 2: //Heavy Core Eye
                            effectManager.addFactionEffect(f, PotionEffectType.STRENGTH);
                            break;
                        case 3: //Totem Eye
                            effectManager.addFactionEffect(f, PotionEffectType.REGENERATION);
                            break;
                        case 4: //Wither Eye
                            effectManager.addFactionEffect(f, PotionEffectType.RESISTANCE);
                            break;
                        case 5: //Warden Eye
                            effectManager.addFactionEffect(f, PotionEffectType.NIGHT_VISION);
                            break;
                        case 6: //Evoker Eye
                            effectManager.addFactionEffect(f, PotionEffectType.HEALTH_BOOST);
                            break;
                        case 7: //Blaze Eye
                            effectManager.addFactionEffect(f, PotionEffectType.FIRE_RESISTANCE);
                            break;
                    }
                }
            }
        }
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
        if(Rivals.getFactionManager().getTeleportOnJoin() == 1) {
            //if player is in Crisis faction and NOT in the End, teleport them to the End.
            if(manager.getFactionByPlayer(e.getPlayer().getUniqueId()).getID() == Rivals.getFactionManager().getCrisisFaction() && !e.getPlayer().getWorld().getEnvironment().equals(World.Environment.THE_END)) {
                e.getPlayer().teleport(Bukkit.getWorld("world_the_end").getSpawnLocation());
            }
            //if player is in the End, and NOT part of the Crisis faction, teleport them to their respawn location
            if(e.getPlayer().getWorld().getEnvironment().equals(World.Environment.THE_END) && manager.getFactionByPlayer(e.getPlayer().getUniqueId()).getID() != Rivals.getFactionManager().getCrisisFaction()) {
                e.getPlayer().teleport(e.getPlayer().getRespawnLocation());
            }
        } else if(Rivals.getFactionManager().getTeleportOnJoin() == 2) {//teleport members of Crisis faction to their respawn point, only if they are in the End
            if(manager.getFactionByPlayer(e.getPlayer().getUniqueId()).getID() == Rivals.getFactionManager().getCrisisFaction() && e.getPlayer().getWorld().getEnvironment().equals(World.Environment.THE_END)) {
                e.getPlayer().teleport(e.getPlayer().getRespawnLocation());
            }
        }
    }

    @EventHandler
    public void onSwitchDimension(PlayerChangedWorldEvent e) {
        //if during Crisis, stop Crisis faction from leaving the End.
        if(Rivals.getFactionManager().getTeleportOnJoin() == 1 &&
            Rivals.getFactionManager().getFactionByPlayer(e.getPlayer().getUniqueId()).getID() == Rivals.getFactionManager().getCrisisFaction()
            && !e.getPlayer().getWorld().getEnvironment().equals(World.Environment.THE_END)) {
                e.getPlayer().teleport(Bukkit.getWorld("world_the_end").getSpawnLocation());
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
        if(e.getEntityType() == EntityType.PLAYER && e.getDamager().getType() == EntityType.PLAYER) {
            if(Rivals.getFactionManager().isCrisis()) {
                //check if one player is in the Crisis faction and the other is not, otherwise cancel event
                if(Rivals.getFactionManager().getFactionByPlayer(e.getEntity().getUniqueId()).getID() == Rivals.getFactionManager().getCrisisFaction() &&
                    Rivals.getFactionManager().getFactionByPlayer(e.getDamager().getUniqueId()).getID() != Rivals.getFactionManager().getCrisisFaction()) {
                    e.setCancelled(true);
                    return;
                } else if(Rivals.getFactionManager().getFactionByPlayer(e.getEntity().getUniqueId()).getID() != Rivals.getFactionManager().getCrisisFaction() &&
                    Rivals.getFactionManager().getFactionByPlayer(e.getDamager().getUniqueId()).getID() == Rivals.getFactionManager().getCrisisFaction()) {
                    e.setCancelled(true);
                    return;
                }
            }
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
