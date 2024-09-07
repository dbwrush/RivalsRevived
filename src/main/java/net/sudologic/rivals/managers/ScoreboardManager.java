package net.sudologic.rivals.managers;

import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import net.sudologic.rivals.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class ScoreboardManager implements CommandExecutor {
    private final Map<UUID, FastBoard> boards = new HashMap<>();
    private final List<String> excluded;

    public ScoreboardManager(Server server) {
        server.getScheduler().runTaskTimer(Rivals.getPlugin(), () -> {
            for(UUID id : boards.keySet()) {
                updateScoreboard(id);
            }
        }, 0, 20);
        excluded = new ArrayList<>();
    }

    public void assignScoreboard(Player p) {
        FastBoard board = new FastBoard(p);
        boards.put(p.getUniqueId(), board);
        board.updateTitle(ChatColor.YELLOW + "-= Rivals =-");
        updateScoreboard(p.getUniqueId());
    }

    public void removeScoreboard(Player p) {
        FastBoard b = boards.remove(p.getUniqueId());
        if(b != null) {
            b.delete();
        }
    }

    public void updateScoreboard(UUID id) {
        FastBoard b = boards.get(id);
        Faction f = Rivals.getFactionManager().getFactionByPlayer(id);
        Location l = Bukkit.getPlayer(id).getLocation();
        String loc = "Location: " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ();
        if(f != null) {
            b.updateLines("Faction: " + f.getColor() + f.getName(),
                    "Members: " + ChatColor.WHITE + f.countOnlineMembers() + "/" + f.getMembers().size(),
                    "WarMongering: " + ChatColor.WHITE + Rivals.getRoundedDecimal(Rivals.getEffectManager().getPlayerWarMongering(id)),
                    "In Combat: " + ChatColor.WHITE + combatString(Rivals.getEventManager().combatTimeLeft(id)),
                    loc);
        } else {
            b.updateLines("Faction: " + ChatColor.WHITE + "None",
                    "Join or create a faction", "using /rivals",
                    loc);
        }
    }

    private String combatString(double time) {
        if(time <= 0) {
            return ChatColor.WHITE + "False";
        } else {
            return ChatColor.RED + String.valueOf(Rivals.getRoundedDecimal(time / 1000));
        }
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("[Rivals] Only players may run this command.");
            return true;
        }
        Player p = (Player) commandSender;
        if(excluded.contains(p.getUniqueId().toString())) {
            excluded.remove(p.getUniqueId().toString());
            assignScoreboard(p);
        } else {
            excluded.add(p.getUniqueId().toString());
            removeScoreboard(p);
        }
        return true;
    }

    public boolean getExcluded(UUID uuid) {
        return excluded.contains(uuid.toString());
    }
}
