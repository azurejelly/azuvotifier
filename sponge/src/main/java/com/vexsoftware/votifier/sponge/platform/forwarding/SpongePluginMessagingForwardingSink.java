package com.vexsoftware.votifier.sponge.platform.forwarding;

import com.vexsoftware.votifier.platform.logger.LoggingAdapter;
import com.vexsoftware.votifier.sponge.NuVotifierSponge;
import com.vexsoftware.votifier.platform.forwarding.sink.messaging.AbstractPluginMessagingForwardingSink;
import com.vexsoftware.votifier.platform.forwarding.listener.ForwardedVoteListener;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.network.ServerConnectionState;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataHandler;

public class SpongePluginMessagingForwardingSink
        extends AbstractPluginMessagingForwardingSink
        implements RawPlayDataHandler<ServerConnectionState.Game> {

    private final LoggingAdapter logger;
    private final RawDataChannel channel;

    public SpongePluginMessagingForwardingSink(NuVotifierSponge plugin, String channel, ForwardedVoteListener listener) {
        super(listener, plugin.getPluginLogger());

        this.channel = Sponge.game().channelManager().ofType(ResourceKey.resolve(channel), RawDataChannel.class);
        this.logger = plugin.getPluginLogger();
    }

    @Override
    public void init() {
        this.channel.play().addHandler(ServerConnectionState.Game.class, this);
        this.logger.info("Receiving votes over plugin messaging channel '{}'", channel.key().asString());
    }

    @Override
    public void halt() {
        if (channel != null) {
            channel.play().removeHandler(this);
        }
    }

    @Override
    public void handlePayload(ChannelBuf buf, ServerConnectionState.Game state) {
        byte[] msgDirBuf = buf.readBytes(buf.available());
        try {
            this.handlePluginMessage(msgDirBuf);
        } catch (Exception e) {
            logger.error("There was an unknown error when processing a forwarded vote.", e);
        }
    }
}
