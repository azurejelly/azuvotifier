package com.vexsoftware.votifier.velocity.utils;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import lombok.experimental.UtilityClass;

@UtilityClass
public class VelocityConstants {

    public static final String VERSION = "${version}";
    public static final int BSTATS_ID = 26785;

    // very hacky way to prevent the main command from being entirely blue on legacy clients
    public static final TextColor SECONDARY_COLOR_FALLBACK = NamedTextColor.WHITE;
}