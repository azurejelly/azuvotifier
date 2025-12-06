package com.vexsoftware.votifier.fabric.platform;

import com.vexsoftware.votifier.fabric.Votifier;
import com.vexsoftware.votifier.fabric.configuration.FabricConfig;
import com.vexsoftware.votifier.fabric.configuration.loader.ConfigLoader;
import com.vexsoftware.votifier.fabric.event.VoteListener;
import com.vexsoftware.votifier.fabric.platform.logger.FabricLoggerAdapter;
import com.vexsoftware.votifier.fabric.provider.MinecraftServerProvider;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.platform.scheduler.impl.StandaloneVotifierScheduler;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.util.KeyCreator;
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
import java.util.concurrent.Executors;

public class FabricVotifierPlugin implements VotifierPlugin, ForwardedVoteListener {

    private final MinecraftServer server;
    private final Logger logger;
    private final LoggingAdapter loggerAdapter;
    private final File configDir;
    private final Map<String, Key> tokens;
    private final VotifierScheduler scheduler;

    private FabricConfig config;
    private VotifierServerBootstrap bootstrap;
    private ForwardingVoteSink forwardingSink;
    private boolean debug;
    private KeyPair keyPair;

    public FabricVotifierPlugin() {
        this.server = MinecraftServerProvider.getServer();
        this.logger = Votifier.getInstance().getLogger();
        this.loggerAdapter = new FabricLoggerAdapter(logger);
        this.configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "azuvotifier");
        this.scheduler = new StandaloneVotifierScheduler(Executors.newScheduledThreadPool(1));
        this.tokens = new HashMap<>();
    }

    public void init() {
        try {
            config = ConfigLoader.loadFrom(configDir);
        } catch (IOException | RuntimeException ex) {
            logger.error("Failed to create or load configuration file!", ex);
            return;
        }

        File rsaDirectory = new File(configDir, "rsa");
        try {
            if (!rsaDirectory.exists()) {
                if (!rsaDirectory.mkdirs()) {
                    throw new RuntimeException("Failed to create RSA key folder at " + rsaDirectory);
                }

                this.keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDirectory, keyPair);
            } else {
                this.keyPair = RSAIO.load(rsaDirectory);
            }
        } catch (Exception ex) {
            logger.error("Error creating or reading RSA tokens", ex);
        }

        this.debug = config.debug;
        config.tokens.forEach((s, t) -> {
            tokens.put(s, KeyCreator.createKeyFrom(t));
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
            logger.info("will only listen for pluginMessaging forwarded votes!");
            logger.info("------------------------------------------------------------------------------");
        }

        if (config.forwarding != null) {
            String method = config.forwarding.method;
            switch (method.toLowerCase()) {
                case "none": {
                    logger.info("Method none selected for vote forwarding: Votes will not be received from a forwarder.");
                    break;
                }
                case "redis": {
                    logger.warn("Redis is not implemented");
                    break;
                }
                case "pluginmessaging": {
                    logger.warn("Plugin messaging is not implemented");
                    break;
                }
                default: {
                    logger.error("No vote forwarding method '{}' known!", method);
                }
            }
        }
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
        return loggerAdapter;
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
        if (config.experimental.skipOfflinePlayers) {
            String username = vote.getUsername();
            var player = server.getPlayerManager().getPlayer(username);

            if (player == null) {
                logger.info("Skipping vote from {} on this server as player is offline.", username);
                return;
            }
        }

        server.submitAndJoin(() -> VoteListener.EVENT.invoker().onVote(vote));
    }
}
