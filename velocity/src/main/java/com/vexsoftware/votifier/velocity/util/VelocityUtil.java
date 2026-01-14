package com.vexsoftware.votifier.velocity.util;

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VelocityUtil {

    public static ChannelIdentifier getId(String channel) {
        if (channel.contains(":")) {
            String[] split = channel.split(":");
            return MinecraftChannelIdentifier.create(split[0], split[1]);
        }

        return new LegacyChannelIdentifier(channel);
    }
}
