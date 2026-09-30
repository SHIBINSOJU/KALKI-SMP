package live.shotdevs.kalkismp;

import live.shotdevs.kalkismp.commands.KalkiCommand;
import live.shotdevs.kalkismp.config.ConfigManager;
import live.shotdevs.kalkismp.discord.DiscordManager;
import live.shotdevs.kalkismp.discord.DiscordMessageSender;
import live.shotdevs.kalkismp.listeners.ChatListener;
import live.shotdevs.kalkismp.listeners.DeathListener;
import live.shotdevs.kalkismp.listeners.JoinLeaveListener;
import live.shotdevs.kalkismp.logging.DiscordLogHandler;
import org.bukkit.plugin.java.JavaPlugin;

public final class KalkiSMP extends JavaPlugin {

    private static KalkiSMP instance;
    private ConfigManager configManager;
    private DiscordManager discordManager;
    private DiscordLogHandler logHandler;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("========================================");
        getLogger().info(" Enabling KalkiSMP - Kalki SMP S2");
        getLogger().info("========================================");

        // Load configuration
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigurations();

        // Initialize Discord Manager
        this.discordManager = new DiscordManager(this);
        this.discordManager.startAsync();

        // Register Event Listeners
        DiscordMessageSender messageSender = new DiscordMessageSender(this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, messageSender), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this, messageSender), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this, messageSender), this);

        // Register Console Log Handler
        this.logHandler = new DiscordLogHandler(this);
        this.logHandler.register();

        // Register Command
        KalkiCommand kalkiCommand = new KalkiCommand(this);
        if (getCommand("kalkismp") != null) {
            getCommand("kalkismp").setExecutor(kalkiCommand);
            getCommand("kalkismp").setTabCompleter(kalkiCommand);
        }

        getLogger().info("KalkiSMP core initialized successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling KalkiSMP...");
        if (logHandler != null) {
            logHandler.unregister();
        }
        if (discordManager != null) {
            discordManager.shutdown();
        }
        instance = null;
    }

    public static KalkiSMP getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }
}
