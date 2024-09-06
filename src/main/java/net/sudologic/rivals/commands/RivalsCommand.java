package net.sudologic.rivals.commands;

import com.google.common.primitives.Ints;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.sudologic.rivals.*;
import net.sudologic.rivals.managers.ClaimManager;
import net.sudologic.rivals.managers.FactionManager;
import net.sudologic.rivals.managers.ShopManager;
import net.sudologic.rivals.resources.ResourceSpawner;
import net.sudologic.rivals.util.NameFetcher;
import net.sudologic.rivals.util.UUIDFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class RivalsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        FactionManager manager = Rivals.getFactionManager();
        if(!(sender instanceof Player)) {
            Bukkit.getLogger().log(Level.INFO, "[Rivals] This command may only be run by players");
            return true;
        }
        Player p = (Player)sender;
        if(args.length >= 1) {
            Faction faction = Rivals.getFactionManager().getFactionByPlayer(p.getUniqueId());
            if("create".equals(args[0])) {//create submenu
                if(faction != null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must leave your current faction before making a new one." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Please include faction name" + ChatColor.RESET);
                    return true;
                }
                String name = args[1];
                if(manager.nameAlreadyExists(name)) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.RESET + name + ChatColor.LIGHT_PURPLE + " already exists." + ChatColor.RESET);
                    return true;
                }
                if(name.length() > (int)Rivals.getSettings().get("maxNameLength")) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " That name is too long." + ChatColor.RESET);
                    return true;
                }
                Faction f = new Faction(p.getUniqueId(), name, manager.getUnusedFactionID());
                if(manager.addFaction(f)) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + "Created the " + f.getColor() + name + " faction." + ChatColor.RESET);
                    p.sendMessage(ChatColor.BLUE + "For a list of things to do, use /rivals help" + ChatColor.RESET);
                    p.sendMessage(ChatColor.BLUE + "P.S., if you don't like your randomly chosen color, use /rivals color" + ChatColor.RESET);
                } else {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + "Unable to create faction." + ChatColor.RESET);
                }
                return true;
            }
            else if("kick".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be a part of a faction to kick people out of a faction." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Please include kicked player's name" + ChatColor.RESET);
                    return true;
                }
                if(!faction.getLeader().equals(p.getUniqueId())) {
                    String leaderName = NameFetcher.getName(faction.getLeader());
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be your faction's leader to kick players. Your faction leader is " + ChatColor.RESET + leaderName);
                    return true;
                }
                UUID kicked = UUIDFetcher.getUUID(args[1]);
                if(kicked == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Incorrect player name, did you spell it right?" + ChatColor.RESET);
                    return true;
                }
                if(faction.removeMember(kicked)) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Kicked " + args[1] + " from your faction." + ChatColor.RESET);
                    return true;
                }
            }
            else if("leader".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be a part of a faction to set your faction's leader" + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Please include new leader's name" + ChatColor.RESET);
                    return true;
                }
                if(!faction.getLeader().equals(p.getUniqueId())) {
                    String leaderName = NameFetcher.getName(faction.getLeader());
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be your faction's leader to set a new leader. Your faction leader is " + ChatColor.RESET + leaderName);
                    return true;
                }
                UUID newLeaderID = UUIDFetcher.getUUID(args[1]);
                if(newLeaderID == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Incorrect player name, did you spell it right?" + ChatColor.RESET);
                    return true;
                }
                if(!faction.getMembers().contains(newLeaderID)) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " That player is not in your faction." + ChatColor.RESET);
                    return true;
                }
                if(faction.setLeader(newLeaderID)) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Made " + args[1] + " the leader of your faction." + ChatColor.RESET);
                    faction.sendMessageToOnlineMembers(args[1] + ChatColor.LIGHT_PURPLE + " is now the leader of your faction." + ChatColor.RESET);
                    return true;
                }
            }
            else if("invite".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be a part of a faction to invite people." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Please include invited player's name" + ChatColor.RESET);
                    return true;
                }
                Player invited = Bukkit.getPlayer(args[1]);
                if(invited == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " That player is offline, please wait for them to log in before inviting them." + ChatColor.RESET);
                    return true;
                }
                if(faction.getMembers().contains(invited.getUniqueId())) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " That player is already in your faction." + ChatColor.RESET);
                    return true;
                }
                manager.addMemberInvite(invited.getUniqueId(), faction.getID());
                invited.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You have been invited to join " + faction.getColor() + faction.getName() + ChatColor.RESET);
                faction.sendMessageToOnlineMembers(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " " + invited.getName() + " has been invited to join your faction." + ChatColor.RESET);
                return true;
            }
            else if("invites".equals(args[0])) {
                List<Integer> invites = manager.getInvitesForPlayer(p.getUniqueId());
                if(invites.size() == 0) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " No factions have invited you." + ChatColor.RESET);
                    return true;
                }
                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Faction Invites:" + ChatColor.RESET);
                for(Integer invite : invites) {
                    Faction f = manager.getFactionByID(invite);
                    p.sendMessage(f.getColor() + f.getName());
                }
                p.sendMessage(ChatColor.YELLOW + "Use /rivals <factionName> to join a faction.");
                return true;
            }
            else if("join".equals(args[0])) {
                if(faction != null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must leave your current faction before you can join another." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Please include the faction's name." + ChatColor.RESET);
                    return true;
                }
                String fName = args[1];
                List<Integer> invites = manager.getInvitesForPlayer(p.getUniqueId());
                for(int f : invites) {
                    if(manager.getFactionByID(f).getName().equals(fName)) {
                        manager.getFactionByID(f).addMember(p.getUniqueId());
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You've joined " + ChatColor.RESET + fName);
                        manager.removeMemberInvite(p.getUniqueId(), f);
                        return true;
                    }
                }
                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " That faction either hasn't invited you or doesn't exist." + ChatColor.RESET);
            }
            else if("leave".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction in order to leave it." + ChatColor.RESET);
                    return true;
                }
                faction.removeMember(p.getUniqueId());
                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You are no longer a member of " + ChatColor.RESET + faction.getName());
                return true;
            }
            else if("enemy".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to declare war." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must specify a faction to declare war on." + ChatColor.RESET);
                    return true;
                }
                String enemyName = args[1];
                boolean now = false;
                if(args.length > 2) {
                    now = args[2].equals("now");
                }
                Faction enemy = manager.getFactionByName(enemyName);

                if(now) {
                    boolean mutual = enemy != null;
                    for(Integer a : faction.getAllies()) {
                        if(enemy.getEnemies().contains(a))
                            mutual = true;
                    }
                    if(faction.getPower() > (double)Rivals.getSettings().get("nowWarPower") || mutual) {
                        if(!mutual)
                            faction.rawPowerChange((double)Rivals.getSettings().get("nowWarPower") * -1);
                        if(faction.addEnemy(enemy.getID())) {
                            p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You are now enemies with " + ChatColor.RESET + enemyName);
                            faction.changeWarmongering(.5);
                        } else {
                            p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Could not declare war on " + ChatColor.RESET + enemyName + ChatColor.LIGHT_PURPLE + ", there might not be a faction by that name." + ChatColor.RESET);
                            Faction imprecise = manager.getFactionByNameImprecise(enemyName);
                            if(imprecise != null) {
                                p.sendMessage(ChatColor.RED + "There is a faction named "  + ChatColor.RESET + imprecise.getName());
                            }
                        }
                    } else {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction does not have enough power to declare war immediately." + ChatColor.RESET);
                    }
                } else {
                    if(enemy == null) {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Could not declare war on " + ChatColor.RESET + enemyName + ChatColor.LIGHT_PURPLE + ", there might not be a faction by that name." + ChatColor.RESET);
                        Faction imprecise = manager.getFactionByNameImprecise(enemyName);
                        if(imprecise != null) {
                            p.sendMessage("There is a faction named " + imprecise.getName());
                        }
                    } else {
                        int delay = (int) Rivals.getSettings().get("warDelay");
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Sent war declaration to " + ChatColor.RESET + enemyName + ChatColor.LIGHT_PURPLE + ". They have " + delay + " hours to prepare." + ChatColor.RESET);
                        manager.createWarDeclaration(faction.getID(), enemy.getID(), System.currentTimeMillis(), delay);
                        faction.changeWarmongering(.1);
                    }
                }
                return true;
            }
            else if("ally".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to invite another faction to an alliance." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must specify a faction to invite to an alliance." + ChatColor.RESET);
                    return true;
                }
                String allyName = args[1];
                Faction ally = manager.getFactionByName(allyName);
                if(ally == null) {
                    Faction imprecise = manager.getFactionByNameImprecise(allyName);
                    if(imprecise != null) {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " There is no faction with that name. Maybe you meant " + ChatColor.RESET + imprecise.getName());
                    } else {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " There is no faction with that name." + ChatColor.RESET);
                    }
                    return true;
                }
                if(manager.getAllyInvitesForFaction(faction.getID()).contains(ally.getID())) {
                    faction.addAlly(ally.getID());
                    manager.removeAllyInvite(ally.getID(), faction.getID());
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You are now allies with " + ChatColor.RESET + ally.getName());
                } else {
                    manager.addAllyInvite(faction.getID(), ally.getID());
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Sent alliance invite to " + ChatColor.RESET + ally.getName() + ChatColor.LIGHT_PURPLE + ". Ask them to run '/rivals ally " + faction.getName() + "' to accept.");
                }
                return true;
            }
            else if("peace".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to send a peace offer." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must specify a faction to offer peace." + ChatColor.RESET);
                    return true;
                }
                String enemyName = args[1];
                Faction enemy = manager.getFactionByName(enemyName);
                if(!faction.getEnemies().contains(enemy.getID())) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You are not at war with "  + ChatColor.RESET + enemy.getName());
                    Faction imprecise = manager.getFactionByNameImprecise(enemyName);
                    if(imprecise != null && faction.getEnemies().contains(imprecise.getID())) {
                        p.sendMessage("You ARE at war with" + ChatColor.RESET + imprecise.getName());
                    }
                    return true;
                }
                if(manager.getPeaceInvitesForFaction(faction.getID()).contains(enemy.getID())) {
                    faction.removeEnemy(enemy.getID());
                    manager.removePeaceInvite(enemy.getID(), faction.getID());
                } else {
                    manager.addPeaceInvite(faction.getID(), enemy.getID());
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Sent peace offer to " + ChatColor.RESET + enemy.getName() + ChatColor.LIGHT_PURPLE + ", they can run the same command to accept the offer.");
                }
                return true;
            }
            else if("unally".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to end an alliance." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must specify a faction to end alliance." + ChatColor.RESET);
                    return true;
                }
                String allyName = args[1];
                Faction ally = manager.getFactionByName(allyName);
                if(!faction.getAllies().contains(ally.getID())) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You are not allied with " + ChatColor.RESET + ally.getName());
                    Faction imprecise = manager.getFactionByNameImprecise(allyName);
                    if(imprecise != null && faction.getAllies().contains(imprecise.getID())) {
                        p.sendMessage("You ARE allied with" + ChatColor.RESET + imprecise.getName());
                    }
                    return true;
                }
                faction.removeAlly(ally.getID());
                return true;
            }
            else if("claim".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to claim land." + ChatColor.RESET);
                    return true;
                }
                ClaimManager claimManager = Rivals.getClaimManager();
                Chunk c = p.getLocation().getChunk();
                double myStrength = claimManager.getClaimStrength(faction);
                if(3 * myStrength < 1) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction is not powerful enough to claim this land." + ChatColor.RESET);
                    return true;
                }
                if(faction.addClaim(c)) {
                    faction.sendMessageToOnlineMembers("Claimed chunk X: " + c.getX() + " Z: " + c.getZ() + " in " + c.getWorld().getName() + ".");
                } else {
                    ProtectedRegion existingClaim = claimManager.getExistingClaim(c);
                    if(existingClaim != null) {
                        String id = existingClaim.getId();
                        Faction f = manager.getFactionByID(Integer.valueOf(id.split("_")[2]));
                        if(f != null) {
                            if(faction.getHostileFactions().contains(f.getID())) {
                                double enemyStrength = claimManager.getClaimStrength(f);
                                f.rawPowerChange(-2.5);
                                myStrength = claimManager.getClaimStrength(faction);
                                if(myStrength > enemyStrength && 3 * myStrength >= 1) {
                                    claimManager.removeClaim(c, f);
                                    claimManager.createClaim(c, faction);
                                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You have taken this chunk from " + ChatColor.RESET + f.getName());
                                } else {
                                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction is not powerful enough to take this claim from " + ChatColor.RESET + f.getName());
                                    f.rawPowerChange(2.5);
                                }
                            } else {
                                if(f.equals(faction)) {
                                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction already claims this chunk." + ChatColor.RESET);
                                } else {
                                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " This chunk is already claimed by " + ChatColor.RESET + f.getName());
                                }
                            }
                            return true;
                        }
                    }
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " For unknown reasons, you cannot claim this chunk." + ChatColor.RESET);
                    return true;
                }
            }
            else if("trust".equals(args[0])) {//share the claim you're standing in with a faction
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to share claims." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must specify a faction to share the claim with" + ChatColor.RESET);
                    return true;
                }
                String shareName = args[1];
                Faction share = manager.getFactionByName(shareName);
                if(share == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " There is no faction by that name." + ChatColor.RESET);
                    Faction imprecise = manager.getFactionByNameImprecise(shareName);
                    if(imprecise != null) {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Maybe you meant " + ChatColor.RESET + imprecise.getName());
                    }
                    return true;
                }
                if(!faction.getAllies().contains(share.getID())) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be allied with a faction to share claims." + ChatColor.RESET);
                    return true;
                }
                Chunk c = p.getLocation().getChunk();
                ProtectedRegion region = Rivals.getClaimManager().getExistingClaim(c);
                if(region == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be standing in a claimed chunk to share it." + ChatColor.RESET);
                    return true;
                }
                if(region.getId().split("_")[2].equals(String.valueOf(faction.getID()))) {
                    if(Rivals.getClaimManager().addFactionToRegion(share, c)) {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Shared your claim with " + ChatColor.RESET + share.getName());
                        return true;
                    }
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Could not share your claim with " + ChatColor.RESET + share.getName());
                    return true;
                } else {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be standing in a claim owned by your faction to share it." + ChatColor.RESET);
                    return true;
                }
            }
            else if("resource".equals(args[0])) {//list all resource chunks with certain parameters
                //syntax: /resource <type | distance>, returns all chunks with that resource type or within that distance.
                //user can also supply both a type and a distance to use both filters.
                //Returned list will also display chunk owning faction if applicable.
                if(args.length < 1) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must specify a resource type or distance." + ChatColor.RESET);
                    return true;
                }
                Integer distance = Ints.tryParse(args[0]);
                String typeString = args[0];
                Material type;
                if(distance != null) {
                    if(args.length > 1) {
                        typeString = args[1];
                    }
                }
                try {
                    type = Material.valueOf(typeString);
                } catch (IllegalArgumentException e) {
                    type = null;
                }
                ArrayList<ResourceSpawner> spawners = Rivals.getResourceManager().getSpawners();
                if(distance != null) {
                    spawners = Rivals.getResourceManager().filterByDist(p.getLocation(), distance, spawners);
                }
                if(type != null) {
                    spawners = Rivals.getResourceManager().filterByType(type, spawners);
                }
                if(spawners.size() == 0) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " No resources found." + ChatColor.RESET);
                    return true;
                }
                //sort by distance to player
                spawners.sort((ResourceSpawner a, ResourceSpawner b) -> {
                    return (int)(a.getLocation().distance(p.getLocation()) - b.getLocation().distance(p.getLocation()));
                });
                //trim to only the first 10 chunks
                if(spawners.size() > 10) {
                    spawners = new ArrayList<>(spawners.subList(0, 10));
                }
                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Resource Chunks:" + ChatColor.RESET);
                String rep = "";
                if(spawners.size() > 0) {
                    for (ResourceSpawner spawner : spawners) {
                        Chunk c = spawner.getLocation().getChunk();
                        ProtectedRegion region = Rivals.getClaimManager().getExistingClaim(c);
                        Faction f = null;
                        if (region != null) {
                            String[] parts = region.getId().split("_");
                            f = manager.getFactionByID(Integer.valueOf(parts[2]));
                        }
                        rep += ChatColor.LIGHT_PURPLE + "World: " + spawner.getLocation().getWorld().getName() + " X: " + spawner.getLocation().getBlockX() + " Z: " + spawner.getLocation().getBlockZ() + " Type: " + spawner.getMaterial();
                        if (f != null) {
                            rep += " Owner: " + f.getColor() + f.getName() + "\n";
                        }
                    }
                    p.sendMessage(rep);
                    return true;
                } else {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " No resources found." + ChatColor.RESET);
                    //display syntax
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Syntax: /resource <type | distance>, returns all chunks with that resource type or within that distance." + ChatColor.RESET);
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You can also supply both a type and a distance to use both filters." + ChatColor.RESET);
                    return true;
                }
            }
            else if("untrust".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to unshare claims." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must specify a faction to unshare the claim with" + ChatColor.RESET);
                    return true;
                }
                String unshareName = args[1];
                Faction unshare = manager.getFactionByName(unshareName);
                if(unshare == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " There is no faction by that name." + ChatColor.RESET);
                    Faction imprecise = manager.getFactionByNameImprecise(unshareName);
                    if(imprecise != null) {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Maybe you meant " + ChatColor.RESET + imprecise.getName());
                    }
                    return true;
                }
                Chunk c = p.getLocation().getChunk();
                ProtectedRegion region = Rivals.getClaimManager().getExistingClaim(c);
                if(region == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be standing in a claimed chunk to unshare it." + ChatColor.RESET);
                    return true;
                }
                if(region.getId().split("_")[2].equals(String.valueOf(faction.getID()))) {
                    if(Rivals.getClaimManager().removeFactionFromRegion(unshare, c)) {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Unshared your claim with " + ChatColor.RESET + unshare.getName());
                        return true;
                    }
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Could not unshare your claim with " + ChatColor.RESET + unshare.getName());
                    return true;
                } else {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be standing in a claim owned by your faction to unshare it." + ChatColor.RESET);
                    return true;
                }
            }
            else if("claims".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to view your claims." + ChatColor.RESET);
                    return true;
                }
                List<String> claims = faction.getRegions();
                if(claims.size() == 0) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction has no claims." + ChatColor.RESET);
                    return true;
                }
                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction's claims:" + ChatColor.RESET);
                for(String claim : claims) {
                    String[] parts = claim.split("_");
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "World: " + parts[1] + " X: " + parts[3] + " Z: " + parts[4]);
                }
                return true;
            }
            else if("unclaim".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to unclaim land." + ChatColor.RESET);
                    return true;
                }
                Chunk c = p.getLocation().getChunk();
                if(faction.removeClaim(c)) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Removed your claim to chunk " + ChatColor.RESET + c.getX() + " " + c.getZ());
                    return true;
                } else {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction does not claim chunk " + ChatColor.RESET + c.getX() + " " + c.getZ());
                }
            }
            else if("info".equals(args[0])) {
                if(args.length < 2) {
                    if(faction == null) {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to get info on your own faction. Add a faction name to look up their info." + ChatColor.RESET);
                        return true;
                    }
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Info on " + ChatColor.COLOR_CHAR + faction.getColor().toString() + faction.getName());
                    sendFactionInfo(p, faction, "");
                    return true;
                }
                String factionName = args[1];
                Faction f = manager.getFactionByName(factionName);
                if(f == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " There is no faction by that name." + ChatColor.RESET);
                    Faction imprecise = manager.getFactionByNameImprecise(factionName);
                    if(imprecise != null) {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Maybe you meant " + ChatColor.RESET + imprecise.getName());
                    }
                    return true;
                }
                if(args.length == 3) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Info on " + ChatColor.COLOR_CHAR + f.getColor().toString() + f.getName());
                    sendFactionInfo(p, f, args[2]);
                    if(faction == f) {//player has looked up their own faction by name
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " HINT You don't need to specify your own faction. You could have used /rivals info" + ChatColor.RESET);
                    }
                    return true;
                }
                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Info on " + ChatColor.COLOR_CHAR + f.getColor().toString() + f.getName());
                sendFactionInfo(p, f, "");
            }
            else if("list".equals(args[0])) {
                int perPage = 8;
                List<Integer> rankings = manager.getFactionRankings();
                List<Faction> factions = manager.getFactions();
                // Calculate the number of pages correctly to avoid an extra empty page
                int numPages = rankings.size() % perPage == 0 ? rankings.size() / perPage : (rankings.size() / perPage) + 1;
                int start = 0;
                if(rankings.size() == 0) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " There aren't any factions yet." + ChatColor.RESET);
                    return true;
                }
                String mess = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Factions List Page 1/" + numPages;
                if(args.length >= 2) {
                    try {
                        Integer page = Integer.parseInt(args[1]);
                        if (page <= 0 || page > numPages) {
                            p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Invalid page number. There are only " + numPages + " pages.");
                            return true;
                        }
                        mess = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Factions List Page " + page + "/" + numPages;
                        start = (page - 1) * perPage;
                    } catch (NumberFormatException e) {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Invalid page number format.");
                        return true;
                    }
                }
                for(int i = start; i < perPage + start && i < rankings.size(); i++) {
                    manager.buildFactionRanks();
                    int factionIndex = rankings.get(i);
                    // Check if the index is within the bounds of the factions list
                    if (factionIndex < factions.size()) {
                        Faction f = factions.get(factionIndex);
                        mess += "\n" + ChatColor.YELLOW + (i + 1) + " " + ChatColor.COLOR_CHAR + f.getColor().toString() + f.getName() + " " + ChatColor.RESET + f.getPower();
                    } else {
                        // Handle the case where the factionIndex is out of bounds
                        mess += "\n" + ChatColor.RED + "Error: " + ChatColor.RESET + "Faction ranking out of bounds.";
                    }
                }
                p.sendMessage(mess);
                return true;
                /*int perPage = 8;
                List<Faction> factions = manager.getFactions();
                int numPages = (factions.size() / perPage) + 1;
                int start = 0;
                if(factions.size() == 0) {
                    p.sendMessage("[Rivals] There aren't any factions yet.");
                    return true;
                }
                String mess = "[Rivals] Factions List Page 1/" + numPages;
                if(args.length >= 2) {
                    Integer page = Integer.parseInt(args[1]);
                    mess = "[Rivals] Factions List Page " + page + "/" + numPages;
                    if (page > numPages) {
                        p.sendMessage("[Rivals] There are only " + numPages + " pages.");
                        return true;
                    }
                    start = (page - 1) * perPage;
                }
                for(int i = start; i < perPage + start && i < manager.getFactions().size(); i++) {
                    mess += "\n" + ChatColor.COLOR_CHAR + factions.get(i).getColor().toString() + factions.get(i).getName();
                }
                p.sendMessage(mess);
                return true;*/
            }
            else if("map".equals(args[0])) {
                Chunk c = p.getLocation().getChunk();
                int CHUNK_Z =c.getZ();
                int CHUNK_X = c.getX();
                String mess = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Map of your surroundings" + ChatColor.RESET;
                String facts = "\nFactions: ";
                for(int z = -4; z <= 4; z++) {
                    String row = "\n| ";
                    for(int x = -4; x <= 4; x++) {
                        Chunk loc = c.getWorld().getChunkAt(CHUNK_X + x, CHUNK_Z + z);
                        ProtectedRegion claim = Rivals.getClaimManager().getExistingClaim(loc);
                        if(claim != null) {
                            Faction f = manager.getFactionByID(Integer.parseInt(claim.getId().split("_")[2]));
                            if(z == 0 && (x == -1 || x == 0)){
                                row += ChatColor.COLOR_CHAR + f.getColor().toString() + "X " + ChatColor.COLOR_CHAR + ChatColor.GREEN + "| ";
                            } else{
                                row += ChatColor.COLOR_CHAR + f.getColor().toString() + "X " + ChatColor.COLOR_CHAR + ChatColor.RESET + "| ";
                            }
                            if(!facts.contains(f.getName())) {
                                facts += ChatColor.COLOR_CHAR + f.getColor().toString() + f.getName() + ChatColor.COLOR_CHAR + ChatColor.RESET + " ";
                            }
                        } else {
                            row += "_ | ";
                        }
                    }
                    mess += row;
                }
                if("\nFactions: ".equals(facts)) {
                    facts = "There are no nearby factions";
                }
                mess += facts;
                p.sendMessage(mess);
            }
            else if("color".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to set the faction color." + ChatColor.RESET);
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must include a color code to set your color. Example: c" + ChatColor.RED + " for red." + ChatColor.RESET);
                    return true;
                }
                String colorString = args[1];
                ChatColor c = ChatColor.getByChar(colorString);
                if(c == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Invalid color code. Example: c" + ChatColor.RED + " for red." + ChatColor.RESET);
                    return true;
                }
                if(c.equals(ChatColor.MAGIC)) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Looks cool, sorry I can't allow it." + ChatColor.RESET);
                    return true;
                }
                if(c.equals(ChatColor.BLACK)) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Sorry, then nobody will be able to read your name." + ChatColor.RESET);
                    return true;
                }
                faction.setColor(c);
                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Successfully changed faction color to " + faction.getColor() + faction.getName());
                return true;
            }
            else if("cashout".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to cash out." + ChatColor.RESET);
                    return true;
                }
                int amount;
                if(args.length < 2) {
                    amount = (int) faction.remInfluence(Math.floor(faction.getInfluence()));
                } else {
                    amount = (int) faction.remInfluence(Integer.parseInt(args[1]));
                }
                p.getInventory().addItem(new ItemStack(Material.DIAMOND, amount));
            }
            else if("help".equals(args[0])) {
                String[] commands = {
                    "§e/rivals create <factionName> §f- Creates a new Faction.",
                    "§e/rivals kick <playerName> §f- Kicks player from faction if you are leader.",
                    "§e/rivals leader <playerName> §f- Promote a player to team leader.",
                    "§e/rivals invite <playerName> §f- Invites a player to your faction.",
                    "§e/rivals invites §f- Lists your current invites.",
                    "§e/rivals join <factionName> §f- Joins a faction that has invited you.",
                    "§e/rivals leave §f- Leaves your current faction.",
                    "§e/rivals enemy <factionName> §f- Declare another faction to be your enemy.",
                    "§e/rivals ally <factionName> §f- Propose/Accept faction alliance.",
                    "§e/rivals peace <factionName> §f- Propose/Accept peace with another faction.",
                    "§e/rivals unally <factionName> §f- Ends your alliance with another faction.",
                    "§e/rivals claim §f- Claim the chunk you are standing in for your faction.",
                    "§e/rivals unclaim §f- Unclaim a chunk your faction owns.",
                    "§e/rivals info <factionName> §f- Display info for a faction.",
                    "§e/rivals list <pageNumber> §f- Display the faction list, you may specify a page number.",
                    "§e/rivals map §f- Display a map of nearby claims.",
                    "§e/rivals color <colorCode> §f- Sets the color for your faction using Minecraft color codes.",
                    "§e/rivals cashout [amount] §f- Cash out your faction's influence for diamonds.",
                    "§e/rivals pay <factionName> <amount> §f- Pay influence to another faction.",
                    "§e/rivals shop [create/close] §f- Manage your faction's shop."
                    // "§e/rivals INSERT NAME HERE §f- INSERT DESCRIPTION HERE."
                };

                int perPage = 6;
                int page = 1;

                if(args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        p.sendMessage("§c[Rivals] §fInvalid page number.");
                        return true;
                    }
                }

                int start = (page - 1) * perPage;
                int end = start + perPage;

                if(start < 0 || start >= commands.length) {
                    p.sendMessage("§c[Rivals] §fInvalid page number.");
                    return true;
                }

                // Add spacing
                p.sendMessage("");
                p.sendMessage("");
                p.sendMessage("");

                    p.sendMessage("§6[Rivals] §fHelp Menu (Page §a" + page + "§f):");

                    for(int i = start; i < end && i < commands.length; i++) {
                        p.sendMessage(commands[i]);
                    }

                    // Page navigation
                    TextComponent pageNavigation = new TextComponent();

                    // Previous page
                    if(page > 1) {
                        TextComponent prevPage = new TextComponent("§b[Previous Page]");
                        prevPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rivals help " + (page - 1)));
                        pageNavigation.addExtra(prevPage);
                    }

                    // Separator
                    TextComponent separator = new TextComponent("   ");
                    pageNavigation.addExtra(separator);

                    // Next page
                    if(end < commands.length) {
                        TextComponent nextPage = new TextComponent("§b[Next Page]");
                        nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rivals help " + (page + 1)));
                        pageNavigation.addExtra(nextPage);
                    }

                    p.spigot().sendMessage(pageNavigation);

                    return true;
            }
            else if("pay".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to pay them.");
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Please include the name of the faction you wish to pay");
                    return true;
                }
                if(args.length < 3) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Please include the amount you wish to pay");
                    return true;
                }
                String factionName = args[1];
                int amount = Integer.parseInt(args[2]);
                Faction payF = manager.getFactionByNameImprecise(factionName);
                if(faction.getInfluence() < amount) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You can't afford to pay that much influence");
                } else {
                    faction.remInfluence(amount);
                    payF.addInfluence(amount);
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Paid " + amount + " influence to " + payF.getColor() + payF.getName());
                }
                return true;
            }
            else if("shop".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to access your faction's shop");
                    return true;
                }
                ShopManager shopManager = Rivals.getShopManager();
                Shopkeeper shopkeeper = shopManager.getShopkeeperForFaction(faction);
                if(args.length == 2) {
                    if("create".equals(args[1])) {
                        if (shopkeeper != null) {
                            p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction already has a shop!");
                            return true;
                        } else {
                            if (faction.getPower() < (double) Rivals.getSettings().get("minShopPower")) {
                                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction has too little power to open a shop.");
                                return true;
                            }
                            if (shopManager.setupShop(faction)) {
                                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Created a shop for your faction.");
                                int x = shopManager.getShopkeeperForFaction(faction).getX();
                                int y = shopManager.getShopkeeperForFaction(faction).getY();
                                int z = shopManager.getShopkeeperForFaction(faction).getZ();
                                p.sendMessage("It is at (" + x + ", " + y + ", " + z + ").");
                                return true;
                            } else {
                                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Unable to create a shop. There might not be any open spaces.");
                                return true;
                            }
                        }
                    }
                    else if("close".equals(args[1])) {
                        if (shopkeeper == null) {
                            p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction doesn't have a shop.");
                            return true;
                        } else {
                            if(shopManager.removeShop(faction)) {
                                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Closed your factions shop.");
                            } else {
                                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " For some unknown reason, we can't close your shop.");
                            }
                            return true;
                        }
                    }
                }
                if(shopkeeper != null) {
                    if(shopkeeper instanceof PlayerShopkeeper) {
                        ((PlayerShopkeeper) shopkeeper).setOwner(p);
                    }
                    if(shopkeeper.openEditorWindow(p)) {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Opening your shop's editor.");
                    } else {
                        p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Unable to open your shop's editor.");
                    }
                    return true;
                } else {//faction might not have shop.
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction doesn't have a shop. Create one with /rivals shop create");
                    return true;
                }
            }
            else if("rename".equals(args[0])) {
                if(faction == null) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " You must be in a faction to rename it!");
                    return true;
                }
                if(args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Please include faction name");
                    return true;
                }
                String name = args[1];
                if(manager.nameAlreadyExists(name)) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.RESET + " " + name + ChatColor.LIGHT_PURPLE + " already exists.");
                    return true;
                }
                if(name.length() > (int) Rivals.getSettings().get("maxNameLength")) {
                    p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " That name is too long.");
                    return true;
                }
                faction.setName(args[1]);
                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Your faction's name is now " + faction.getColor() + faction.getName());
                return true;
            }
            else {
                p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Invalid syntax");
            }
        }
        else {
            p.sendMessage(ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Options: create, kick, leader, invite, invites, join, leave, enemy, ally, peace, unally, claim, unclaim, info, list, map, color, cashout, help, pay, shop, rename");
        }
        return true;
    }

    public void sendFactionInfo(Player p, Faction f, String s) {
        FactionManager manager = Rivals.getFactionManager();
        String mess = "";
        if("".equals(s)) {
            mess = f.getName() + "\nPower: " + Rivals.getRoundedDecimal(f.getPower()) + "\nInfluence: " + Rivals.getRoundedDecimal(f.getInfluence()) + "\nWarmongering: " + Rivals.getRoundedDecimal(f.getWarmongering());
            String members = ChatColor.COLOR_CHAR + ChatColor.RESET.toString() + "\nMembers: ";
            if(f.getMembers().size() > 3) {
                for(int i = 0; i < 3; i++) {
                    members += Bukkit.getOfflinePlayer(f.getMembers().get(i)).getName() + ", ";
                }
                members += "+ " + (f.getMembers().size() - 3);
            } else if(f.getMembers().size() > 1){
                for(int i = 0; i < f.getMembers().size() - 1; i++) {
                    members += Bukkit.getOfflinePlayer(f.getMembers().get(i)).getName() + ", ";
                }
                members += "and " + Bukkit.getOfflinePlayer(f.getMembers().get(f.getMembers().size() - 1)).getName();
            } else {
                members += Bukkit.getOfflinePlayer(f.getMembers().get(0)).getName();
            }
            mess += members;

            String allies = "\nAllies: ";
            if(f.getAllies().size() > 0) {
                if(f.getAllies().size() > 3) {
                    for(int i = 0; i < 3; i++) {
                        allies += manager.getFactionByID(f.getAllies().get(i)) + ", ";
                    }
                    allies += "+ " + (f.getAllies().size() - 3);
                } else if(f.getAllies().size() > 1){
                    for(int i = 0; i < f.getAllies().size() - 1; i++) {
                        allies += manager.getFactionByID(f.getAllies().get(i)).getName() + ", ";
                    }
                    allies += "and " + manager.getFactionByID(f.getAllies().get(f.getAllies().size() - 1)).getName();
                } else {
                    allies += manager.getFactionByID(f.getAllies().get(0)).getName();
                }
            } else {
                allies += "None";
            }
            mess += allies;

            String enemies = "\nEnemies: ";
            if(f.getEnemies().size() > 0) {
                if(f.getEnemies().size() > 3) {
                    for(int i = 0; i < 3; i++) {
                        enemies += manager.getFactionByID(f.getEnemies().get(i)).getName() + ", ";
                    }
                    enemies += "+ " + (f.getEnemies().size() - 3);
                } else if(f.getEnemies().size() > 1){
                    for(int i = 0; i < f.getEnemies().size() - 1; i++) {
                        enemies += manager.getFactionByID(f.getEnemies().get(i)).getName() + ", ";
                    }
                    enemies += "and " + manager.getFactionByID(f.getEnemies().get(f.getEnemies().size() - 1)).getName();
                } else {
                    enemies += manager.getFactionByID(f.getEnemies().get(0)).getName();
                }
            } else {
                enemies += "None";
            }

            mess += enemies;

            String upcoming = "\nUpcoming Wars: ";
            List<Integer> u = manager.getUpcoming(f.getID());
            if(u.size() > 0) {
                if(u.size() > 3) {
                    for(int i = 0; i < 3; i++) {
                        upcoming += manager.getFactionByID(u.get(i)).getName() + ", ";
                    }
                    upcoming += "+ " + (u.size() - 3);
                } else if(u.size() > 1){
                    for(int i = 0; i < u.size() - 1; i++) {
                        upcoming += manager.getFactionByID(u.get(i)).getName() + ", ";
                    }
                    upcoming += "and " + manager.getFactionByID(u.get(u.size() - 1)).getName();
                } else {
                    upcoming += manager.getFactionByID(u.get(0)).getName();
                }
            } else {
                upcoming += "None";
            }

            mess += upcoming;

            String chunks = "\nChunks: " + f.getRegions().size();

            mess += chunks;

            String hint = "\nFor more info, add 'members', 'allies', 'enemies', or 'upcoming' to the command.";

            mess += hint;
        } else {
            if("members".equals(s)) {
                mess = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Members of " + f.getName();
                String members = "\n";
                if(f.getMembers().size() > 1) {
                    for(int i = 0; i < f.getMembers().size() - 1; i++) {
                        members += Bukkit.getOfflinePlayer(f.getMembers().get(i)).getName() + ", ";
                    }
                    members += "and " + Bukkit.getOfflinePlayer(f.getMembers().get(f.getMembers().size() - 1)).getName();
                } else {
                    members += Bukkit.getOfflinePlayer(f.getMembers().get(f.getMembers().size() - 1)).getName();
                }
                mess += members;
            }
            else if("allies".equals(s)) {
                mess = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Allies of " + f.getName();
                String allies = "\n";
                if(f.getAllies().size() > 1) {
                    for(int i = 0; i < f.getAllies().size() - 1; i++) {
                        allies += manager.getFactionByID(f.getAllies().get(i)).getName() + ", ";
                    }
                    allies += "and " + manager.getFactionByID(f.getAllies().get(f.getAllies().size() - 1)).getName();
                } else if(f.getAllies().size() > 0) {
                    allies += manager.getFactionByID(f.getAllies().get(f.getAllies().size() - 1)).getName();
                } else {
                    allies += "None";
                }
                mess += allies;
            }
            else if("enemies".equals(s)) {
                mess = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Enemies of " + f.getName();
                String enemies = "\n";
                if(f.getEnemies().size() > 1) {
                    for (int i = 0; i < f.getEnemies().size() - 1; i++) {
                        enemies += manager.getFactionByID(f.getEnemies().get(i)).getName() + ", ";
                    }
                    enemies += "and " + manager.getFactionByID(f.getEnemies().get(f.getEnemies().size() - 1)).getName();
                } else if(f.getEnemies().size() > 0) {
                    enemies += "and " + manager.getFactionByID(f.getEnemies().get(f.getEnemies().size() - 1)).getName();
                } else {
                    enemies += "None";
                }
                mess += enemies;
            } else if("upcoming".equals(s)) {
                mess = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Upcoming wars for " + f.getName();
                String str = "\n";
                List<Integer> upcoming = manager.getUpcoming(f.getID());
                if(upcoming.size() > 1) {
                    for (int i = 0; i < upcoming.size() - 1; i++) {
                        str += manager.getFactionByID(upcoming.get(i)).getName() + ", ";
                    }
                    str += "and " + manager.getFactionByID(upcoming.get(upcoming.size() - 1)).getName();
                } else if(f.getEnemies().size() > 0) {
                    str += "and " + manager.getFactionByID(upcoming.get(upcoming.size() - 1)).getName();
                } else {
                    str += "None";
                }
                mess += str;
            }
            else {
                mess = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Choose either 'members', 'allies', 'enemies', 'upcoming' to get details about a faction.";
            }
        }
        p.sendMessage(mess);
    }
}
