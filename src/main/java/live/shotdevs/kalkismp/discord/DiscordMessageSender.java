package live.shotdevs.kalkismp.discord;

import live.shotdevs.kalkismp.KalkiSMP;
import java.awt.Color;

public class DiscordMessageSender {

    private final KalkiSMP plugin;

    public DiscordMessageSender(KalkiSMP plugin) {
        this.plugin = plugin;
    }

    private DiscordManager getManager() {
        return plugin.getDiscordManager();
    }

    public void sendPlayerChat(String player, String message) {
        if (getManager() != null) {
            getManager().sendChatMessageToDiscord(player, message);
        }
    }

    public void sendJoinEvent(String player) {
        if (getManager() == null || !plugin.getConfigManager().isJoinMessagesEnabled()) return;
        String template = plugin.getConfigManager().getDiscordJoinMessage();
        String message = template.replace("{player}", player);
        getManager().sendEventMessage(message, new Color(0x5865F2)); // Discord Blurple
    }

    public void sendLeaveEvent(String player) {
        if (getManager() == null || !plugin.getConfigManager().isLeaveMessagesEnabled()) return;
        String template = plugin.getConfigManager().getDiscordLeaveMessage();
        String message = template.replace("{player}", player);
        getManager().sendEventMessage(message, new Color(0xFEE75C)); // Yellow
    }

    public void sendDeathEvent(String deathMessage) {
        if (getManager() == null || !plugin.getConfigManager().isDeathMessagesEnabled()) return;
        String template = plugin.getConfigManager().getDiscordDeathMessage();
        String message = template.replace("{death_message}", deathMessage);
        getManager().sendEventMessage(message, new Color(0xED4245)); // Red
    }
}
