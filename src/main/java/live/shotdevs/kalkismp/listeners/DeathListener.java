package live.shotdevs.kalkismp.listeners;

import live.shotdevs.kalkismp.KalkiSMP;
import live.shotdevs.kalkismp.discord.DiscordMessageSender;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

    private final KalkiSMP plugin;
    private final DiscordMessageSender messageSender;

    public DeathListener(KalkiSMP plugin, DiscordMessageSender messageSender) {
        this.plugin = plugin;
        this.messageSender = messageSender;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        String deathMessage = null;
        if (event.deathMessage() != null) {
            deathMessage = PlainTextComponentSerializer.plainText().serialize(event.deathMessage());
        }

        if (deathMessage == null || deathMessage.isBlank()) {
            deathMessage = event.getEntity().getName() + " died";
        }

        messageSender.sendDeathEvent(deathMessage);
    }
}
