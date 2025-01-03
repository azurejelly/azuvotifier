package com.vexsoftware.votifier.support.forwarding.redis;

import com.google.gson.Gson;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * @author AkramL
 */
public class RedisForwardingVoteSource implements ForwardingVoteSource {

    private final JedisPool pool;
    private final Gson gson = new Gson();
    private final String channel;

    public RedisForwardingVoteSource(RedisCredentials credentials, RedisPoolConfiguration cfg) {
        this.channel = credentials.getChannel();

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(cfg.getMaxTotal());
        jedisPoolConfig.setMaxIdle(cfg.getMaxIdle());
        jedisPoolConfig.setMinIdle(cfg.getMinIdle());
        jedisPoolConfig.setMinEvictableIdleTime(Duration.ofMillis(cfg.getMinEvictableIdleTime()));
        jedisPoolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(cfg.getTimeBetweenEvictionRuns()));
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
    }

    @Override
    public void forward(Vote v) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, gson.toJson(v.serialize()));
        }
    }

    @Override
    public void halt() {
        try {
            pool.destroy();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
