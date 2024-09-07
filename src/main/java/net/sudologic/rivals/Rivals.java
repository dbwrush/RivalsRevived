package net.sudologic.rivals;

import net.sudologic.rivals.commands.AdminCommand;
import net.sudologic.rivals.commands.RivalsCommand;
import net.sudologic.rivals.commands.home.DelHomeCommand;
import net.sudologic.rivals.commands.home.HomeCommand;
import net.sudologic.rivals.commands.home.HomesCommand;
import net.sudologic.rivals.commands.home.SetHomeCommand;
import net.sudologic.rivals.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/*
TODO: Add control points
 - Detect placement or removal of faction banner on a control point
 - Display progress towards capturing a control point
 - Display which faction owns a control point
 - Reward owning facton with Eye of Ender at 1 per week
TODO: Add Crisis Faction
 - When Dragon is killed, Crisis mode activates
 - Dragon killing faction becomes Crisis faction
 - All other factions cannot PvP each other, only against the Crisis faction
 - All other factions teleported out of End
 - End portals disabled
 - Crisis faction cannot exit End for 2 days
 - At end of 2 days, Crisis faction teleported to one of the Control Points
TODO: Add a custom command to give admins an Eye of Ender with custom NBT data
TODO: Add permanent potion effects to factions that place an Eye of Ender into the portal
 - Effect is based on NBT data on the Eye they used.
 */

public final class Rivals extends JavaPlugin {
    private static FileConfiguration customConfig;
    private static FactionManager factionManager;
    private static ClaimManager claimManager;
    private static ShopManager shopManager;
    private static EffectManager effectManager;
    private static RivalsCommand command;
    private static ConfigurationSection settings;
    private static EventManager eventManager;
    private static ScoreboardManager scoreboardManager;
    private static Rivals plugin;
    private BukkitTask t;

    public static boolean changeSetting(String settingName, String settingValue) {
        try {
            switch (settingName) {
                case "minShopPower", "killEntityPower", "killNeutralPower", "killAllyPower", "killEnemyPower", "deathPowerLoss", "tradePower", "defaultPower", "nowWarPower", "votePassRatio", "warDelay" -> {
                    double value = Double.parseDouble(settingValue);
                    settings.set(settingName, value);
                    return true;
                }
                case "maxNameLength", "minVotes", "resourceDistance", "votePassTime" -> {
                    int value = Integer.parseInt(settingValue);
                    settings.set(settingName, value);
                    return true;
                }
                default -> {
                    settings.set(settingName, settingValue);
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            return false; // The provided settingValue could not be cast to the correct type.
        }
    }

    public static boolean validSetting(String settingName, String settingValue) {
        try {
            switch (settingName) {
                case "deathPowerLoss" -> {
                    if(Double.parseDouble(settingValue) > 0) {
                        return false;
                    }
                    return true;
                }
                case "killNeutralPower", "killAllyPower" -> {
                    Double.parseDouble(settingValue);
                    return true;
                }
                case "minShopPower", "killEnemyPower", "combatTeleportDelay", "killEntityPower", "tradePower", "defaultPower", "nowWarPower", "votePassRatio", "warDelay" -> {
                    if(Double.parseDouble(settingValue) < 0) {
                        return false;
                    }
                    return true;
                }
                case "maxNameLength", "minVotes", "resourceDistance", "votePassTime" -> {
                    if(Integer.parseInt(settingValue) < 0) {
                        return false;
                    }
                    return true;
                }
                default -> {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getLogger().log(Level.INFO, "[Rivals] Starting!");
        plugin = this;

        claimManager = new ClaimManager();
        effectManager = new EffectManager();
        scoreboardManager = new ScoreboardManager(Bukkit.getServer());


        registerClasses();
        createCustomConfig();
        createConfigs();

        registerListeners();
        registerCommands();

        t = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getLogger().log(Level.INFO, "[Rivals] Updating!");
                effectManager.update();
            }
        }.runTaskTimer(this, 0, 20L * 60L * 60L);//run once per hour



        //t = new Task();
        //t.runTaskTimer(this, 0, 72000);
    }
    @Override
    public void onDisable() {
        t.cancel();
        saveData();
        Bukkit.getLogger().log(Level.INFO, "[Rivals] Closing!");
    }

    public void saveData() {
        if(getConfig().getConfigurationSection("settings") == null) {
            getConfig().set("settings", settings);
        }
        getConfig().set("factionManager", factionManager);
        getConfig().set("shopManager", shopManager);
        //System.out.println(getConfig().get("data"));
        saveConfig();
    }

    public void readData() {
        if(getConfig().getConfigurationSection("settings") != null) {
            settings = (ConfigurationSection) getConfig().get("settings");
        } else {
            settings = new YamlConfiguration();
            Bukkit.getLogger().log(Level.INFO, "[Rivals] No existing settings, creating them.");
        }
        if(!settings.contains("maxNameLength"))
            settings.set("maxNameLength", 16);
        if(!settings.contains("combatTeleportDelay"))
            settings.set("combatTeleportDelay", 120.0);
        if(getConfig().get("factionManager") != null) {
            factionManager = (FactionManager) getConfig().get("factionManager", FactionManager.class);
        } else {
            factionManager = new FactionManager();
        }
        if(getConfig().get("shopManager") != null) {
            shopManager = (ShopManager) getConfig().get("shopManager", ShopManager.class);
        } else {
            shopManager = new ShopManager();
        }
        factionManager.buildFactionRanks();
    }

    public static ConfigurationSection getSettings() {
        return settings;
    }

    public void createCustomConfig() {
        File customConfigFile = new File(getDataFolder(), "config.yml");
        if(!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        customConfig = new YamlConfiguration();
        try{
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        readData();
    }

    public void createConfigs() {
        this.saveDefaultConfig();
        this.getConfig();
    }

    public void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        eventManager = new EventManager(effectManager);
        pm.registerEvents(eventManager, this);
    }

    public static Rivals getPlugin() {
        return plugin;
    }

    public static RivalsCommand getCommand() {
        return command;
    }

    public void registerCommands() {
        command = new RivalsCommand();
        this.getCommand("rivals").setExecutor(command);
        this.getCommand("rivalsadmin").setExecutor(new AdminCommand());
        this.getCommand("home").setExecutor(new HomeCommand());
        this.getCommand("sethome").setExecutor(new SetHomeCommand());
        this.getCommand("delHome").setExecutor(new DelHomeCommand());
        this.getCommand("homes").setExecutor(new HomesCommand());
        this.getCommand("rsb").setExecutor(scoreboardManager);
    }

    public void registerClasses() {
        ConfigurationSerialization.registerClass(Faction.class);
        ConfigurationSerialization.registerClass(FactionManager.class);
        ConfigurationSerialization.registerClass(FactionManager.MemberInvite.class);
        ConfigurationSerialization.registerClass(ShopManager.class);
    }

    public static FactionManager getFactionManager() {
        return factionManager;
    }

    public static ShopManager getShopManager() {
        return shopManager;
    }

    public static ClaimManager getClaimManager() {
        return claimManager;
    }

    public static EventManager getEventManager() {return eventManager;}


    public static EffectManager getEffectManager() {
        return effectManager;
    }

    public static ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public static double getRoundedDecimal(double value) {
        return Math.round(value * 100) / 100;
    }
}
