package com.vexsoftware.votifier.bungee.platform.forwarding;

import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import com.vexsoftware.votifier.bungee.platform.server.BungeeBackendServer;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.platform.forwarding.source.messaging.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.platform.forwarding.ServerFilter;
import com.vexsoftware.votifier.cache.VoteCache;
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

    private final NuVotifierBungee plugin;
    private final String fallback;

    public OnlineForwardPluginMessagingForwardingSource(
            String channel,
            NuVotifierBungee plugin,
            ServerFilter serverFilter,
            VoteCache cache,
            String fallback,
            int dumpRate
    ) {
        super(channel, serverFilter, plugin, cache, dumpRate);
        this.plugin = plugin;
        this.fallback = fallback;
    }

    @Override
    public void init() {
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void forward(Vote vote) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(vote.getUsername());

        if (player != null && player.getServer() != null && serverFilter.isAllowed(player.getServer().getInfo().getName())) {
            if (forwardSpecific(new BungeeBackendServer(player.getServer().getInfo()), vote)) {
                if (plugin.isDebug()) {
                    plugin.getPluginLogger().info("Successfully forwarded vote " + vote
                            + " to server " + player.getServer().getInfo().getName());
                }

                return;
            }
        }

        ServerInfo serverInfo = plugin.getProxy().getServers().get(fallback);

        // nowhere to fall back to, yet still not online. lets save this vote yet!
        if (serverInfo == null) {
            attemptToAddToPlayerCache(vote, vote.getUsername());
        } else if (!forwardSpecific(new BungeeBackendServer(serverInfo), vote)) {
            attemptToAddToCache(vote, fallback);
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getTag().equals(channel)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onServerConnected(final ServerConnectedEvent event) { // Attempt to resend any votes that were previously cached.
        BackendServer server = new BungeeBackendServer(event.getServer().getInfo());
        handlePlayerSwitch(server, event.getPlayer().getName());
        onServerConnect(server);
    }
}
