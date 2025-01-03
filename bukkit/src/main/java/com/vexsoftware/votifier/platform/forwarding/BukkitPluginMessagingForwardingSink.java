package com.vexsoftware.votifier.platform.forwarding;

import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Created by Joe Hirschfeld on 10/20/2015.
 */
public class BukkitPluginMessagingForwardingSink extends AbstractPluginMessagingForwardingSink implements PluginMessageListener {

    private final Plugin plugin;
    private final String channel;

    public BukkitPluginMessagingForwardingSink(
            Plugin plugin,
            String channel,
            ForwardedVoteListener listener,
            LoggingAdapter logger
    ) {
        super(listener, logger);

        if (channel == null) {
            throw new IllegalArgumentException("channel cannot be null");
        }

        this.channel = channel;
        this.plugin = plugin;

        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, channel, this);
    }

    @Override
    public void halt() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, byte @NotNull [] bytes) {
        try {
            this.handlePluginMessage(bytes);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "There was an unknown error when processing a forwarded vote.", e);
        }
    }
}
