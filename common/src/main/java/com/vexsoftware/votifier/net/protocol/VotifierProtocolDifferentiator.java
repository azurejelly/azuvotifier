package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.util.QuietException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Attempts to determine if original protocol or protocol v2 is being used.
 */
public class VotifierProtocolDifferentiator extends ByteToMessageDecoder {

    private static final QuietException V2_ONLY = new QuietException("This server only accepts well-formed Votifier v2 packets.");
    private static final short PROTOCOL_2_MAGIC = 0x733A;

    private final boolean testMode;
    private final boolean allowV1;

    public VotifierProtocolDifferentiator(boolean testMode, boolean allowV1) {
        this.testMode = testMode;
        this.allowV1 = allowV1;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
        // Determine the number of bytes that are available.
        int readable = buf.readableBytes();

        if (readable < 2) {
            // Some voting sites seem to send empty buffers for no good reason.
            return;
        }

        short readMagic = buf.getShort(0);
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        if (readMagic == PROTOCOL_2_MAGIC) {
            // Short 0x733A + Message = Protocol v2 Vote
            session.setVersion(VotifierSession.ProtocolVersion.TWO);

            if (!testMode) {
                ctx.pipeline().addAfter("protocolDifferentiator", "protocol2LengthDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, 0, 4));
                ctx.pipeline().addAfter("protocol2LengthDecoder", "protocol2StringDecoder", new StringDecoder(StandardCharsets.UTF_8));
                ctx.pipeline().addAfter("protocol2StringDecoder", "protocol2VoteDecoder", new VotifierProtocol2Decoder());
                ctx.pipeline().addAfter("protocol2VoteDecoder", "protocol2StringEncoder", new StringEncoder(StandardCharsets.UTF_8));
                ctx.pipeline().remove(this);
            }
        } else {
            if (!allowV1) {
                throw V2_ONLY;
            }

            // Probably Protocol v1 Vote Message
            session.setVersion(VotifierSession.ProtocolVersion.ONE);
            if (!testMode) {
                ctx.pipeline().addAfter("protocolDifferentiator", "protocol1Handler", new VotifierProtocol1Decoder());
                ctx.pipeline().remove(this);
            }
        }
    }
}
