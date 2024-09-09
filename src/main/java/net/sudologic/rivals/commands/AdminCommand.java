package net.sudologic.rivals.commands;

import net.sudologic.rivals.Rivals;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class AdminCommand implements CommandExecutor {
    //data structure to match eye type to NBT data
    private static final Map<String, ItemMeta> eyeTypes = new HashMap<String, ItemMeta>() {{
    }};

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
            case "eye":
                // Give the sender an eye of ender with custom NBT data
                if (!(commandSender instanceof Player player)) {
                    commandSender.sendMessage("[Rivals] This command can only be run by a player.");
                    return true;
                }
                if (args.length < 2) {
                    commandSender.sendMessage("[Rivals] This subcommand requires an eye type");
                    return true;
                }
                if (args[1].equals("help")) {
                    showEyeTypes((Player) commandSender);
                    return true;
                }
                // Custom NBT data for the Eye of Ender
                ItemStack eye = new ItemStack(Material.ENDER_EYE);
                ItemMeta data = eyeTypes.get(args[1]);
                if (data == null) {
                    commandSender.sendMessage("[Rivals] Unknown eye type. Use /rivalsadmin eye help for a list of eye types.");
                    return true;
                }
                eye.setItemMeta(data);
                player.getInventory().addItem(eye);
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

    //show list of eye types to player
    public void showEyeTypes(Player player) {
        player.sendMessage("[Rivals] Eye Types:");
        for (String type : eyeTypes.keySet()) {
            player.sendMessage(type);
        }
    }
}