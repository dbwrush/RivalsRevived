package net.sudologic.rivals.commands.home;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import net.sudologic.rivals.managers.FactionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class DelHomeCommand implements CommandExecutor {
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
            if(args.length < 1) {
                p.sendMessage("[Rivals] Please specify which home to delete.");
            } else if(!faction.getHomes().containsKey(args[0])) {
                p.sendMessage("[Rivals] No home by that name, did you spell it correctly?");
            } else if(faction.delHome(args[0])) {
                p.sendMessage("[Rivals] Deleting home " + args[0]);
            } else {
                p.sendMessage("[Rivals] Failed to delete home " + args[0]);
            }
            return true;
        }
        p.sendMessage("[Rivals] You must be in a faction to delete a home.");
        return true;
    }
}