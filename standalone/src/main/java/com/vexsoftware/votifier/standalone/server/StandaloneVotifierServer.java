package com.vexsoftware.votifier.standalone.server;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.network.VotifierServerBootstrap;
import com.vexsoftware.votifier.network.protocol.session.VotifierSession;
import com.vexsoftware.votifier.platform.forwarding.source.ForwardingVoteSource;
import com.vexsoftware.votifier.platform.forwarding.source.proxy.ProxyForwardingVoteSource;
import com.vexsoftware.votifier.platform.forwarding.source.redis.RedisForwardingVoteSource;
import com.vexsoftware.votifier.platform.logger.LoggingAdapter;
import com.vexsoftware.votifier.platform.logger.impl.SLF4JLoggingAdapter;
import com.vexsoftware.votifier.platform.plugin.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.platform.scheduler.impl.StandaloneVotifierScheduler;
import com.vexsoftware.votifier.redis.RedisCredentials;
import com.vexsoftware.votifier.standalone.config.redis.RedisVotifierConfiguration;
import com.vexsoftware.votifier.standalone.config.server.ForwardableServer;
import com.vexsoftware.votifier.util.TokenUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class StandaloneVotifierServer implements VotifierPlugin {

    private final RedisVotifierConfiguration redis;
    private final boolean debug;
    private final Map<String, Key> tokens;
    private final KeyPair v1Key;
    private final InetSocketAddress socket;
    private final VotifierScheduler scheduler;
    private final LoggingAdapter logger;
    private final Map<String, ForwardableServer> backendServers;
    private final boolean disableV1Protocol;
    private ForwardingVoteSource forwardingMethod;
    private VotifierServerBootstrap bootstrap;

    public StandaloneVotifierServer(
            boolean debug, Map<String, Key> tokens,
            KeyPair v1Key, InetSocketAddress socket,
            Map<String, ForwardableServer> backendServers,
            boolean disableV1Protocol,
            RedisVotifierConfiguration redis
    ) {
        this.debug = debug;
        this.socket = socket;
        this.tokens = Map.copyOf(tokens);
        this.v1Key = v1Key;
        this.backendServers = backendServers;
        this.scheduler = new StandaloneVotifierScheduler();
        this.logger = new SLF4JLoggingAdapter(getClass());
        this.disableV1Protocol = disableV1Protocol;
        this.redis = redis;
    }

    public void start(Consumer<Throwable> error) {
        this.bootstrap = new VotifierServerBootstrap(socket.getHostString(), socket.getPort(), this, disableV1Protocol);
        this.bootstrap.start(error);
        this.makeForwardingSource(backendServers);
    }

    public void halt() {
        if (bootstrap != null) {
            bootstrap.shutdown();
            bootstrap = null;
        }

        if (forwardingMethod != null) {
            forwardingMethod.halt();
            forwardingMethod = null;
        }
    }

    private void makeForwardingSource(Map<String, ForwardableServer> backendServers) {
        if (redis != null && redis.isEnabled()) {
            RedisCredentials redisCredentials = RedisCredentials.builder()
                    .host(redis.getAddress())
                    .port(redis.getPort())
                    .username(redis.getUsername())
                    .password(redis.getPassword())
                    .uri(redis.getUri())
                    .channel(redis.getChannel())
                    .build();

            this.forwardingMethod = new RedisForwardingVoteSource(
                    redisCredentials, getPluginLogger()
            );

            try {
                this.forwardingMethod.init();
            } catch (RuntimeException ex) {
                getPluginLogger().error("Could not set up Redis for vote forwarding", ex);
            }
        } else {
            List<ProxyForwardingVoteSource.BackendServer> serverList = new ArrayList<>();
            for (Map.Entry<String, ForwardableServer> entry : backendServers.entrySet()) {
                String key = entry.getKey();
                ForwardableServer server = entry.getValue();

                InetAddress address;
                try {
                    address = InetAddress.getByName(server.getAddress());
                } catch (UnknownHostException ex) {
                    getPluginLogger().warn("Couldn't look up {} for server '{}'. Ignoring!", server.getAddress(), key);
                    continue;
                }

                Key token;
                try {
                    token = TokenUtil.toKey(server.getToken());
                } catch (IllegalArgumentException ex) {
                    getPluginLogger().error("Could not add proxy target '{}'. Is the provided token valid?" +
                            "Votes will not be forwarded to this server!", key, ex);

                    continue;
                }

                InetSocketAddress socket = new InetSocketAddress(address, server.getPort());
                serverList.add(new ProxyForwardingVoteSource.BackendServer(key, socket, token));
            }

            if (!serverList.isEmpty()) {
                getPluginLogger().info(
                        "Forwarding votes from this NuVotifier instance to another {} valid backend servers.",
                        serverList.size()
                );
            }

            this.forwardingMethod = bootstrap.createForwardingSource(serverList, null);
            this.forwardingMethod.init();
        }
    }

    @Override
    public Map<String, Key> getTokens() {
        return tokens;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return v1Key;
    }

    @Override
    public LoggingAdapter getPluginLogger() {
        return logger;
    }

    @Override
    public VotifierScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) {
        if (debug) {
            logger.info("Received protocol {} vote record for username {} from service {} @ {}",
                    protocolVersion.getHumanReadable(),
                    vote.getUsername(),
                    vote.getServiceName(),
                    vote.getAddress()
            );
        }

        if (forwardingMethod != null) {
            forwardingMethod.forward(vote);
        }
    }

    @Override
    public void onError(Throwable t, boolean alreadyHandledVote, String remoteAddress) {
        if (debug) {
            if (alreadyHandledVote) {
                logger.warn("Vote processed, however an exception occurred with a vote from " + remoteAddress, t);
            } else {
                logger.warn("Unable to process vote from " + remoteAddress, t);
            }
        } else if (!alreadyHandledVote) {
            logger.warn("Unable to process vote from " + remoteAddress);
        }
    }

    @Override
    public boolean isDebug() {
        return debug;
    }
}
