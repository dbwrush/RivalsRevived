package net.sudologic.rivals.commands.home;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class HomeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(!(sender instanceof Player)) {
            Bukkit.getLogger().log(Level.INFO, "[Rivals] This command may only be run by players");
            return true;
        }
        Player p = (Player) sender;
        if(args.length < 1) {
            p.sendMessage("[Rivals] You must specify a home to visit. Use /homes to get a list of your homes, or /sethome to add a new home.");
            return true;
        }
        Faction f = Rivals.getFactionManager().getFactionByPlayer(p.getUniqueId());
        if(f != null) {
            Location h = f.getHome(args[0]);
            if(h != null) {
                if(Rivals.getEventManager().getCombat(p.getUniqueId())) {
                    p.sendMessage("[Rivals] You cannot teleport in combat. Please wait " + (Rivals.getEventManager().combatTimeLeft(p.getUniqueId()) / 1000) + " seconds.");
                    return true;
                }
                if(f.getHomes().size() > f.getMaxHomes()) {
                    p.sendMessage("[Rivals] Your faction has too many homes. Remove " + (f.getHomes().size() - f.getMaxHomes()) + " homes.");
                    return true;
                }
                p.sendMessage("[Rivals] Teleporting to " + args[0]);
                p.teleport(h);
                return true;
            }
            p.sendMessage("[Rivals] Your faction has no home by that name.");
            return true;
        }
        p.sendMessage("[Rivals] You must be in a faction to use homes.");
        return true;
    }
}
