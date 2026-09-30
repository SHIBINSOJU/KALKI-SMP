package live.shotdevs.kalkismp.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import live.shotdevs.kalkismp.KalkiSMP;
import live.shotdevs.kalkismp.discord.DiscordMessageSender;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final KalkiSMP plugin;
    private final DiscordMessageSender messageSender;

    public ChatListener(KalkiSMP plugin, DiscordMessageSender messageSender) {
        this.plugin = plugin;
        this.messageSender = messageSender;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!plugin.getConfigManager().isMcToDiscordChatEnabled()) {
            return;
        }

        String playerName = event.getPlayer().getName();
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (plainMessage.isEmpty() || plainMessage.startsWith("/")) {
            return;
        }

        // Send to Discord asynchronously
        messageSender.sendPlayerChat(playerName, plainMessage);
    }
}
