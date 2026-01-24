package com.vexsoftware.votifier.util;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;

// We're suppressing deprecation warnings as we use ChatColor to preserve
// compatibility with older versions of Minecraft (e.g. 1.12.2 and 1.8.8)
@UtilityClass
@SuppressWarnings("deprecation")
public class BukkitColors {

    public static String getColor(int rgb, ChatColor fallback) {
        try {
            String hex = String.format("#%06X", rgb);
            return net.md_5.bungee.api.ChatColor.of(hex).toString();
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return fallback.toString();
        }
    }

    public String primary() {
        return getColor(CommonConstants.PRIMARY_COLOR, ChatColor.AQUA);
    }

    public String secondary() {
        return getColor(CommonConstants.SECONDARY_COLOR, ChatColor.WHITE);
    }

    public String success() {
        return getColor(CommonConstants.SUCCESS_COLOR, ChatColor.GREEN);
    }

    public String failure() {
        return getColor(CommonConstants.FAILURE_COLOR, ChatColor.RED);
    }

    public String processing() {
        return getColor(CommonConstants.PROCESSING_COLOR, ChatColor.YELLOW);
    }
}
