package live.shotdevs.kalkismp.config;

import live.shotdevs.kalkismp.KalkiSMP;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final KalkiSMP plugin;

    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    // Discord Settings
    private boolean discordEnabled;
    private String botToken;
    private String chatChannelId;
    private String eventsChannelId;
    private String consoleChannelId;

    // Feature Toggles
    private boolean mcToDiscordChatEnabled;
    private boolean discordToMcChatEnabled;
    private boolean joinMessagesEnabled;
    private boolean leaveMessagesEnabled;
    private boolean deathMessagesEnabled;
    private boolean serverStartEnabled;
    private boolean serverStopEnabled;
    private boolean consoleLoggingEnabled;

    // Status Panel Settings
    private boolean statusEnabled;
    private String statusChannelId;
    private long statusUpdateInterval;
    private String statusServerIp;
    private boolean statusShowPlayers;
    private boolean statusShowTps;
    private boolean statusShowMspt;
    private boolean statusShowUptime;
    private boolean statusShowIp;

    // Console Filter Settings
    private long consoleFlushIntervalSeconds;
    private int consoleMaxBufferLength;
    private String consoleMinLogLevel;
    private List<String> consoleIgnoredPhrases;

    // Messages & Branding
    private String branding;
    private String mcDiscordChatFormat;
    private String mcPrefix;
    private String mcNoPermission;
    private String mcReloaded;
    private String discordMcChatFormat;
    private String discordJoinMessage;
    private String discordLeaveMessage;
    private String discordDeathMessage;
    private String discordServerOnline;
    private String discordServerOffline;

    private boolean configValid = true;

    public ConfigManager(KalkiSMP plugin) {
        this.plugin = plugin;
    }

    public void loadConfigurations() {
        // Save default config.yml if not exists
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Load messages.yml
        loadMessagesFile();

        // Parse Discord settings
        this.discordEnabled = config.getBoolean("discord.enabled", true);

        // Check environment variable first for bot token
        String envToken = System.getenv("KALKI_DISCORD_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            this.botToken = envToken.trim();
        } else {
            this.botToken = config.getString("discord.token", "").trim();
        }

        this.chatChannelId = config.getString("discord.channels.chat", "").trim();
        this.eventsChannelId = config.getString("discord.channels.events", "").trim();
        this.consoleChannelId = config.getString("discord.channels.console", "").trim();

        // Parse features
        this.mcToDiscordChatEnabled = config.getBoolean("features.minecraft-to-discord-chat", true);
        this.discordToMcChatEnabled = config.getBoolean("features.discord-to-minecraft-chat", true);
        this.joinMessagesEnabled = config.getBoolean("features.join-messages", true);
        this.leaveMessagesEnabled = config.getBoolean("features.leave-messages", true);
        this.deathMessagesEnabled = config.getBoolean("features.death-messages", true);
        this.serverStartEnabled = config.getBoolean("features.server-start", true);
        this.serverStopEnabled = config.getBoolean("features.server-stop", true);
        this.consoleLoggingEnabled = config.getBoolean("features.console-logging", true);

        // Status settings
        this.statusEnabled = config.getBoolean("status.enabled", true);
        this.statusChannelId = config.getString("status.channel", "").trim();
        this.statusUpdateInterval = config.getLong("status.update-interval", 30L);
        this.statusServerIp = config.getString("status.server-ip", "play.kalkismp.com").trim();
        this.statusShowPlayers = config.getBoolean("status.show.players", true);
        this.statusShowTps = config.getBoolean("status.show.tps", true);
        this.statusShowMspt = config.getBoolean("status.show.mspt", true);
        this.statusShowUptime = config.getBoolean("status.show.uptime", true);
        this.statusShowIp = config.getBoolean("status.show.ip", true);

        // Console filters
        this.consoleFlushIntervalSeconds = config.getLong("console-filter.flush-interval-seconds", 2L);
        this.consoleMaxBufferLength = config.getInt("console-filter.max-buffer-length", 1800);
        this.consoleMinLogLevel = config.getString("console-filter.min-log-level", "INFO");
        this.consoleIgnoredPhrases = config.getStringList("console-filter.ignored-phrases");
        if (this.consoleIgnoredPhrases == null) {
            this.consoleIgnoredPhrases = new ArrayList<>();
        }

        // Messages & Branding
        this.branding = messagesConfig.getString("branding", "KALKI SMP S2");
        this.mcDiscordChatFormat = messagesConfig.getString("minecraft.discord-chat-format", "&8[&9Discord&8] &b{user}&7: &f{message}");
        this.mcPrefix = messagesConfig.getString("minecraft.prefix", "&8[&bKalkiSMP&8] ");
        this.mcNoPermission = messagesConfig.getString("minecraft.no-permission", "&cYou do not have permission to use this command.");
        this.mcReloaded = messagesConfig.getString("minecraft.reloaded", "&aConfiguration reloaded successfully!");

        this.discordMcChatFormat = messagesConfig.getString("discord.minecraft-chat-format", "**{player}**: {message}");
        this.discordJoinMessage = messagesConfig.getString("discord.join-message", "⚔ **{player}** joined the server");
        this.discordLeaveMessage = messagesConfig.getString("discord.leave-message", "👋 **{player}** left the server");
        this.discordDeathMessage = messagesConfig.getString("discord.death-message", "☠ {death_message}");
        this.discordServerOnline = messagesConfig.getString("discord.server-online", "🟢 Server Online");
        this.discordServerOffline = messagesConfig.getString("discord.server-offline", "🔴 Server Offline");

        validateConfiguration();
    }

    private void loadMessagesFile() {
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultMessagesStream = plugin.getResource("messages.yml");
        if (defaultMessagesStream != null) {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultMessagesStream, StandardCharsets.UTF_8));
            this.messagesConfig.setDefaults(defaultMessages);
        }
    }

    private void validateConfiguration() {
        this.configValid = true;
        if (discordEnabled) {
            if (botToken.isBlank() || botToken.equals("YOUR_BOT_TOKEN_HERE")) {
                plugin.getLogger().warning("[Config] Discord bot token is missing or default! Set KALKI_DISCORD_TOKEN or config.yml -> discord.token.");
                this.configValid = false;
            }
        }
    }

    // Getters
    public boolean isDiscordEnabled() { return discordEnabled; }
    public String getBotToken() { return botToken; }
    public String getChatChannelId() { return chatChannelId; }
    public String getEventsChannelId() { return eventsChannelId; }
    public String getConsoleChannelId() { return consoleChannelId; }

    public boolean isMcToDiscordChatEnabled() { return mcToDiscordChatEnabled; }
    public boolean isDiscordToMcChatEnabled() { return discordToMcChatEnabled; }
    public boolean isJoinMessagesEnabled() { return joinMessagesEnabled; }
    public boolean isLeaveMessagesEnabled() { return leaveMessagesEnabled; }
    public boolean isDeathMessagesEnabled() { return deathMessagesEnabled; }
    public boolean isServerStartEnabled() { return serverStartEnabled; }
    public boolean isServerStopEnabled() { return serverStopEnabled; }
    public boolean isConsoleLoggingEnabled() { return consoleLoggingEnabled; }

    public boolean isStatusEnabled() { return statusEnabled; }
    public String getStatusChannelId() { return statusChannelId; }
    public long getStatusUpdateInterval() { return statusUpdateInterval; }
    public String getStatusServerIp() { return statusServerIp; }
    public boolean isStatusShowPlayers() { return statusShowPlayers; }
    public boolean isStatusShowTps() { return statusShowTps; }
    public boolean isStatusShowMspt() { return statusShowMspt; }
    public boolean isStatusShowUptime() { return statusShowUptime; }
    public boolean isStatusShowIp() { return statusShowIp; }

    public long getConsoleFlushIntervalSeconds() { return consoleFlushIntervalSeconds; }
    public int getConsoleMaxBufferLength() { return consoleMaxBufferLength; }
    public String getConsoleMinLogLevel() { return consoleMinLogLevel; }
    public List<String> getConsoleIgnoredPhrases() { return consoleIgnoredPhrases; }

    public String getBranding() { return branding; }
    public String getMcDiscordChatFormat() { return mcDiscordChatFormat; }
    public String getMcPrefix() { return mcPrefix; }
    public String getMcNoPermission() { return mcNoPermission; }
    public String getMcReloaded() { return mcReloaded; }
    public String getDiscordMcChatFormat() { return discordMcChatFormat; }
    public String getDiscordJoinMessage() { return discordJoinMessage; }
    public String getDiscordLeaveMessage() { return discordLeaveMessage; }
    public String getDiscordDeathMessage() { return discordDeathMessage; }
    public String getDiscordServerOnline() { return discordServerOnline; }
    public String getDiscordServerOffline() { return discordServerOffline; }

    public boolean isConfigValid() { return configValid; }
}
