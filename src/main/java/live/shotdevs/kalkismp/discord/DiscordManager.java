package live.shotdevs.kalkismp.discord;

import live.shotdevs.kalkismp.KalkiSMP;
import live.shotdevs.kalkismp.config.ConfigManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.awt.Color;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DiscordManager {

    private final KalkiSMP plugin;
    private JDA jda;
    private DiscordListener discordListener;
    private boolean connected = false;

    public DiscordManager(KalkiSMP plugin) {
        this.plugin = plugin;
    }

    public void startAsync() {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isDiscordEnabled()) {
            plugin.getLogger().info("[Discord] Integration is disabled in config.yml.");
            return;
        }

        if (!config.isConfigValid()) {
            plugin.getLogger().warning("[Discord] Skipping connection due to invalid configuration (missing token or channels).");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                plugin.getLogger().info("[Discord] Connecting to Discord...");
                this.discordListener = new DiscordListener(plugin);

                this.jda = JDABuilder.createDefault(config.getBotToken())
                        .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                        .setMemberCachePolicy(MemberCachePolicy.NONE)
                        .setChunkingFilter(ChunkingFilter.NONE)
                        .setActivity(Activity.playing("Kalki SMP S2"))
                        .addEventListeners(discordListener)
                        .build();

                this.jda.awaitReady();
                this.connected = true;
                plugin.getLogger().info("[Discord] Bot connected successfully as " + jda.getSelfUser().getAsTag() + "!");

                // Send server start notification if enabled
                if (config.isServerStartEnabled()) {
                    sendServerStartNotification();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                plugin.getLogger().warning("[Discord] Connection thread interrupted during startup.");
            } catch (Exception e) {
                this.connected = false;
                plugin.getLogger().severe("[Discord] Failed to connect to Discord: " + e.getMessage());
                plugin.getLogger().severe("[Discord] Disabling Discord features until reloaded with a valid token.");
            }
        });
    }

    public void shutdown() {
        if (jda != null) {
            try {
                plugin.getLogger().info("[Discord] Shutting down Discord bot...");
                if (connected && plugin.getConfigManager().isServerStopEnabled()) {
                    sendServerStopNotificationSync();
                }

                this.connected = false;
                jda.shutdown();
                if (!jda.awaitShutdown(Duration.ofSeconds(5))) {
                    plugin.getLogger().warning("[Discord] Force shutting down JDA due to timeout...");
                    jda.shutdownNow();
                }
                plugin.getLogger().info("[Discord] Bot shut down completely.");
            } catch (Exception e) {
                plugin.getLogger().warning("[Discord] Error shutting down JDA: " + e.getMessage());
            } finally {
                this.jda = null;
            }
        }
    }

    public boolean isConnected() {
        return connected && jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }

    public JDA getJda() {
        return jda;
    }

    /**
     * Sends plain message to a specific channel ID.
     */
    public void sendMessage(String channelId, String content) {
        if (!isConnected() || channelId == null || channelId.isBlank()) return;
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null && channel.canTalk()) {
                channel.sendMessage(content).queue(
                        null,
                        throwable -> plugin.getLogger().warning("[Discord] Failed to send message to channel " + channelId + ": " + throwable.getMessage())
                );
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Discord] Error sending message: " + e.getMessage());
        }
    }

    /**
     * Sends a Minecraft chat message to the configured Discord chat channel.
     */
    public void sendChatMessageToDiscord(String player, String message) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isMcToDiscordChatEnabled()) return;

        String channelId = config.getChatChannelId();
        if (channelId.isBlank()) return;

        String formatted = config.getDiscordMcChatFormat()
                .replace("{player}", player)
                .replace("{message}", message);

        sendMessage(channelId, formatted);
    }

    /**
     * Sends an event message (join, leave, death) to the events channel (or fallback to chat channel).
     */
    public void sendEventMessage(String rawMessage, Color color) {
        ConfigManager config = plugin.getConfigManager();
        String channelId = config.getEventsChannelId();
        if (channelId.isBlank()) {
            channelId = config.getChatChannelId();
        }
        if (channelId.isBlank()) return;

        // Clean container style embed with branding header
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor(config.getBranding(), null, null);
        embed.setDescription(rawMessage);
        embed.setColor(color != null ? color : new Color(0x2B2D31));

        sendEmbedMessage(channelId, embed);
    }

    /**
     * Sends an embed message to a specified channel.
     */
    public void sendEmbedMessage(String channelId, EmbedBuilder embed) {
        if (!isConnected() || channelId == null || channelId.isBlank()) return;
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null && channel.canTalk()) {
                channel.sendMessageEmbeds(embed.build()).queue(
                        null,
                        throwable -> plugin.getLogger().warning("[Discord] Failed to send embed to channel " + channelId + ": " + throwable.getMessage())
                );
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Discord] Error sending embed: " + e.getMessage());
        }
    }

    /**
     * Sends server online notification.
     */
    private void sendServerStartNotification() {
        ConfigManager config = plugin.getConfigManager();
        String channelId = config.getEventsChannelId();
        if (channelId.isBlank()) channelId = config.getChatChannelId();
        if (channelId.isBlank()) return;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor(config.getBranding(), null, null);
        embed.setDescription(config.getDiscordServerOnline());
        embed.setColor(new Color(0x57F287)); // Green

        sendEmbedMessage(channelId, embed);
    }

    /**
     * Sends server offline notification synchronously during server shutdown.
     */
    private void sendServerStopNotificationSync() {
        ConfigManager config = plugin.getConfigManager();
        String channelId = config.getEventsChannelId();
        if (channelId.isBlank()) channelId = config.getChatChannelId();
        if (channelId.isBlank()) return;

        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null && channel.canTalk()) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setAuthor(config.getBranding(), null, null);
                embed.setDescription(config.getDiscordServerOffline());
                embed.setColor(new Color(0xED4245)); // Red

                channel.sendMessageEmbeds(embed.build()).complete();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Discord] Could not send shutdown notification: " + e.getMessage());
        }
    }
}
