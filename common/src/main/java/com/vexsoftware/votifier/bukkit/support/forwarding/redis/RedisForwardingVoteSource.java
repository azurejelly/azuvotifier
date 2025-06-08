package com.vexsoftware.votifier.bukkit.support.forwarding.redis;

import com.vexsoftware.votifier.bukkit.model.Vote;
import com.vexsoftware.votifier.bukkit.platform.LoggingAdapter;
import com.vexsoftware.votifier.bukkit.support.forwarding.ForwardingVoteSource;
import com.vexsoftware.votifier.bukkit.util.gson.GsonInst;
import com.vexsoftware.votifier.bukkit.util.redis.RedisPoolBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author AkramL, azurejelly
 */
public class RedisForwardingVoteSource implements ForwardingVoteSource {

    private final RedisCredentials credentials;
    private final LoggingAdapter logger;
    private final JedisPool pool;

    public RedisForwardingVoteSource(RedisCredentials credentials, LoggingAdapter logger) {
        this.credentials = credentials;
        this.logger = logger;
        this.pool = RedisPoolBuilder.fromCredentials(credentials);
    }

    @Override
    public void init() {
        /* noop */
    }

    @Override
    public void forward(Vote v) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(credentials.getChannel(), GsonInst.GSON.toJson(v.serialize()));
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
