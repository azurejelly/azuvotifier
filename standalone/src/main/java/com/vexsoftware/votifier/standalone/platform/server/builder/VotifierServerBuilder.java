package com.vexsoftware.votifier.standalone.platform.server.builder;

import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.standalone.config.redis.RedisVotifierConfiguration;
import com.vexsoftware.votifier.standalone.config.server.BackendServer;
import com.vexsoftware.votifier.standalone.platform.server.StandaloneVotifierServer;
import com.vexsoftware.votifier.util.KeyCreator;

import java.io.File;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.security.interfaces.RSAKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VotifierServerBuilder {

    private final Map<String, Key> keyMap = new HashMap<>();
    private KeyPair v1Key;
    private InetSocketAddress bind;
    private Map<String, BackendServer> servers;
    private boolean debug;
    private boolean disableV1Protocol;
    private RedisVotifierConfiguration redis;

    public VotifierServerBuilder addToken(String service, String token) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(token, "key");

        keyMap.put(service, KeyCreator.createKeyFrom(token));
        return this;
    }

    public VotifierServerBuilder v1Key(KeyPair v1Key) {
        this.v1Key = Objects.requireNonNull(v1Key, "v1Key");
        if (!(v1Key.getPrivate() instanceof RSAKey)) {
            throw new IllegalArgumentException("Provided key is not an RSA key.");
        }
        return this;
    }

    public VotifierServerBuilder v1KeyFolder(File file) throws Exception {
        this.v1Key = RSAIO.load(Objects.requireNonNull(file, "file"));
        return this;
    }

    public VotifierServerBuilder bind(InetSocketAddress bind) {
        this.bind = Objects.requireNonNull(bind, "bind");
        return this;
    }

    public VotifierServerBuilder debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public VotifierServerBuilder backendServers(Map<String, BackendServer> servers) {
        this.servers = servers;
        return this;
    }

    public VotifierServerBuilder disableV1Protocol(boolean disableV1Protocol) {
        this.disableV1Protocol = disableV1Protocol;
        return this;
    }

    public VotifierServerBuilder redis(RedisVotifierConfiguration cfg) {
        this.redis = cfg;
        return this;
    }

    public StandaloneVotifierServer create() {
        Objects.requireNonNull(bind, "need an address to bind to");
        Objects.requireNonNull(servers, "need a list of servers to forward votes for");
        return new StandaloneVotifierServer(debug, keyMap, v1Key, bind, servers, disableV1Protocol, redis);
    }
}
