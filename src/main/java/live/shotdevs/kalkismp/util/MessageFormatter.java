package live.shotdevs.kalkismp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utility class for formatting text and components across Bukkit and Discord.
 */
public class MessageFormatter {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Converts legacy color codes (e.g. &a, &c) into an Adventure Component.
     */
    public static Component formatComponent(String text) {
        if (text == null) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(text);
    }

    /**
     * Sanitizes Discord input by stripping pings (@everyone, @here, role pings) to prevent unwanted mentions.
     */
    public static String sanitizeDiscordMentions(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("@everyone", "@\u200Beveryone")
                   .replace("@here", "@\u200Bhere")
                   .replaceAll("<@&(\\d+)>", "@role")
                   .replaceAll("<@!?(\\d+)>", "@user");
    }

    /**
     * Strips legacy color codes from a string.
     */
    public static String stripColor(String input) {
        if (input == null) return "";
        return input.replaceAll("(?i)&[0-9A-FK-OR]", "").replaceAll("(?i)§[0-9A-FK-OR]", "");
    }
}
