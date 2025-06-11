package com.vexsoftware.votifier.velocity.utils;

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

public class VelocityUtil {

    private VelocityUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static ChannelIdentifier getId(String channel) {
        if (channel.contains(":")) {
            String[] split = channel.split(":");
            return MinecraftChannelIdentifier.create(split[0], split[1]);
        }

        return new LegacyChannelIdentifier(channel);
    }
}
