package com.vexsoftware.votifier.fabric.platform.forwarding;

import com.vexsoftware.votifier.fabric.platform.forwarding.packet.FabricVotifierPacket;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class FabricMessagingForwardingSink extends AbstractPluginMessagingForwardingSink implements ServerPlayNetworking.PlayPayloadHandler<FabricVotifierPacket> {

    private final Identifier channel;
    private final CustomPayload.Id<FabricVotifierPacket> id;

    public FabricMessagingForwardingSink(String channel, ForwardedVoteListener listener, LoggingAdapter logger) {
        super(listener, logger);
        this.channel = Identifier.of(channel);
        this.id = new CustomPayload.Id<>(this.channel);
    }

    @Override
    public void init() {
        FabricVotifierPacket.setPacketId(id);
        PayloadTypeRegistry.playC2S().register(id, FabricVotifierPacket.PACKET_CODEC);
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
