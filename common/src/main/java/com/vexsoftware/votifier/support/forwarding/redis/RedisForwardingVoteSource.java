package com.vexsoftware.votifier.support.forwarding.redis;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSource;
import com.vexsoftware.votifier.util.GsonInst;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * @author AkramL
 */
public class RedisForwardingVoteSource implements ForwardingVoteSource {

    private final JedisPool pool;
    private final String channel;
    private final LoggingAdapter logger;

    public RedisForwardingVoteSource(RedisCredentials credentials, RedisPoolConfiguration cfg, LoggingAdapter logger) {
        this.channel = credentials.getChannel();
        this.logger = logger;

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
    }

    @Override
    public void forward(Vote v) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, GsonInst.GSON.toJson(v.serialize()));
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
