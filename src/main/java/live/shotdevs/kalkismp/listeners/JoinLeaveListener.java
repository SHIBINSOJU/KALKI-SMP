package live.shotdevs.kalkismp.listeners;

import live.shotdevs.kalkismp.KalkiSMP;
import live.shotdevs.kalkismp.discord.DiscordMessageSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {

    private final KalkiSMP plugin;
    private final DiscordMessageSender messageSender;

    public JoinLeaveListener(KalkiSMP plugin, DiscordMessageSender messageSender) {
        this.plugin = plugin;
        this.messageSender = messageSender;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        messageSender.sendJoinEvent(event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        messageSender.sendLeaveEvent(event.getPlayer().getName());
    }
}
