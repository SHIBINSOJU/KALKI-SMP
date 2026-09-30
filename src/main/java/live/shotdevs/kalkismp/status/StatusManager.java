package live.shotdevs.kalkismp.status;

import live.shotdevs.kalkismp.KalkiSMP;
import live.shotdevs.kalkismp.config.ConfigManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatusManager {

    private final KalkiSMP plugin;
    private final long startTimeMillis;
    private File dataFile;
    private YamlConfiguration dataConfig;
    private String statusMessageId;

    private BukkitTask updateTask;
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    public StatusManager(KalkiSMP plugin) {
        this.plugin = plugin;
        this.startTimeMillis = System.currentTimeMillis();
        loadDataFile();
    }

    private void loadDataFile() {
        this.dataFile = new File(plugin.getDataFolder(), "status_data.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("[Status] Could not create status_data.yml: " + e.getMessage());
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        this.statusMessageId = dataConfig.getString("status-message-id", null);
    }

    private synchronized void saveStatusMessageId(String id) {
        this.statusMessageId = id;
        dataConfig.set("status-message-id", id);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Status] Could not save status_data.yml: " + e.getMessage());
        }
    }

    public void start() {
        stopTask();

        ConfigManager config = plugin.getConfigManager();
        if (!config.isStatusEnabled() || config.getStatusChannelId().isBlank()) {
            plugin.getLogger().info("[Status] Status panel is disabled or channel ID is not configured.");
            return;
        }

        long intervalTicks = Math.max(5, config.getStatusUpdateInterval()) * 20L;
        this.updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateStatusAsync, 20L, intervalTicks);
        plugin.getLogger().info("[Status] Status panel updater started (interval: " + config.getStatusUpdateInterval() + "s).");
    }

    public void stopTask() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void shutdown() {
        stopTask();

        ConfigManager config = plugin.getConfigManager();
        if (!config.isStatusEnabled() || config.getStatusChannelId().isBlank()) {
            return;
        }

        if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isConnected()) {
            return;
        }

        if (statusMessageId == null || statusMessageId.isBlank()) {
            return;
        }

        try {
            JDA jda = plugin.getDiscordManager().getJda();
            TextChannel channel = jda.getTextChannelById(config.getStatusChannelId());
            if (channel != null && channel.canTalk()) {
                MessageEditData editData = StatusComponentBuilder.buildOfflineStatusEditMessage(config);
                channel.editMessageById(statusMessageId, editData).complete();
                plugin.getLogger().info("[Status] Status panel set to offline on shutdown.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Status] Could not update offline status panel: " + e.getMessage());
        }
    }

    public void updateStatusAsync() {
        if (!isUpdating.compareAndSet(false, true)) {
            return; // Skip if update already in progress
        }

        CompletableFuture.runAsync(() -> {
            try {
                ConfigManager config = plugin.getConfigManager();
                if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isConnected()) {
                    return;
                }

                String channelId = config.getStatusChannelId();
                if (channelId.isBlank()) return;

                JDA jda = plugin.getDiscordManager().getJda();
                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel == null || !channel.canTalk()) {
                    plugin.getLogger().warning("[Status] Cannot access status channel (ID: " + channelId + "). Check bot permissions.");
                    return;
                }

                // Gather real Paper metrics
                StatusComponentBuilder.ServerMetrics metrics = gatherMetrics();

                if (statusMessageId != null && !statusMessageId.isBlank()) {
                    MessageEditData editData = StatusComponentBuilder.buildStatusEditMessage(config, metrics);
                    channel.editMessageById(statusMessageId, editData).queue(
                            success -> {},
                            failure -> {
                                plugin.getLogger().warning("[Status] Saved status message was deleted or invalid (" + failure.getMessage() + "). Creating new message...");
                                createNewStatusMessage(channel, config, metrics);
                            }
                    );
                } else {
                    createNewStatusMessage(channel, config, metrics);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Status] Error updating status panel: " + e.getMessage());
            } finally {
                isUpdating.set(false);
            }
        });
    }

    private void createNewStatusMessage(TextChannel channel, ConfigManager config, StatusComponentBuilder.ServerMetrics metrics) {
        MessageCreateData createData = StatusComponentBuilder.buildStatusCreateMessage(config, metrics);
        channel.sendMessage(createData).queue(
                msg -> saveStatusMessageId(msg.getId()),
                err -> plugin.getLogger().warning("[Status] Failed to create status message: " + err.getMessage())
        );
    }

    public StatusComponentBuilder.ServerMetrics gatherMetrics() {
        boolean isOnline = Bukkit.isPrimaryThread() || Bukkit.getServer() != null;
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();

        double tps = -1.0;
        try {
            double[] tpsArray = Bukkit.getTPS();
            if (tpsArray != null && tpsArray.length > 0) {
                tps = Math.min(20.0, tpsArray[0]);
            }
        } catch (Exception ignored) {}

        double mspt = -1.0;
        try {
            mspt = Bukkit.getAverageTickTime();
        } catch (Exception ignored) {}

        String uptimeStr = formatUptime(System.currentTimeMillis() - startTimeMillis);
        String serverIp = plugin.getConfigManager().getStatusServerIp();

        return new StatusComponentBuilder.ServerMetrics(
                isOnline,
                onlinePlayers,
                maxPlayers,
                tps,
                mspt,
                uptimeStr,
                serverIp,
                System.currentTimeMillis()
        );
    }

    private String formatUptime(long durationMillis) {
        long seconds = durationMillis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}
