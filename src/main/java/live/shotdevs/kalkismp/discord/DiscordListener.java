package live.shotdevs.kalkismp.discord;

import live.shotdevs.kalkismp.KalkiSMP;
import live.shotdevs.kalkismp.config.ConfigManager;
import live.shotdevs.kalkismp.util.MessageFormatter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public class DiscordListener extends ListenerAdapter {

    private final KalkiSMP plugin;

    public DiscordListener(KalkiSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        ConfigManager config = plugin.getConfigManager();

        // 1. Ignore bot users and webhooks
        if (event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }

        // 2. Check if Discord to MC feature is enabled
        if (!config.isDiscordToMcChatEnabled()) {
            return;
        }

        // 3. Ensure message is from the configured chat channel
        String targetChannel = config.getChatChannelId();
        if (targetChannel.isBlank() || !event.getChannel().getId().equals(targetChannel)) {
            return;
        }

        // 4. Extract message content and author name
        String messageText = event.getMessage().getContentDisplay().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Display name (Guild nickname if set, otherwise username)
        String senderName = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();

        // Sanitize mentions to prevent unwanted pings or broken formatting
        String sanitizedMessage = MessageFormatter.sanitizeDiscordMentions(messageText);

        // Format MC message using configured template
        String formattedString = config.getMcDiscordChatFormat()
                .replace("{user}", senderName)
                .replace("{message}", sanitizedMessage);

        Component component = MessageFormatter.formatComponent(formattedString);

        // 5. Broadcast safely on Minecraft main server thread
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcast(component));
    }
}
