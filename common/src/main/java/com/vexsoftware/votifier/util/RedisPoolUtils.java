package com.vexsoftware.votifier.util;

import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

public class RedisPoolUtils {

    public static JedisPoolConfig getJedisConfiguration() {
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
}
