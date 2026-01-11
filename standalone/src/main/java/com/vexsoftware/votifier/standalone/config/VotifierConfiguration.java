package com.vexsoftware.votifier.standalone.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vexsoftware.votifier.standalone.config.redis.RedisVotifierConfiguration;
import com.vexsoftware.votifier.standalone.config.server.ForwardableServer;
import com.vexsoftware.votifier.util.TokenUtil;

import java.util.HashMap;
import java.util.Map;

public class VotifierConfiguration {

    private final String host;
    private final int port;
    private final boolean debug;
    private final Map<String, String> tokens;
    private final RedisVotifierConfiguration redis;

    @JsonProperty("forwarding")
    private final Map<String, ForwardableServer> forwardableServers;

    @JsonProperty("disable-v1-protocol")
    private final boolean disableV1Protocol;

    public VotifierConfiguration() {
        this.host = "0.0.0.0";
        this.port = 8192;
        this.debug = false;
        this.tokens = new HashMap<>();
        this.tokens.put("default", TokenUtil.newToken());
        this.redis = new RedisVotifierConfiguration();
        this.disableV1Protocol = false;
        this.forwardableServers = new HashMap<>();
    }

    public VotifierConfiguration(
            String host, int port, boolean debug,
            Map<String, String> tokens, RedisVotifierConfiguration redis,
            boolean disableV1Protocol, Map<String, ForwardableServer> forwardableServers
    ) {
        this.host = host;
        this.port = port;
        this.debug = debug;
        this.tokens = tokens;
        this.redis = redis;
        this.disableV1Protocol = disableV1Protocol;
        this.forwardableServers = forwardableServers;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isDebug() {
        return debug;
    }

    public Map<String, String> getTokens() {
        return tokens;
    }

    public boolean isDisableV1Protocol() {
        return disableV1Protocol;
    }

    public Map<String, ForwardableServer> getForwardableServers() {
        return forwardableServers;
    }

    public RedisVotifierConfiguration getRedis() {
        return redis;
    }
}
