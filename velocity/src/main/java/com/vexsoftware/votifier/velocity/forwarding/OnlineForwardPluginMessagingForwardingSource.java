package com.vexsoftware.votifier.velocity.forwarding;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.support.forwarding.ServerFilter;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.velocity.NuVotifierVelocity;
import com.vexsoftware.votifier.velocity.platform.server.VelocityBackendServer;
import com.vexsoftware.votifier.velocity.utils.VelocityUtil;

import java.util.Optional;

public final class OnlineForwardPluginMessagingForwardingSource extends AbstractPluginMessagingForwardingSource {

    private final String fallbackServer;
    private final NuVotifierVelocity plugin;
    private final ChannelIdentifier velocityChannelId;

    public OnlineForwardPluginMessagingForwardingSource(String channel, ServerFilter filter, NuVotifierVelocity plugin, VoteCache cache, String fallback, int dumpRate) {
        super(channel, filter, plugin, cache, dumpRate);
        this.fallbackServer = fallback;
        this.plugin = plugin;
        this.velocityChannelId = VelocityUtil.getId(channel);
    }

    @Override
    public void init() {
        plugin.getServer().getChannelRegistrar().register(velocityChannelId);
        plugin.getServer().getEventManager().register(plugin, this);
        plugin.getLogger().info("Forwarding votes over plugin messaging channel '{}' for online players", channel);
    }

    @Override
    public void forward(Vote v) {
        Optional<Player> p = plugin.getServer().getPlayer(v.getUsername());
        Optional<ServerConnection> sc = p.flatMap(Player::getCurrentServer);

        if (sc.isPresent() && serverFilter.isAllowed(sc.get().getServerInfo().getName())) {
            if (forwardSpecific(new VelocityBackendServer(sc.get().getServer()), v)) {
                if (plugin.isDebug()) {
                    plugin.getPluginLogger().info("Successfully forwarded vote " + v + " to server "
                            + sc.get().getServerInfo().getName());
                }

                return;
            }
        }

        Optional<RegisteredServer> fs = fallbackServer == null
                ? Optional.empty()
                : plugin.getServer().getServer(fallbackServer);

        // nowhere to fall back to, yet still not online. lets save this vote yet!
        if (fs.isEmpty()) {
            attemptToAddToPlayerCache(v, v.getUsername());
        } else if (!forwardSpecific(new VelocityBackendServer(fs.get()), v)) {
            attemptToAddToCache(v, fallbackServer);
        }
    }

    @Subscribe
    public void onServerConnected(final ServerConnectedEvent e) { //Attempt to resend any votes that were previously cached.
        BackendServer server = new VelocityBackendServer(e.getServer());
        onServerConnect(server);
        handlePlayerSwitch(server, e.getPlayer().getUsername());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getIdentifier().equals(velocityChannelId)) {
            e.setResult(PluginMessageEvent.ForwardResult.handled());
        }
    }
}
