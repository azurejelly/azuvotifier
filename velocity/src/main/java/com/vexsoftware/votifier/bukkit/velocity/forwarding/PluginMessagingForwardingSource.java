package com.vexsoftware.votifier.bukkit.velocity.forwarding;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.vexsoftware.votifier.bukkit.support.forwarding.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.bukkit.support.forwarding.ServerFilter;
import com.vexsoftware.votifier.bukkit.support.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.bukkit.velocity.NuVotifierVelocity;
import com.vexsoftware.votifier.bukkit.velocity.platform.server.VelocityBackendServer;
import com.vexsoftware.votifier.bukkit.velocity.utils.VelocityUtil;

public class PluginMessagingForwardingSource extends AbstractPluginMessagingForwardingSource {

    private final NuVotifierVelocity plugin;
    private final ChannelIdentifier velocityChannelId;

    public PluginMessagingForwardingSource(String channel, ServerFilter filter, NuVotifierVelocity plugin, VoteCache cache, int dumpRate) {
        super(channel, filter, plugin, cache, dumpRate);
        this.plugin = plugin;
        this.velocityChannelId = VelocityUtil.getId(channel);
    }

    @Subscribe
    public void onServerConnected(final ServerConnectedEvent e) { //Attempt to resend any votes that were previously cached.
        onServerConnect(new VelocityBackendServer(e.getServer()));
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getIdentifier().equals(velocityChannelId)) {
            e.setResult(PluginMessageEvent.ForwardResult.handled());
        }
    }

    @Override
    public void init() {
        plugin.getServer().getChannelRegistrar().register(velocityChannelId);
        plugin.getServer().getEventManager().register(plugin, this);
        plugin.getLogger().info("Forwarding votes over plugin messaging channel '{}'", channel);
    }
}
