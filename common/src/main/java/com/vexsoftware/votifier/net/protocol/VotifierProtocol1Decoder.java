package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSA;
import com.vexsoftware.votifier.util.QuietException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Decodes original protocol votes.
 */
public class VotifierProtocol1Decoder extends ByteToMessageDecoder {
    private static final int MAX_PACKET_SIZE = 256;
    private static final int ATTACK_THRESHOLD = 3;
    private static final long ATTACK_WINDOW_MS = 60000; // 1 minute

    private static final Map<String, AttackTracker> attackTrackers = new ConcurrentHashMap<>();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
        if (!ctx.channel().isActive()) {
            buf.skipBytes(buf.readableBytes());
            return;
        }

        if (buf.readableBytes() < MAX_PACKET_SIZE) {
            return;
        }

        String remoteIp = getRemoteIp(ctx);

        if (buf.readableBytes() > MAX_PACKET_SIZE) {
            // Track potential attack
            AttackTracker tracker = attackTrackers.computeIfAbsent(remoteIp, k -> new AttackTracker());
            tracker.recordAttack();

            VotifierPlugin plugin = ctx.channel().attr(VotifierPlugin.KEY).get();

            if (tracker.isUnderAttack()) {
                // Log detailed attack information for security monitoring
                if (plugin.isDebug()) {
                    throw new CorruptedFrameException(String.format(
                            "Potential attack detected from %s: received %d bytes (max: %d). " +
                                    "Attack attempts: %d within %d ms. Connection blocked.",
                            remoteIp, buf.readableBytes(), MAX_PACKET_SIZE,
                            tracker.getAttackCount(), ATTACK_WINDOW_MS));
                } else {
                    throw new QuietException(String.format(
                            "Oversized packet from %s (%d bytes). Attack attempts: %d. Connection blocked.",
                            remoteIp, buf.readableBytes(), tracker.getAttackCount()));
                }
            } else {
                // First few attempts - log with less severity
                throw new QuietException(String.format(
                        "Could not decrypt data from %s as it is too long (%d bytes, expected %d). " +
                                "Possible misconfiguration or attack attempt (%d/%d).",
                        remoteIp, buf.readableBytes(), MAX_PACKET_SIZE,
                        tracker.getAttackCount(), ATTACK_THRESHOLD));
            }
        }

        byte[] block = ByteBufUtil.getBytes(buf);
        buf.skipBytes(buf.readableBytes());

        VotifierPlugin plugin = ctx.channel().attr(VotifierPlugin.KEY).get();

        try {
            block = RSA.decrypt(block, plugin.getProtocolV1Key().getPrivate());
        } catch (Exception e) {
            if (plugin.isDebug()) {
                throw new CorruptedFrameException("Could not decrypt data from " + ctx.channel().remoteAddress()
                        + ". Make sure the public key on the list is correct.", e);
            } else {
                throw new QuietException("Could not decrypt data from " + ctx.channel().remoteAddress()
                        + ". Make sure the public key on the list is correct.");
            }
        }

        // Parse the string we received.
        String all = new String(block, StandardCharsets.US_ASCII);
        String[] split = all.split("\n");
        if (split.length < 5) {
            throw new QuietException("Not enough fields specified in vote. This is not a NuVotifier issue. Got "
                    + split.length + " fields, but needed 5.");
        }

        if (!split[0].equals("VOTE")) {
            throw new QuietException("The VOTE opcode was not present. This is not a NuVotifier issue," +
                    "but a bug with the server list.");
        }

        // Create the vote.
        Vote vote = new Vote(split[1], split[2], split[3], split[4]);
        list.add(vote);

        // We are done, remove ourselves. Why? Sometimes, we will decode multiple vote messages.
        // Netty doesn't like this, so we must remove ourselves from the pipeline. With Protocol 1,
        // ending votes is a "fire and forget" operation, so this is safe.
        ctx.pipeline().remove(this);
    }

    private String getRemoteIp(ChannelHandlerContext ctx) {
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress) {
            return ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        }
        return ctx.channel().remoteAddress().toString();
    }

    /**
     * Tracks attack attempts from a single IP address
     */
    private static class AttackTracker {
        private final AtomicInteger attackCount = new AtomicInteger(0);
        private final AtomicLong firstAttackTime = new AtomicLong(0);
        private final AtomicLong lastAttackTime = new AtomicLong(0);

        void recordAttack() {
            long now = System.currentTimeMillis();
            long firstTime = firstAttackTime.get();

            if (firstTime == 0) {
                firstAttackTime.set(now);
                lastAttackTime.set(now);
                attackCount.set(1);
            } else {
                // Reset counter if attack window has passed
                if (now - firstTime > ATTACK_WINDOW_MS) {
                    firstAttackTime.set(now);
                    attackCount.set(1);
                } else {
                    attackCount.incrementAndGet();
                }
                lastAttackTime.set(now);
            }
        }

        boolean isUnderAttack() {
            long now = System.currentTimeMillis();
            long firstTime = firstAttackTime.get();

            // Check if we're still within the attack window and exceeded threshold
            return (now - firstTime <= ATTACK_WINDOW_MS) && (attackCount.get() >= ATTACK_THRESHOLD);
        }

        int getAttackCount() {
            return attackCount.get();
        }
    }
}