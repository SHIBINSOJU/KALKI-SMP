package live.shotdevs.kalkismp.commands;

import live.shotdevs.kalkismp.KalkiSMP;
import live.shotdevs.kalkismp.config.ConfigManager;
import live.shotdevs.kalkismp.discord.DiscordManager;
import live.shotdevs.kalkismp.util.MessageFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class KalkiCommand implements CommandExecutor, TabCompleter {

    private final KalkiSMP plugin;

    public KalkiCommand(KalkiSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager config = plugin.getConfigManager();

        // Permission check
        if (!sender.hasPermission("kalkismp.admin")) {
            sender.sendMessage(MessageFormatter.formatComponent(config.getMcPrefix() + config.getMcNoPermission()));
            return true;
        }

        if (args.length == 0) {
            sendPluginInfo(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "status" -> sendPluginStatus(sender);
            case "reload" -> handleReload(sender);
            default -> sendPluginInfo(sender);
        }

        return true;
    }

    private void sendPluginInfo(CommandSender sender) {
        ConfigManager config = plugin.getConfigManager();
        sender.sendMessage(MessageFormatter.formatComponent("&8&m----------------------------------------"));
        sender.sendMessage(MessageFormatter.formatComponent("&b&l" + config.getBranding() + " &7- Plugin Info"));
        sender.sendMessage(MessageFormatter.formatComponent("&7Version: &a" + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(MessageFormatter.formatComponent("&7Authors: &fshotdevs"));
        sender.sendMessage(MessageFormatter.formatComponent("&7Use &b/kalkismp status &7or &b/kalkismp reload"));
        sender.sendMessage(MessageFormatter.formatComponent("&8&m----------------------------------------"));
    }

    private void sendPluginStatus(CommandSender sender) {
        ConfigManager config = plugin.getConfigManager();
        DiscordManager discord = plugin.getDiscordManager();

        boolean isDiscordConnected = discord != null && discord.isConnected();
        String discordStatusStr = !config.isDiscordEnabled() ? "&cDisabled"
                : (isDiscordConnected ? "&aCONNECTED" : "&eDISCONNECTED / INITIALIZING");

        sender.sendMessage(MessageFormatter.formatComponent("&8&m----------------------------------------"));
        sender.sendMessage(MessageFormatter.formatComponent("&b&l" + config.getBranding() + " &7- Status"));
        sender.sendMessage(MessageFormatter.formatComponent("&7Discord Status: " + discordStatusStr));
        sender.sendMessage(MessageFormatter.formatComponent("&7Chat Channel ID: &f" + (config.getChatChannelId().isBlank() ? "&cNot Set" : config.getChatChannelId())));
        sender.sendMessage(MessageFormatter.formatComponent("&7Events Channel ID: &f" + (config.getEventsChannelId().isBlank() ? "&cNot Set" : config.getEventsChannelId())));
        sender.sendMessage(MessageFormatter.formatComponent("&7Console Channel ID: &f" + (config.getConsoleChannelId().isBlank() ? "&cNot Set" : config.getConsoleChannelId())));
        sender.sendMessage(MessageFormatter.formatComponent("&7Features:"));
        sender.sendMessage(MessageFormatter.formatComponent("  &7MC->Discord Chat: " + formatBool(config.isMcToDiscordChatEnabled())));
        sender.sendMessage(MessageFormatter.formatComponent("  &7Discord->MC Chat: " + formatBool(config.isDiscordToMcChatEnabled())));
        sender.sendMessage(MessageFormatter.formatComponent("  &7Join Messages: " + formatBool(config.isJoinMessagesEnabled())));
        sender.sendMessage(MessageFormatter.formatComponent("  &7Leave Messages: " + formatBool(config.isLeaveMessagesEnabled())));
        sender.sendMessage(MessageFormatter.formatComponent("  &7Death Messages: " + formatBool(config.isDeathMessagesEnabled())));
        sender.sendMessage(MessageFormatter.formatComponent("  &7Console Logging: " + formatBool(config.isConsoleLoggingEnabled())));
        sender.sendMessage(MessageFormatter.formatComponent("&8&m----------------------------------------"));
    }

    private void handleReload(CommandSender sender) {
        ConfigManager config = plugin.getConfigManager();
        sender.sendMessage(MessageFormatter.formatComponent(config.getMcPrefix() + "&eReloading configuration..."));

        // Save old state
        boolean oldDiscordEnabled = config.isDiscordEnabled();
        String oldToken = config.getBotToken();

        // Reload configs
        config.loadConfigurations();

        // Check if Discord token or enabled state changed, safely restart JDA if needed
        DiscordManager discord = plugin.getDiscordManager();
        if (discord != null) {
            if (!config.isDiscordEnabled() && oldDiscordEnabled) {
                discord.shutdown();
                sender.sendMessage(MessageFormatter.formatComponent(config.getMcPrefix() + "&cDiscord integration disabled. Bot shut down."));
            } else if (config.isDiscordEnabled() && (!oldDiscordEnabled || !oldToken.equals(config.getBotToken()) || !discord.isConnected())) {
                discord.shutdown();
                discord.startAsync();
                sender.sendMessage(MessageFormatter.formatComponent(config.getMcPrefix() + "&aReconnecting Discord bot..."));
            }
        }

        sender.sendMessage(MessageFormatter.formatComponent(config.getMcPrefix() + config.getMcReloaded()));
    }

    private String formatBool(boolean val) {
        return val ? "&aEnabled" : "&cDisabled";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            if ("status".startsWith(args[0].toLowerCase())) suggestions.add("status");
            if ("reload".startsWith(args[0].toLowerCase())) suggestions.add("reload");
            return suggestions;
        }
        return List.of();
    }
}
