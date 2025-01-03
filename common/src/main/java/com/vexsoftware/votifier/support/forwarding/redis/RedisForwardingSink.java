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

import java.time.Duration;

/**
 * @author AkramL
 */
public class RedisForwardingSink extends JedisPubSub implements ForwardingVoteSink {

    private final ForwardedVoteListener listener;
    private final LoggingAdapter logger;
    private final String channel;
    private final JedisPool pool;

    public RedisForwardingSink(
            RedisCredentials credentials,
            RedisPoolConfiguration cfg,
            ForwardedVoteListener listener,
            LoggingAdapter logger
    ) {
        this.logger = logger;
        this.channel = credentials.getChannel();
        this.listener = listener;

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(cfg.getMaxTotal());
        jedisPoolConfig.setMaxIdle(cfg.getMaxIdle());
        jedisPoolConfig.setMinIdle(cfg.getMinIdle());
        jedisPoolConfig.setMinEvictableIdleTime(Duration.ofMillis(cfg.getMinEvictableIdleTime()));
        jedisPoolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(cfg.getTimeBetweenEvictionRuns()));
        jedisPoolConfig.setNumTestsPerEvictionRun(cfg.getNumTestsPerEvictionRun());
        jedisPoolConfig.setBlockWhenExhausted(cfg.isBlockWhenExhausted());

        String password = credentials.getPassword();
        if (password == null || password.trim().isEmpty()) {
            this.pool = new JedisPool(jedisPoolConfig,
                    credentials.getHost(),
                    credentials.getPort(),
                    5000
            );
        } else {
            this.pool = new JedisPool(jedisPoolConfig,
                    credentials.getHost(),
                    credentials.getPort(),
                    5000,
                    credentials.getPassword()
            );
        }

        // Using a CompletableFuture here caused the vote to be received
        // like 4 times instead of 1 - I shouldn't be using a Thread here
        // because Bukkit doesn't like it, but I don't like Bukkit either
        // at this point so fuck it
        new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(this, channel);
            }
        }, "Votifier Redis Forwarding Sink").start();
    }

    public void handleMessage(String message) {
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
