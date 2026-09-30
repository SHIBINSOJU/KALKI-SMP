package live.shotdevs.kalkismp.status;

import live.shotdevs.kalkismp.util.MessageFormatter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerListFormatter {

    public record FormattedPlayer(String name, String prefix, String primaryGroup) {}

    public static List<FormattedPlayer> getFormattedOnlinePlayers() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        List<FormattedPlayer> result = new ArrayList<>();

        boolean luckPermsAvailable = isLuckPermsAvailable();
        LuckPerms luckPerms = luckPermsAvailable ? LuckPermsProvider.get() : null;

        for (Player p : onlinePlayers) {
            String prefix = null;
            String group = null;

            if (luckPerms != null) {
                try {
                    User user = luckPerms.getUserManager().getUser(p.getUniqueId());
                    if (user != null) {
                        prefix = user.getCachedData().getMetaData().getPrefix();
                        group = user.getPrimaryGroup();
                    }
                } catch (Exception ignored) {}
            }

            String cleanPrefix = prefix != null ? MessageFormatter.stripColor(prefix).trim() : null;
            String cleanGroup = group != null ? group.toUpperCase() : null;

            result.add(new FormattedPlayer(p.getName(), cleanPrefix, cleanGroup));
        }

        return result;
    }

    public static String buildPlayerListText(List<FormattedPlayer> players, int page, int pageSize) {
        if (players.isEmpty()) {
            return "No players are currently online.";
        }

        int totalPlayers = players.size();
        int totalPages = (int) Math.ceil((double) totalPlayers / pageSize);
        int currentPage = Math.max(1, Math.min(page, totalPages));

        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalPlayers);

        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            FormattedPlayer fp = players.get(i);
            if (fp.prefix() != null && !fp.prefix().isBlank()) {
                sb.append(fp.prefix()).append("\n").append(fp.name()).append("\n\n");
            } else if (fp.primaryGroup() != null && !fp.primaryGroup().isBlank() && !fp.primaryGroup().equalsIgnoreCase("default")) {
                sb.append("🛡️ ").append(fp.primaryGroup()).append("\n").append(fp.name()).append("\n\n");
            } else {
                sb.append("👤 ").append(fp.name()).append("\n\n");
            }
        }

        return sb.toString().trim();
    }

    private static boolean isLuckPermsAvailable() {
        try {
            return Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
        } catch (Exception e) {
            return false;
        }
    }
}
