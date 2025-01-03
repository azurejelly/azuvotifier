package com.vexsoftware.votifier.support.forwarding.redis;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.util.GsonInst;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.net.URI;
import java.net.URISyntaxException;

import static com.vexsoftware.votifier.util.RedisPoolUtils.getJedisConfiguration;

/**
 * @author AkramL, azurejelly
 */
public class RedisForwardingSink extends JedisPubSub implements ForwardingVoteSink {

    private final ForwardedVoteListener listener;
    private final LoggingAdapter logger;
    private final String channel;
    private final JedisPool pool;
    private final Thread thread;

    private static final int REDIS_TIMEOUT_MS = 5000;

    public RedisForwardingSink(RedisCredentials credentials, ForwardedVoteListener listener, LoggingAdapter logger) {
        this.logger = logger;
        this.channel = credentials.getChannel();
        this.listener = listener;

        JedisPoolConfig cfg = getJedisConfiguration();
        if (credentials.getURI() != null && !credentials.getURI().isBlank()) {
            try {
                URI uri = new URI(credentials.getURI().trim());
                this.pool = new JedisPool(cfg, uri, 5000);
            } catch (URISyntaxException e) {
                throw new RuntimeException("The Redis URI is invalid and a forwarding sink cannot be built:", e);
            }
        } else {
            if (credentials.getHost() == null || credentials.getHost().isBlank()) {
                throw new IllegalArgumentException("No Redis hostname or URI provided");
            }

            if (credentials.getPort() <= 0 || credentials.getPort() >= 65535) {
                throw new IllegalArgumentException("Redis port must be within range");
            }

            String hostname = credentials.getHost();
            int port = credentials.getPort();
            String username = credentials.getUsername();
            String password = credentials.getPassword();

            // please let me know (or open a pull req) if
            // there's a better way of doing this because i
            // was not able to find one
            boolean hasUsername = username != null && !username.isBlank();
            boolean hasPassword = password != null && !password.isBlank();

            if (hasUsername) {
                if (hasPassword) {
                    this.pool = new JedisPool(cfg, hostname, port, REDIS_TIMEOUT_MS, username, password);
                } else {
                    this.pool = new JedisPool(cfg, hostname, port, REDIS_TIMEOUT_MS, username);
                }
            } else {
                if (hasPassword) {
                    this.pool = new JedisPool(cfg, hostname, port, REDIS_TIMEOUT_MS, password);
                } else {
                    this.pool = new JedisPool(cfg, hostname, port, REDIS_TIMEOUT_MS);
                }
            }
        }

        this.thread = new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(this, channel);
            }
        }, "Votifier Redis Forwarding Sink");

        this.thread.start();
    }

    private void handleMessage(String message) {
        JsonObject object = GsonInst.GSON.fromJson(message, JsonObject.class);
        Vote vote = new Vote(object);
        listener.onForward(vote);
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(this.channel)) {
            // Using try-catch block to avoid channel break on exceptions.
            try {
                handleMessage(message);
            } catch (Exception ex) {
                logger.error("Failed to handle Redis message", ex);
            }
        }
    }

    @Override
    public void halt() {
        try {
            pool.destroy();
        } catch (Exception ex) {
            logger.error("Failed to destroy Redis pool", ex);
        }
    }
}
