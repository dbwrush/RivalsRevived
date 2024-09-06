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

public class HomesCommand implements CommandExecutor {

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
            p.sendMessage("[Rivals] Homes for " + faction.getColor() + faction.getName());
            for(String home : faction.getHomes().keySet()) {
                p.sendMessage(home);
            }
            p.sendMessage("Used " + faction.getHomes().size() + " / " + faction.getMaxHomes());
            return true;
        }
        p.sendMessage("[Rivals] You must be in a faction to list homes.");
        return true;
    }
}