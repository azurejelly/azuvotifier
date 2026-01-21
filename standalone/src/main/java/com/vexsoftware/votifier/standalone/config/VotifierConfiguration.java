package com.vexsoftware.votifier.standalone.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vexsoftware.votifier.standalone.config.redis.RedisVotifierConfiguration;
import com.vexsoftware.votifier.standalone.config.server.ForwardableServer;
import com.vexsoftware.votifier.util.TokenUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class VotifierConfiguration {

    private final String host;
    private final int port;
    private final boolean debug;

    @JsonProperty("check-for-updates")
    private final boolean updateCheckingEnabled;
    private final Map<String, String> tokens;
    private final RedisVotifierConfiguration redis;

    @JsonProperty("forwarding")
    private final Map<String, ForwardableServer> forwardableServers;

    @JsonProperty("disable-v1-protocol")
    private final boolean v1ProtocolDisabled;

    public VotifierConfiguration() {
        this.host = "0.0.0.0";
        this.port = 8192;
        this.debug = false;
        this.updateCheckingEnabled = true;
        this.tokens = new HashMap<>();
        this.tokens.put("default", TokenUtil.newToken());
        this.redis = new RedisVotifierConfiguration();
        this.v1ProtocolDisabled = false;
        this.forwardableServers = new HashMap<>();
    }
}
