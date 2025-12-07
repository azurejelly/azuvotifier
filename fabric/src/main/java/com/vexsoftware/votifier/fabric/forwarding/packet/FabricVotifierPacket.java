package com.vexsoftware.votifier.fabric.forwarding.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.NotNull;

public class FabricVotifierPacket implements CustomPayload {

    public static CustomPayload.Id<FabricVotifierPacket> PACKET_ID;
    public static final PacketCodec<RegistryByteBuf, FabricVotifierPacket> PACKET_CODEC
            = PacketCodec.of(FabricVotifierPacket::write, FabricVotifierPacket::new);

    private final byte[] data;

    public FabricVotifierPacket(@NotNull RegistryByteBuf buf) {
        this.data = new byte[buf.readableBytes()];
        buf.readBytes(data);
    }

    public void write(RegistryByteBuf buf) {
        buf.writeBytes(data);
    }

    public byte[] getData() {
        return this.data;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

    public static void setPacketId(CustomPayload.Id<FabricVotifierPacket> id) {
        PACKET_ID = id;
    }

    public static void clearPacketId() {
        PACKET_ID = null;
    }
}
