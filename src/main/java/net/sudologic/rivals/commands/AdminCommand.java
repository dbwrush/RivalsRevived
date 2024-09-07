package net.sudologic.rivals.commands;

import net.sudologic.rivals.Rivals;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (commandSender instanceof Player) {
            if (!commandSender.isOp() || !commandSender.hasPermission("rivals.admin")) {
                commandSender.sendMessage("[Rivals] You do not have permission to use this command.");
                return true;
            }
        }
        if (args.length < 1) {
            commandSender.sendMessage("[Rivals] Options: setMainShopRegion <id>, scanForShopRegions, forceChangeSetting <setting> <value>");
            return true;
        }
        switch (args[0]) {
            case "setMainShopRegion":
                if (args.length < 2) {
                    commandSender.sendMessage("[Rivals] This subcommand requires a region ID.");
                    return true;
                }
                Rivals.getShopManager().setMainRegionString(args[1]);
                commandSender.sendMessage("[Rivals] Set main shop region to " + args[1]);
                return true;
            case "scanForShopRegions":
                int count = Rivals.getShopManager().addSubregions();
                commandSender.sendMessage("[Rivals] There are now " + count + " shop subregions.");
                return true;
            case "setting":
                if (args.length < 3) {
                    commandSender.sendMessage("[Rivals] This subcommand requires a setting name and a new value.");
                    return true;
                }
                String settingName = args[1];
                String newValue = args[2];
                if (Rivals.changeSetting(settingName, newValue)) {
                    commandSender.sendMessage("[Rivals] Setting " + settingName + " changed to " + newValue + " successfully.");
                } else {
                    commandSender.sendMessage("[Rivals] Failed to change setting " + settingName + ".");
                }
                return true;
            case "help":
                commandSender.sendMessage("[Rivals] Admin Command Help:\n" +
                        "- /rivalsadmin setMainShopRegion <id>: Set the main shop region.\n" +
                        "- /rivalsadmin scanForShopRegions: Scan for shop subregions.\n" +
                        "- /rivalsadmin setting <setting> <value>: Forcefully change a setting.\n" +
                        "- /rivalsadmin stopProposal <id>: Stop a policy proposal.\n" +
                        "Use /rivalsadmin help for this message.");
                return true;
            default:
                commandSender.sendMessage("[Rivals] Unknown subcommand. Use /rivalsadmin help for a list of commands.");
                return true;
        }
    }
}