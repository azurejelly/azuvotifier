package com.vexsoftware.votifier.redis.pool;

import com.vexsoftware.votifier.redis.RedisCredentials;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

public final class RedisPoolFactory {

    private static final int REDIS_TIMEOUT_MS = 5000;

    public static JedisPoolConfig config() {
        JedisPoolConfig jedisConfig = new JedisPoolConfig();
        jedisConfig.setMaxIdle(20);
        jedisConfig.setMinIdle(6);
        jedisConfig.setMaxWait(Duration.ofSeconds(15));
        jedisConfig.setMaxTotal(256);
        jedisConfig.setTestWhileIdle(true);
        jedisConfig.setMinEvictableIdleDuration(Duration.ofSeconds(60));
        jedisConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        jedisConfig.setNumTestsPerEvictionRun(-1);
        jedisConfig.setBlockWhenExhausted(true);
        return jedisConfig;
    }

    public static JedisPool create(JedisPoolConfig config, RedisCredentials credentials) {
        if (credentials.getURI() != null && !credentials.getURI().isBlank()) {
            try {
                URI uri = new URI(credentials.getURI().trim());
                return new JedisPool(config, uri, REDIS_TIMEOUT_MS);
            } catch (URISyntaxException e) {
                throw new RuntimeException("The Redis URI is invalid and a forwarding vote source cannot be built", e);
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
                    return new JedisPool(config, hostname, port, REDIS_TIMEOUT_MS, username, password);
                } else {
                    return new JedisPool(config, hostname, port, REDIS_TIMEOUT_MS, username);
                }
            } else {
                if (hasPassword) {
                    return new JedisPool(config, hostname, port, REDIS_TIMEOUT_MS, password);
                } else {
                    return new JedisPool(config, hostname, port, REDIS_TIMEOUT_MS);
                }
            }
        }
    }

    public static JedisPool create(RedisCredentials credentials) {
        return create(config(), credentials);
    }
}
