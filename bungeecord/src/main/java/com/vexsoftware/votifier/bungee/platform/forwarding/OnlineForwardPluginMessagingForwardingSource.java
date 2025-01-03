package com.vexsoftware.votifier.bungee.platform.forwarding;

import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import com.vexsoftware.votifier.bungee.platform.server.BungeeBackendServer;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.support.forwarding.ServerFilter;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * @author Joseph Hirschfeld
 * @date 12/31/2015
 */
public final class OnlineForwardPluginMessagingForwardingSource extends AbstractPluginMessagingForwardingSource implements Listener {

    private final String fallback;

    public OnlineForwardPluginMessagingForwardingSource(
            String channel,
            NuVotifierBungee nuVotifier,
            ServerFilter serverFilter,
            VoteCache cache,
            String fallback,
            int dumpRate
    ) {
        super(channel, serverFilter, nuVotifier, cache, dumpRate);
        this.fallback = fallback;

        ProxyServer.getInstance().getPluginManager().registerListener(nuVotifier, this);
    }

    @Override
    public void forward(Vote v) {
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(v.getUsername());
        if (p != null && p.getServer() != null && serverFilter.isAllowed(p.getServer().getInfo().getName())) {
            if (forwardSpecific(new BungeeBackendServer(p.getServer().getInfo()), v)) {
                if (plugin.isDebug()) {
                    plugin.getPluginLogger().info("Successfully forwarded vote " + v
                            + " to server " + p.getServer().getInfo().getName());
                }

                return;
            }
        }

        ServerInfo serverInfo = ProxyServer.getInstance().getServers().get(fallback);

        // nowhere to fall back to, yet still not online. lets save this vote yet!
        if (serverInfo == null) {
            attemptToAddToPlayerCache(v, v.getUsername());
        } else if (!forwardSpecific(new BungeeBackendServer(serverInfo), v)) {
            attemptToAddToCache(v, fallback);
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getTag().equals(channel)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onServerConnected(final ServerConnectedEvent e) { //Attempt to resend any votes that were previously cached.
        BackendServer server = new BungeeBackendServer(e.getServer().getInfo());
        handlePlayerSwitch(server, e.getPlayer().getName());
        onServerConnect(server);
    }
}
