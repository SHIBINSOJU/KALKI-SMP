package live.shotdevs.kalkismp.logging;

import live.shotdevs.kalkismp.KalkiSMP;
import live.shotdevs.kalkismp.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class DiscordLogHandler extends Handler {

    private final KalkiSMP plugin;
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private BukkitTask flushTask;
    private boolean registered = false;

    public DiscordLogHandler(KalkiSMP plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (registered) return;

        ConfigManager config = plugin.getConfigManager();
        if (!config.isConsoleLoggingEnabled() || config.getConsoleChannelId().isBlank()) {
            return;
        }

        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(this);
        this.registered = true;

        // Schedule periodic flushing every N seconds
        long intervalTicks = Math.max(1, config.getConsoleFlushIntervalSeconds()) * 20L;
        this.flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushLogsToDiscord, intervalTicks, intervalTicks);
        plugin.getLogger().info("[ConsoleLog] Discord log handler registered successfully.");
    }

    public void unregister() {
        if (registered) {
            Logger rootLogger = Logger.getLogger("");
            rootLogger.removeHandler(this);
            registered = false;
        }

        if (flushTask != null && !flushTask.isCancelled()) {
            flushTask.cancel();
            flushTask = null;
        }

        // Flush remaining logs before complete shutdown
        flushLogsToDiscord();
    }

    @Override
    public void publish(LogRecord record) {
        if (!registered || record == null) return;

        ConfigManager config = plugin.getConfigManager();
        if (!config.isConsoleLoggingEnabled()) return;

        // 1. Level check
        if (record.getLevel().intValue() < getLevelThreshold(config.getConsoleMinLogLevel()).intValue()) {
            return;
        }

        // 2. Logger name filter (Prevent recursion and JDA internal spam)
        String loggerName = record.getLoggerName() != null ? record.getLoggerName() : "";
        if (loggerName.contains("net.dv8tion.jda") ||
            loggerName.contains("okhttp3") ||
            loggerName.contains("live.shotdevs.kalkismp.discord") ||
            loggerName.contains("live.shotdevs.kalkismp.logging")) {
            return;
        }

        // Format log line
        String rawMessage = record.getMessage();
        if (rawMessage == null) rawMessage = "";

        // Format exceptions if present
        if (record.getThrown() != null) {
            rawMessage += " (" + record.getThrown().getClass().getSimpleName() + ": " + record.getThrown().getMessage() + ")";
        }

        // 3. Ignored phrases filter (Credentials / Loops)
        String botToken = config.getBotToken();
        if (!botToken.isBlank() && rawMessage.contains(botToken)) {
            rawMessage = rawMessage.replace(botToken, "[REDACTED_TOKEN]");
        }

        for (String phrase : config.getConsoleIgnoredPhrases()) {
            if (!phrase.isBlank() && rawMessage.toLowerCase().contains(phrase.toLowerCase())) {
                return;
            }
        }

        // Clean up formatting
        String logLine = "[" + record.getLevel().getName() + "] " + rawMessage;
        logQueue.add(logLine);
    }

    private void flushLogsToDiscord() {
        if (logQueue.isEmpty()) return;

        if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isConnected()) {
            logQueue.clear();
            return;
        }

        ConfigManager config = plugin.getConfigManager();
        String consoleChannelId = config.getConsoleChannelId();
        if (consoleChannelId.isBlank()) {
            logQueue.clear();
            return;
        }

        List<String> lines = new ArrayList<>();
        String line;
        while ((line = logQueue.poll()) != null) {
            lines.add(line);
        }

        if (lines.isEmpty()) return;

        // Group into code block chunks of max ~1800 chars
        StringBuilder currentBlock = new StringBuilder("```logs\n");
        int maxLen = Math.min(1800, config.getConsoleMaxBufferLength());

        for (String l : lines) {
            if (currentBlock.length() + l.length() + 10 > maxLen) {
                currentBlock.append("```");
                plugin.getDiscordManager().sendMessage(consoleChannelId, currentBlock.toString());
                currentBlock = new StringBuilder("```logs\n");
            }
            currentBlock.append(l).append("\n");
        }

        if (currentBlock.length() > 8) { // More than just "```logs\n"
            currentBlock.append("```");
            plugin.getDiscordManager().sendMessage(consoleChannelId, currentBlock.toString());
        }
    }

    private Level getLevelThreshold(String levelName) {
        try {
            return Level.parse(levelName.toUpperCase());
        } catch (Exception e) {
            return Level.INFO;
        }
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {
        unregister();
    }
}
