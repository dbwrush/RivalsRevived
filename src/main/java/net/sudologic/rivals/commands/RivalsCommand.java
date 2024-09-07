package net.sudologic.rivals.commands;

import com.google.common.primitives.Ints;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.sudologic.rivals.*;
import net.sudologic.rivals.managers.ClaimManager;
import net.sudologic.rivals.managers.FactionManager;
import net.sudologic.rivals.managers.ShopManager;
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
                        mess += "\n" + ChatColor.YELLOW + (i + 1) + " " + ChatColor.COLOR_CHAR + f.getColor().toString() + f.getName() + " " + ChatColor.RESET;
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
            else if("help".equals(args[0])) {
                String[] commands = {
                    "§e/rivals create <factionName> §f- Creates a new Faction.",
                    "§e/rivals kick <playerName> §f- Kicks player from faction if you are leader.",
                    "§e/rivals leader <playerName> §f- Promote a player to team leader.",
                    "§e/rivals invite <playerName> §f- Invites a player to your faction.",
                    "§e/rivals invites §f- Lists your current invites.",
                    "§e/rivals join <factionName> §f- Joins a faction that has invited you.",
                    "§e/rivals leave §f- Leaves your current faction.",
                    "§e/rivals info <factionName> §f- Display info for a faction.",
                    "§e/rivals list <pageNumber> §f- Display the faction list, you may specify a page number.",
                    "§e/rivals color <colorCode> §f- Sets the color for your faction using Minecraft color codes.",
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
            mess = f.getName();
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
            else {
                mess = ChatColor.YELLOW + "[Rivals]" + ChatColor.LIGHT_PURPLE + " Add 'members' to get details about a faction.";
            }
        }
        p.sendMessage(mess);
    }
}
