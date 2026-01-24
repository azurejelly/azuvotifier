package com.vexsoftware.votifier.bungee.util;

import com.vexsoftware.votifier.util.CommonConstants;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Utility to obtain colors depending on the player's
 * Minecraft client version
 *
 * @author azurejelly
 * @since 3.4.0
 */
@UtilityClass
public class BungeeColors {

    public static boolean supportsHexColors(CommandSender sender) {
        if (!(sender instanceof ProxiedPlayer)) {
            return false;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        return player.getPendingConnection().getVersion() >= BungeeConstants.PROTOCOL_1_16;
    }

    public static ChatColor getColor(CommandSender sender, int rgb, ChatColor fallback) {
        if (supportsHexColors(sender)) {
            String hex = String.format("#%06X", rgb);
            return ChatColor.of(hex);
        } else {
            return fallback;
        }
    }

    public static ChatColor primary(CommandSender sender) {
        return getColor(sender, CommonConstants.PRIMARY_COLOR, ChatColor.AQUA);
    }

    public static ChatColor secondary(CommandSender sender) {
        return getColor(sender, CommonConstants.SECONDARY_COLOR, ChatColor.WHITE);
    }

    public static ChatColor processing(CommandSender sender) {
        return getColor(sender, CommonConstants.PROCESSING_COLOR, ChatColor.YELLOW);
    }

    public static ChatColor success(CommandSender sender) {
        return getColor(sender, CommonConstants.SUCCESS_COLOR, ChatColor.GREEN);
    }

    public static ChatColor failure(CommandSender sender) {
        return getColor(sender, CommonConstants.FAILURE_COLOR, ChatColor.RED);
    }
}
