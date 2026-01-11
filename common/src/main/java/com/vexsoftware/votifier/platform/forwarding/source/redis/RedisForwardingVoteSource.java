package com.vexsoftware.votifier.platform.forwarding.source.redis;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.logger.LoggingAdapter;
import com.vexsoftware.votifier.platform.forwarding.source.ForwardingVoteSource;
import com.vexsoftware.votifier.redis.RedisCredentials;
import com.vexsoftware.votifier.util.gson.GsonInst;
import com.vexsoftware.votifier.redis.pool.RedisPoolFactory;
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
        this.pool = RedisPoolFactory.create(credentials);
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
