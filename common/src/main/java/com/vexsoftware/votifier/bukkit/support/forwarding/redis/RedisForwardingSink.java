package com.vexsoftware.votifier.bukkit.support.forwarding.redis;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.bukkit.model.Vote;
import com.vexsoftware.votifier.bukkit.platform.LoggingAdapter;
import com.vexsoftware.votifier.bukkit.support.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.bukkit.support.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.bukkit.util.gson.GsonInst;
import com.vexsoftware.votifier.bukkit.util.redis.RedisPoolBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * @author AkramL, azurejelly
 */
public class RedisForwardingSink extends JedisPubSub implements ForwardingVoteSink {

    private final RedisCredentials credentials;
    private final ForwardedVoteListener listener;
    private final LoggingAdapter logger;
    private final JedisPool pool;
    private Thread thread;

    public RedisForwardingSink(RedisCredentials credentials, ForwardedVoteListener listener, LoggingAdapter logger) {
        this.pool = RedisPoolBuilder.fromCredentials(credentials);
        this.credentials = credentials;
        this.logger = logger;
        this.listener = listener;
    }

    private void handleMessage(String message) {
        JsonObject object = GsonInst.GSON.fromJson(message, JsonObject.class);
        Vote vote = new Vote(object);
        listener.onForward(vote);
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(credentials.getChannel())) {
            // Using try-catch block to avoid channel break on exceptions.
            try {
                handleMessage(message);
            } catch (Exception ex) {
                logger.error("Failed to handle Redis message", ex);
            }
        }
    }

    @Override
    public void init() throws RuntimeException {
        this.thread = new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(this, credentials.getChannel());
            }
        }, "Votifier Redis Forwarding Sink");

        this.thread.start();
    }

    @Override
    public void halt() {
        try {
            thread.interrupt();
        } catch (Exception ex) {
            logger.error("Failed to interrupt thread", ex);
        }

        try {
            pool.destroy();
        } catch (Exception ex) {
            logger.error("Failed to destroy Redis pool", ex);
        }
    }
}
