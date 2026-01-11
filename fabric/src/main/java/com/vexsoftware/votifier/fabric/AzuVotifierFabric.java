package com.vexsoftware.votifier.fabric;

import com.vexsoftware.votifier.network.VotifierServerBootstrap;
import com.vexsoftware.votifier.util.CryptoUtil;
import com.vexsoftware.votifier.fabric.configuration.FabricConfig;
import com.vexsoftware.votifier.fabric.configuration.loader.ConfigLoader;
import com.vexsoftware.votifier.fabric.event.VoteListener;
import com.vexsoftware.votifier.fabric.event.listener.CommandRegistrationCallbackListener;
import com.vexsoftware.votifier.fabric.event.listener.DefaultVoteListener;
import com.vexsoftware.votifier.fabric.platform.forwarding.FabricMessagingForwardingSink;
import com.vexsoftware.votifier.fabric.platform.provider.MinecraftServerProvider;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.network.protocol.session.VotifierSession;
import com.vexsoftware.votifier.platform.plugin.VotifierPlugin;
import com.vexsoftware.votifier.platform.logger.LoggingAdapter;
import com.vexsoftware.votifier.platform.logger.impl.SLF4JLoggingAdapter;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.platform.scheduler.impl.StandaloneVotifierScheduler;
import com.vexsoftware.votifier.platform.forwarding.listener.ForwardedVoteListener;
import com.vexsoftware.votifier.platform.forwarding.sink.ForwardingVoteSink;
import com.vexsoftware.votifier.redis.RedisCredentials;
import com.vexsoftware.votifier.platform.forwarding.sink.redis.RedisForwardingSink;
import com.vexsoftware.votifier.util.TokenUtil;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

public class AzuVotifierFabric implements DedicatedServerModInitializer, VotifierPlugin, ForwardedVoteListener {

    private static AzuVotifierFabric instance;

    private Logger logger;
    private LoggingAdapter loggingAdapter;
    private Map<String, Key> tokens;
    private VotifierScheduler scheduler;
    private FabricConfig config;
    private VotifierServerBootstrap bootstrap;
    private ForwardingVoteSink forwardingSink;
    private KeyPair keyPair;
    private boolean debug;

    @Override
    public void onInitializeServer() {
        instance = this;
        logger = LoggerFactory.getLogger(AzuVotifierFabric.class);

        ServerLifecycleEvents.SERVER_STARTING.register(this::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::stop);
        CommandRegistrationCallback.EVENT.register(new CommandRegistrationCallbackListener());
        VoteListener.EVENT.register(new DefaultVoteListener());
    }

    public void start(MinecraftServer server) {
        MinecraftServerProvider.setServer(server);

        if (init()) {
            return;
        }

        logger.error("azuvotifier did not initialize properly!");
    }

    public void stop(MinecraftServer server) {
        halt();
        logger.info("azuvotifier disabled.");
    }

    public static AzuVotifierFabric getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean init() {
        loggingAdapter = new SLF4JLoggingAdapter(logger);
        tokens = new HashMap<>();
        scheduler = new StandaloneVotifierScheduler();

        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "azuvotifier");
        try {
            config = ConfigLoader.loadFrom(configDir);
        } catch (IOException | RuntimeException ex) {
            logger.error("Failed to create or load configuration file!", ex);
            return false;
        }

        File rsaDirectory = new File(configDir, "rsa");
        try {
            if (!rsaDirectory.exists()) {
                if (!rsaDirectory.mkdirs()) {
                    logger.error("Failed to create RSA key folder at {}", rsaDirectory);
                    return false;
                }

                this.keyPair = CryptoUtil.generateKeyPair(2048);
                CryptoUtil.save(rsaDirectory, keyPair);
            } else {
                this.keyPair = CryptoUtil.load(rsaDirectory);
            }
        } catch (Exception ex) {
            logger.error("Error creating or reading RSA tokens", ex);
            return false;
        }

        this.debug = config.debug;
        config.tokens.forEach((s, t) -> {
            tokens.put(s, TokenUtil.toKey(t));
            logger.info("Loaded token for website {}", s);
        });

        String host = config.host;
        int port = config.port;

        if (port >= 0) {
            boolean disableV1 = config.disableV1Protocol;
            if (disableV1) {
                logger.info("------------------------------------------------------------------------------");
                logger.info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
                logger.info("currently support the modern Votifier protocol in azuvotifier.");
                logger.info("------------------------------------------------------------------------------");
            }

            this.bootstrap = new VotifierServerBootstrap(host, port, this, disableV1);
            this.bootstrap.start((err) -> {});
        } else {
            logger.info("------------------------------------------------------------------------------");
            logger.info("Your Votifier port is less than 0, so we assume you do NOT want to start the");
            logger.info("Votifier port server! Votifier will not listen for votes over any port, and");
            logger.info("will only listen for forwarded votes!");
            logger.info("------------------------------------------------------------------------------");
        }

        if (config.forwarding != null) {
            String method = config.forwarding.method;
            switch (method.toLowerCase()) {
                case "none": {
                    logger.info("Votifier will not receive votes from a forwarder.");
                    break;
                }
                case "redis": {
                    try {
                        forwardingSink = new RedisForwardingSink(
                                RedisCredentials.builder()
                                        .host(config.forwarding.redis.address)
                                        .port(config.forwarding.redis.port)
                                        .username(config.forwarding.redis.username)
                                        .password(config.forwarding.redis.password)
                                        .uri(config.forwarding.redis.uri)
                                        .channel(config.forwarding.redis.channel)
                                        .build(), this, loggingAdapter
                        );

                        forwardingSink.init();
                    } catch (RuntimeException ex) {
                        logger.error("Could not set up Redis for vote forwarding", ex);
                        return false;
                    }

                    logger.info("Votifier will use Redis to receive forwarded votes.");
                    break;
                }
                case "plugin-messaging":
                case "pluginmessaging": {
                    var channel = config.forwarding.pluginMessaging.channel;

                    try {
                        forwardingSink = new FabricMessagingForwardingSink(channel, this, loggingAdapter);
                        forwardingSink.init();
                    } catch (RuntimeException ex) {
                        logger.error("Could not set up plugin messaging for vote forwarding", ex);
                        return false;
                    }

                    logger.info("Votifier will receive forwarded votes over the \"{}\" plugin messaging channel.", channel);
                    break;
                }
                default: {
                    logger.error("No vote forwarding method '{}' known! Votifier will not receive forwarded votes.", method);
                    break;
                }
            }
        }

        return true;
    }

    public void halt() {
        if (bootstrap != null) {
            bootstrap.shutdown();
            bootstrap = null;
        }

        if (forwardingSink != null) {
            forwardingSink.halt();
            forwardingSink = null;
        }
    }

    @Override
    public Map<String, Key> getTokens() {
        return tokens;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return keyPair;
    }

    @Override
    public LoggingAdapter getPluginLogger() {
        return loggingAdapter;
    }

    @Override
    public VotifierScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion proto, String address) {
        if (debug) {
            if (proto == VotifierSession.ProtocolVersion.ONE) {
                logger.info("Got a protocol v1 vote record from {} -> {}", address, vote);
            } else {
                logger.info("Got a protocol v2 vote record from {} -> {}", address, vote);
            }
        }

        fireVoteEvent(vote);
    }

    @Override
    public void onForward(Vote v) {
        if (debug) {
            logger.info("Got a forwarded vote -> {}", v);
        }

        fireVoteEvent(v);
    }

    @Override
    public void onError(Throwable t, boolean voteAlreadyCompleted, String address) {
        if (debug) {
            if (voteAlreadyCompleted) {
                logger.warn("Vote processed, however an exception occurred with a vote from {}", address, t);
            } else {
                logger.warn("Unable to process vote from {}", address, t);
            }
        } else if (!voteAlreadyCompleted) {
            logger.warn("Unable to process vote from {}", address);
        }
    }

    private void fireVoteEvent(Vote vote) {
        var server = MinecraftServerProvider.getServer();

        if (config.experimental.skipOfflinePlayers) {
            String username = vote.getUsername();
            var player = server.getPlayerManager().getPlayer(username);

            if (player == null) {
                logger.info("Skipping vote from {} on this server as the player is offline.", username);
                return;
            }
        }

        server.submitAndJoin(() -> VoteListener.EVENT.invoker().onVote(vote));
    }
}
