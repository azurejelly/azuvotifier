package com.vexsoftware.votifier.sponge.platform.forwarding;

import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.sponge.NuVotifierSponge;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.network.ChannelBinding;
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.RawDataListener;
import org.spongepowered.api.network.RemoteConnection;

public class SpongePluginMessagingForwardingSink extends AbstractPluginMessagingForwardingSink implements RawDataListener {

    private final LoggingAdapter logger;
    private final ChannelBinding.RawDataChannel channelBinding;

    public SpongePluginMessagingForwardingSink(NuVotifierSponge plugin, String channel, ForwardedVoteListener listener) {
        super(listener, plugin.getPluginLogger());

        this.channelBinding = Sponge.getChannelRegistrar().getChannel(channel)
                .map((b) -> {
                    if (b instanceof ChannelBinding.RawDataChannel) {
                        return (ChannelBinding.RawDataChannel) b;
                    } else {
                        throw new IllegalStateException("Found an indexed channel - this is a problem.");
                    }
                }).orElseGet(() -> Sponge.getChannelRegistrar().createRawChannel(plugin, channel));

        this.channelBinding.addListener(Platform.Type.SERVER, this);
        this.logger = plugin.getPluginLogger();
    }

    @Override
    public void halt() {
        channelBinding.removeListener(this);
    }

    @Override
    public void handlePayload(ChannelBuf channelBuf, RemoteConnection remoteConnection, Platform.Type type) {
        byte[] msgDirBuf = channelBuf.readBytes(channelBuf.available());
        try {
            this.handlePluginMessage(msgDirBuf);
        } catch (Exception e) {
            logger.error("There was an unknown error when processing a forwarded vote.", e);
        }
    }
}
