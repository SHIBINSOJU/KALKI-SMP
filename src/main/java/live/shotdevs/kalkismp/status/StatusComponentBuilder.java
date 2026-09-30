package live.shotdevs.kalkismp.status;

import live.shotdevs.kalkismp.config.ConfigManager;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StatusComponentBuilder {

    public static final String BTN_PLAYERS = "kalki_status_players";
    public static final String BTN_REFRESH = "kalki_status_refresh";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault());

    public record ServerMetrics(
            boolean isOnline,
            int onlinePlayers,
            int maxPlayers,
            double tps,
            double mspt,
            String uptimeStr,
            String serverIp,
            long lastUpdatedMillis
    ) {}

    public static MessageCreateData buildStatusCreateMessage(ConfigManager config, ServerMetrics metrics) {
        String content = buildStatusContentText(config, metrics);
        ActionRow actionRow = ActionRow.of(
                Button.primary(BTN_PLAYERS, "👥 Players"),
                Button.secondary(BTN_REFRESH, "🔄 Refresh")
        );

        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(actionRow)
                .build();
    }

    public static MessageEditData buildStatusEditMessage(ConfigManager config, ServerMetrics metrics) {
        String content = buildStatusContentText(config, metrics);
        ActionRow actionRow = ActionRow.of(
                Button.primary(BTN_PLAYERS, "👥 Players"),
                Button.secondary(BTN_REFRESH, "🔄 Refresh")
        );

        return new MessageEditBuilder()
                .setContent(content)
                .setComponents(actionRow)
                .build();
    }

    public static MessageEditData buildOfflineStatusEditMessage(ConfigManager config) {
        String timeStr = TIME_FORMATTER.format(Instant.now());
        String content = ">>> ### 🔴 " + config.getBranding() + "\n\n" +
                "🔴 **Server Offline**\n\n" +
                "The Minecraft server is currently offline.\n\n" +
                "───────────────────────────────────\n" +
                "Last online: " + timeStr;

        ActionRow actionRow = ActionRow.of(
                Button.secondary(BTN_REFRESH, "🔄 Refresh")
        );

        return new MessageEditBuilder()
                .setContent(content)
                .setComponents(actionRow)
                .build();
    }

    private static String buildStatusContentText(ConfigManager config, ServerMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append(">>> ### 🟢 ").append(config.getBranding()).append("\n\n");

        if (metrics.isOnline()) {
            sb.append("🟢 **Server Online**\n\n");
        } else {
            sb.append("🔴 **Server Offline**\n\n");
        }

        if (config.isStatusShowPlayers()) {
            sb.append("👥 **Players:** `").append(metrics.onlinePlayers()).append(" / ").append(metrics.maxPlayers()).append("`\n");
        }
        if (config.isStatusShowTps()) {
            String tpsStr = metrics.tps() >= 0 ? String.format("%.1f", metrics.tps()) : "N/A";
            sb.append("⚡ **TPS:** `").append(tpsStr).append("`\n");
        }
        if (config.isStatusShowMspt()) {
            String msptStr = metrics.mspt() >= 0 ? String.format("%.1f ms", metrics.mspt()) : "N/A";
            sb.append("📊 **MSPT:** `").append(msptStr).append("`\n");
        }
        if (config.isStatusShowUptime()) {
            sb.append("⏱️ **Uptime:** `").append(metrics.uptimeStr() != null ? metrics.uptimeStr() : "N/A").append("`\n");
        }
        if (config.isStatusShowIp()) {
            sb.append("🌐 **Server:** `").append(config.getStatusServerIp()).append("`\n");
        }

        sb.append("\n───────────────────────────────────\n");
        String timeStr = TIME_FORMATTER.format(Instant.ofEpochMilli(metrics.lastUpdatedMillis()));
        sb.append("Last updated: ").append(timeStr);

        return sb.toString();
    }

    public static MessageCreateData buildPlayerListMessage(ConfigManager config, String userId, List<PlayerListFormatter.FormattedPlayer> players, int page, int pageSize) {
        int totalPlayers = players.size();
        int totalPages = totalPlayers == 0 ? 1 : (int) Math.ceil((double) totalPlayers / pageSize);
        int currentPage = Math.max(1, Math.min(page, totalPages));

        String playerText = PlayerListFormatter.buildPlayerListText(players, currentPage, pageSize);

        StringBuilder sb = new StringBuilder();
        sb.append(">>> ### 👥 ").append(config.getBranding()).append(" — Players\n\n");
        sb.append(playerText).append("\n\n");
        sb.append("───────────────────────────────────\n");
        sb.append("**").append(totalPlayers).append("** player").append(totalPlayers == 1 ? "" : "s").append(" online");
        if (totalPages > 1) {
            sb.append(" • Page ").append(currentPage).append(" / ").append(totalPages);
        }

        int prevPage = Math.max(1, currentPage - 1);
        int nextPage = Math.min(totalPages, currentPage + 1);

        Button btnPrev = Button.secondary("kalki_players_page:" + prevPage + ":" + userId, "◀ Previous")
                .withDisabled(currentPage <= 1);
        Button btnRefresh = Button.secondary("kalki_players_refresh:" + currentPage + ":" + userId, "🔄 Refresh");
        Button btnNext = Button.secondary("kalki_players_page:" + nextPage + ":" + userId, "Next ▶")
                .withDisabled(currentPage >= totalPages);

        ActionRow actionRow = ActionRow.of(btnPrev, btnRefresh, btnNext);

        return new MessageCreateBuilder()
                .setContent(sb.toString())
                .setComponents(actionRow)
                .build();
    }
}
