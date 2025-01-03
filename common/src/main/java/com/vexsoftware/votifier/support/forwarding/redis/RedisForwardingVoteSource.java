package com.vexsoftware.votifier.support.forwarding.redis;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSource;
import com.vexsoftware.votifier.util.GsonInst;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;
import java.net.URISyntaxException;

import static com.vexsoftware.votifier.util.RedisPoolUtils.getJedisConfiguration;

/**
 * @author AkramL, azurejelly
 */
public class RedisForwardingVoteSource implements ForwardingVoteSource {

    private final JedisPool pool;
    private final String channel;
    private final LoggingAdapter logger;

    private static final int REDIS_TIMEOUT_MS = 5000;

    public RedisForwardingVoteSource(RedisCredentials credentials, LoggingAdapter logger) {
        this.channel = credentials.getChannel();
        this.logger = logger;

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
