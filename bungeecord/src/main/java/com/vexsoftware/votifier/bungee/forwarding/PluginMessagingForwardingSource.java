package com.vexsoftware.votifier.bungee.forwarding;

import com.vexsoftware.votifier.bungee.platform.BungeeBackendServer;
import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.support.forwarding.ServerFilter;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PluginMessagingForwardingSource extends AbstractPluginMessagingForwardingSource implements Listener {

    public PluginMessagingForwardingSource(String channel, ServerFilter filter, NuVotifierBungee votifier, VoteCache cache, int dumpRate) {
        super(channel, filter, votifier, cache, dumpRate);
        ProxyServer.getInstance().getPluginManager().registerListener(votifier, this);
    }

    protected PluginMessagingForwardingSource(String channel, NuVotifierBungee nuVotifier, VoteCache voteCache, int dumpRate) {
        super(channel, nuVotifier, voteCache, dumpRate);
        ProxyServer.getInstance().getPluginManager().registerListener(nuVotifier, this);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getTag().equals(channel)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onServerConnected(final ServerConnectedEvent e) {
        // Attempt to resend any votes that were previously cached.
        onServerConnect(new BungeeBackendServer(e.getServer().getInfo()));
    }
}
