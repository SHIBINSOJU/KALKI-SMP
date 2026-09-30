package live.shotdevs.kalkismp.status;

import live.shotdevs.kalkismp.KalkiSMP;
import live.shotdevs.kalkismp.config.ConfigManager;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.bukkit.Bukkit;

import java.util.List;

public class StatusButtonHandler {

    private final KalkiSMP plugin;
    private static final int PAGE_SIZE = 8;

    public StatusButtonHandler(KalkiSMP plugin) {
        this.plugin = plugin;
    }

    public void handleButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.equals(StatusComponentBuilder.BTN_PLAYERS)) {
            handlePlayersButton(event);
        } else if (componentId.equals(StatusComponentBuilder.BTN_REFRESH)) {
            handleRefreshButton(event);
        } else if (componentId.startsWith("kalki_players_page:") || componentId.startsWith("kalki_players_refresh:")) {
            handlePlayerListPagination(event);
        }
    }

    private void handlePlayersButton(ButtonInteractionEvent event) {
        ConfigManager config = plugin.getConfigManager();
        String userId = event.getUser().getId();

        // Access player list safely on main server thread or via Bukkit online players
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<PlayerListFormatter.FormattedPlayer> players = PlayerListFormatter.getFormattedOnlinePlayers();
            MessageCreateData message = StatusComponentBuilder.buildPlayerListMessage(config, userId, players, 1, PAGE_SIZE);

            // Respond ephemerally to the clicking user
            event.reply(message).setEphemeral(true).queue(
                    null,
                    failure -> plugin.getLogger().warning("[Status] Failed to reply to Players button click: " + failure.getMessage())
            );
        });
    }

    private void handleRefreshButton(ButtonInteractionEvent event) {
        // Defer edit to acknowledge button click cleanly
        event.deferEdit().queue(
                null,
                failure -> {}
        );

        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().getStatusManager() != null) {
            plugin.getDiscordManager().getStatusManager().updateStatusAsync();
        }
    }

    private void handlePlayerListPagination(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        String[] parts = componentId.split(":");
        if (parts.length < 3) return;

        int page;
        try {
            page = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            page = 1;
        }

        String ownerUserId = parts[2];
        if (!event.getUser().getId().equals(ownerUserId)) {
            event.reply("This player menu belongs to another user. Click '👥 Players' on the status panel to open your own.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        ConfigManager config = plugin.getConfigManager();
        final int targetPage = page;

        Bukkit.getScheduler().runTask(plugin, () -> {
            List<PlayerListFormatter.FormattedPlayer> players = PlayerListFormatter.getFormattedOnlinePlayers();
            MessageCreateData messageData = StatusComponentBuilder.buildPlayerListMessage(config, ownerUserId, players, targetPage, PAGE_SIZE);

            MessageEditData editData = MessageEditData.fromCreateData(messageData);
            event.editMessage(editData).queue(
                    null,
                    failure -> plugin.getLogger().warning("[Status] Failed to update player list pagination: " + failure.getMessage())
            );
        });
    }
}
