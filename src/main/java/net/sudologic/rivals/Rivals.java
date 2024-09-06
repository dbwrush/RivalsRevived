package net.sudologic.rivals;

import net.sudologic.rivals.commands.AdminCommand;
import net.sudologic.rivals.commands.PolicyCommand;
import net.sudologic.rivals.commands.RivalsCommand;
import net.sudologic.rivals.commands.home.DelHomeCommand;
import net.sudologic.rivals.commands.home.HomeCommand;
import net.sudologic.rivals.commands.home.HomesCommand;
import net.sudologic.rivals.commands.home.SetHomeCommand;
import net.sudologic.rivals.managers.*;
import net.sudologic.rivals.resources.ResourceManager;
import net.sudologic.rivals.resources.ResourceSpawner;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class Rivals extends JavaPlugin {
    private static FileConfiguration customConfig;
    private static FactionManager factionManager;
    private static ClaimManager claimManager;
    private static ShopManager shopManager;
    private static PoliticsManager politicsManager;
    private static EffectManager effectManager;
    private static RivalsCommand command;
    private static ConfigurationSection settings;
    private static EventManager eventManager;
    private static ResourceManager resourceManager;
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
        resourceManager = new ResourceManager();
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
                resourceManager.update();
                factionManager.startWars();
                politicsManager.update();
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
        getConfig().set("resourceManager", resourceManager);
        getConfig().set("politicsManager", politicsManager);

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
        if(!settings.contains("minShopPower"))
            settings.set("minShopPower", 10.0);
        if(!settings.contains("killEntityPower"))
            settings.set("killEntityPower", 0.0);
        if(!settings.contains("killMonsterPower"))
            settings.set("killMonsterPower", 1.0);
        if(!settings.contains("killEnemyPower"))
            settings.set("killEnemyPower", 3.0);
        if(!settings.contains("killNeutralPower"))
            settings.set("killNeutralPower", 0.0);
        if(!settings.contains("killAllyPower"))
            settings.set("killAllyPower", -3.0);
        if(!settings.contains("deathPowerLoss"))
            settings.set("deathPowerLoss", 4.0);
        if(!settings.contains("tradePower"))
            settings.set("tradePower", 1.0);
        if(!settings.contains("defaultPower"))
            settings.set("defaultPower", 3.0);
        if(!settings.contains("maxNameLength"))
            settings.set("maxNameLength", 16);
        if(!settings.contains("warDelay"))
            settings.set("warDelay", 48);//in hours
        if(!settings.contains("nowWarPower"))
            settings.set("nowWarPower", 20.0);
        if(!settings.contains("votePassRatio"))
            settings.set("votePassRatio", 0.5);
        if(!settings.contains("votePassTime"))
            settings.set("votePassTime", 24);
        if(!settings.contains("minVotes"))
            settings.set("minVotes", 3);
        if(!settings.contains("resourceDistance"))
            settings.set("resourceDistance", 10000);
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
        if(getConfig().get("politicsManager") != null) {
            politicsManager = (PoliticsManager) getConfig().get("politicsManager", PoliticsManager.class);
        } else {
            politicsManager = new PoliticsManager();
        }
        if(getConfig().get("resourceManager") != null) {
            resourceManager = (ResourceManager) getConfig().get("resourceManager", ResourceManager.class);
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

    public static int getResourceDistance() {
        return (int) settings.get("resourceDistance");
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
        this.getCommand("policy").setExecutor(new PolicyCommand());
        this.getCommand("rsb").setExecutor(scoreboardManager);
    }

    public void registerClasses() {
        ConfigurationSerialization.registerClass(Faction.class);
        ConfigurationSerialization.registerClass(FactionManager.class);
        ConfigurationSerialization.registerClass(FactionManager.MemberInvite.class);
        ConfigurationSerialization.registerClass(FactionManager.AllyInvite.class);
        ConfigurationSerialization.registerClass(FactionManager.PeaceInvite.class);
        ConfigurationSerialization.registerClass(ShopManager.class);
        ConfigurationSerialization.registerClass(PoliticsManager.class);
        ConfigurationSerialization.registerClass(FactionManager.WarDeclaration.class);
        ConfigurationSerialization.registerClass(ResourceSpawner.class);
        ConfigurationSerialization.registerClass(ResourceManager.class);
    }

    public static ResourceManager getResourceManager() {
        return resourceManager;
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

    public static PoliticsManager getPoliticsManager() {
        return politicsManager;
    }

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
