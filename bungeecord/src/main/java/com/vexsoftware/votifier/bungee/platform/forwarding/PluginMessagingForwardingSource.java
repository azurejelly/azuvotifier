package com.vexsoftware.votifier.bungee.platform.forwarding;

import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import com.vexsoftware.votifier.bungee.platform.server.BungeeBackendServer;
import com.vexsoftware.votifier.platform.forwarding.source.messaging.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.platform.forwarding.ServerFilter;
import com.vexsoftware.votifier.cache.VoteCache;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PluginMessagingForwardingSource extends AbstractPluginMessagingForwardingSource implements Listener {

    private final NuVotifierBungee plugin;

    public PluginMessagingForwardingSource(String channel, ServerFilter filter, NuVotifierBungee plugin, VoteCache cache, int dumpRate) {
        super(channel, filter, plugin, cache, dumpRate);
        this.plugin = plugin;
    }

    protected PluginMessagingForwardingSource(String channel, NuVotifierBungee plugin, VoteCache voteCache, int dumpRate) {
        super(channel, plugin, voteCache, dumpRate);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getTag().equals(channel)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onServerConnected(final ServerConnectedEvent event) {
        // Attempt to resend any votes that were previously cached.
        onServerConnect(new BungeeBackendServer(event.getServer().getInfo()));
    }

    @Override
    public void init() {
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }
}
