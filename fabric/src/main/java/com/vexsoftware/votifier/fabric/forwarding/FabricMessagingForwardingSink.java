package com.vexsoftware.votifier.fabric.forwarding;

import com.vexsoftware.votifier.fabric.forwarding.packet.FabricVotifierPacket;
import com.vexsoftware.votifier.platform.logger.LoggingAdapter;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class FabricMessagingForwardingSink extends AbstractPluginMessagingForwardingSink implements ServerPlayNetworking.PlayPayloadHandler<FabricVotifierPacket> {

    private final Identifier channel;
    private final LoggingAdapter logger;
    private final CustomPayload.Id<FabricVotifierPacket> id;

    public FabricMessagingForwardingSink(String channel, ForwardedVoteListener listener, LoggingAdapter logger) {
        super(listener, logger);
        this.channel = Identifier.of(channel);
        this.logger = logger;
        this.id = new CustomPayload.Id<>(this.channel);
    }

    @Override
    public void init() {
        FabricVotifierPacket.setPacketId(id);

        try {
            PayloadTypeRegistry.playC2S().register(id, FabricVotifierPacket.PACKET_CODEC);
        } catch (IllegalArgumentException ex) {
            logger.warn("Plugin messaging packet already registered during sink initialization. " +
                    "This is probably fine!");
        }

        ServerPlayNetworking.registerGlobalReceiver(id, this);
    }

    @Override
    public void halt() {
        FabricVotifierPacket.clearPacketId();
        ServerPlayNetworking.unregisterGlobalReceiver(channel);
    }

    @Override
    public void receive(FabricVotifierPacket packet, ServerPlayNetworking.Context ctx) {
        byte[] data = packet.getData();
        handlePluginMessage(data);
    }
}
