package net.sudologic.rivals.commands.home;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import net.sudologic.rivals.managers.FactionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class SetHomeCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        FactionManager manager = Rivals.getFactionManager();
        if(!(sender instanceof Player)) {
            Bukkit.getLogger().log(Level.INFO, "[Rivals] This command may only be run by players");
            return true;
        }
        Player p = (Player)sender;
        Faction faction = manager.getFactionByPlayer(p.getUniqueId());
        if(faction != null) {
            if(faction.getHomes().size() < faction.getMaxHomes()) {
                if(args.length < 1) {
                    p.sendMessage("[Rivals] Please specify a home name.");
                    return true;
                }
                Location existing = faction.getHome(args[0]);
                if(existing != null) {
                    p.sendMessage("[Rivals] Home " + args[0] + " already exists at " + existing.getBlockX() + ", " + existing.getBlockY() + ", " + existing.getBlockZ() + ".");
                    return true;
                } else {
                    faction.setHome(args[0], p.getLocation());
                    p.sendMessage("[Rivals] Home " + args[0] + " set to " + p.getLocation().getBlockX() + ", " + p.getLocation().getBlockY() + ", " + p.getLocation().getBlockZ() + ".");
                }
            } else {
                p.sendMessage("[Rivals] Your faction needs more power to get more homes.");
            }
            return true;
        }
        p.sendMessage("[Rivals] You must be in a faction to set a home.");
        return true;
    }
}
