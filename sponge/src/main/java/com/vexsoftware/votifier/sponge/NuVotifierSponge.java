package com.vexsoftware.votifier.sponge;

import com.google.inject.Inject;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.sponge.commands.TestVoteCommand;
import com.vexsoftware.votifier.sponge.commands.VotifierReloadCommand;
import com.vexsoftware.votifier.sponge.configuration.SpongeConfig;
import com.vexsoftware.votifier.sponge.configuration.loader.ConfigLoader;
import com.vexsoftware.votifier.sponge.event.VotifierEvent;
import com.vexsoftware.votifier.sponge.platform.forwarding.SpongePluginMessagingForwardingSink;
import com.vexsoftware.votifier.sponge.platform.logger.SLF4JLogger;
import com.vexsoftware.votifier.sponge.platform.scheduler.SpongeScheduler;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.support.forwarding.redis.RedisCredentials;
import com.vexsoftware.votifier.support.forwarding.redis.RedisForwardingSink;
import com.vexsoftware.votifier.util.KeyCreator;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

@Plugin(id = "nuvotifier", name = "NuVotifier", version = "@version@", authors = "Ichbinjoe",
        description = "Safe, smart, and secure Votifier server plugin")
public class NuVotifierSponge implements VoteHandler, VotifierPlugin, ForwardedVoteListener {

    @Inject
    public Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    public File configDir;

    /**
     * The server bootstrap.
     */
    private VotifierServerBootstrap bootstrap;

    /**
     * The RSA key pair.
     */
    private KeyPair keyPair;

    /**
     * Debug mode flag
     */
    private boolean debug;

    /**
     * Keys used for websites.
     */
    private final Map<String, Key> tokens = new HashMap<>();

    private ForwardingVoteSink forwardingMethod;
    private LoggingAdapter loggerAdapter;
    private VotifierScheduler scheduler;

    private boolean loadAndBind() {
        // Load configuration.
        ConfigLoader.loadConfig(this);

        /*
         * Create RSA directory and keys if it does not exist; otherwise, read
         * keys.
         */
        File rsaDirectory = new File(configDir, "rsa");
        try {
            if (!rsaDirectory.exists()) {
                if (!rsaDirectory.mkdir()) {
                    throw new RuntimeException("Unable to create the RSA key folder " + rsaDirectory);
                }

                this.keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDirectory, keyPair);
            } else {
                this.keyPair = RSAIO.load(rsaDirectory);
            }
        } catch (Exception ex) {
            logger.error("Error creating or reading RSA tokens", ex);
            return false;
        }

        this.debug = ConfigLoader.getSpongeConfig().debug;

        // Load Votifier tokens.
        ConfigLoader.getSpongeConfig().tokens.forEach((w, t) -> {
            tokens.put(w, KeyCreator.createKeyFrom(t));
            logger.info("Loaded token for website: {}", w);
        });

        // Initialize the receiver.
        final String host = ConfigLoader.getSpongeConfig().host;
        final int port = ConfigLoader.getSpongeConfig().port;

        if (!debug) {
            logger.info("QUIET mode enabled!");
        }

        if (port >= 0) {
            final boolean disableV1 = ConfigLoader.getSpongeConfig().disableV1Protocol;
            if (disableV1) {
                logger.info("------------------------------------------------------------------------------");
                logger.info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
                logger.info("currently support the modern Votifier protocol in NuVotifier.");
                logger.info("------------------------------------------------------------------------------");
            }

            this.bootstrap = new VotifierServerBootstrap(host, port, this, disableV1);
            this.bootstrap.start(err -> {});
        } else {
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Your Votifier port is less than 0, so we assume you do NOT want to start the");
            getLogger().info("votifier port server! Votifier will not listen for votes over any port, and");
            getLogger().info("will only listen for forwarded votes!");
            getLogger().info("------------------------------------------------------------------------------");
        }

        SpongeConfig config = ConfigLoader.getSpongeConfig();

        if (config.forwarding != null) {
            String method = config.forwarding.method.toLowerCase();
            switch (method) {
                case "none": {
                    logger.info("Method none selected for vote forwarding: Votes will not be received from a forwarder.");
                    break;
                }
                case "pluginmessaging": {
                    String channel = config.forwarding.pluginMessaging.channel;

                    try {
                        this.forwardingMethod = new SpongePluginMessagingForwardingSink(this, channel, this);
                        getLogger().info("Receiving votes over plugin messaging channel '{}'", channel);
                    } catch (RuntimeException ex) {
                        logger.error("NuVotifier could not set up plugin messaging for vote forwarding!", ex);
                    }

                    break;
                }
                case "redis": {
                    String channel = config.forwarding.redis.channel;
                    RedisCredentials credentials = RedisCredentials.builder()
                            .host(config.forwarding.redis.address)
                            .port(config.forwarding.redis.port)
                            .username(config.forwarding.redis.username)
                            .password(config.forwarding.redis.password)
                            .uri(config.forwarding.redis.uri)
                            .channel(channel)
                            .build();

                    this.forwardingMethod = new RedisForwardingSink(credentials, this, loggerAdapter);
                    logger.info("Receiving votes over Redis channel '{}'.", channel);
                }
                default: {
                    logger.error("No vote forwarding method '{}' known. Defaulting to noop implementation.", method);
                }
            }
        }
        return true;
    }

    private void halt() {
        // Shut down the network handlers.
        if (bootstrap != null) {
            bootstrap.shutdown();
            bootstrap = null;
        }

        if (forwardingMethod != null) {
            forwardingMethod.halt();
            forwardingMethod = null;
        }
    }

    public boolean reload() {
        try {
            halt();
        } catch (Exception ex) {
            logger.error("On halt, an exception was thrown. This may be fine!", ex);
        }

        if (loadAndBind()) {
            logger.info("Reload was successful.");
            return true;
        } else {
            try {
                halt();
                logger.error("On reload, there was a problem with the configuration. Votifier currently does nothing!");
            } catch (Exception ex) {
                logger.error("On reload, there was a problem loading, and we could not re-halt the server." +
                        "Votifier is in an unstable state!", ex);
            }

            return false;
        }
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        this.scheduler = new SpongeScheduler(this);
        this.loggerAdapter = new SLF4JLogger(logger);

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Reloads NuVotifier"))
                .permission("nuvotifier.reload")
                .executor(new VotifierReloadCommand(this))
                .build(), "nvreload");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .arguments(GenericArguments.allOf(GenericArguments.string(Text.of("args"))))
                .description(Text.of("Sends a test vote to the server's listeners"))
                .permission("nuvotifier.testvote")
                .executor(new TestVoteCommand(this))
                .build(), "testvote");

        if (!loadAndBind()) {
            logger.error("Votifier did not initialize properly!");
        }
    }

    @Listener
    public void onGameReload(GameReloadEvent event) {
        this.reload();
    }

    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
        this.halt();
        logger.info("Votifier disabled.");
    }

    @Override
    public void onVoteReceived(final Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) {
        if (debug) {
            if (protocolVersion == VotifierSession.ProtocolVersion.ONE) {
                logger.info("Got a protocol v1 vote record from {} -> {}", remoteAddress, vote);
            } else {
                logger.info("Got a protocol v2 vote record from {} -> {}", remoteAddress, vote);
            }
        }

        this.fireVoteEvent(vote);
    }

    @Override
    public void onError(Throwable t, boolean alreadyHandledVote, String remoteAddress) {
        if (debug) {
            if (alreadyHandledVote) {
                logger.warn("Vote processed, however an exception occurred with a vote from {}", remoteAddress, t);
            } else {
                logger.warn("Unable to process vote from {}", remoteAddress, t);
            }
        } else if (!alreadyHandledVote) {
            logger.warn("Unable to process vote from {}", remoteAddress);
        }
    }

    @Override
    public void onForward(final Vote v) {
        if (debug) {
            logger.info("Got a forwarded vote -> {}", v);
        }

        this.fireVoteEvent(v);
    }

    private void fireVoteEvent(final Vote vote) {
        Sponge.getScheduler()
                .createTaskBuilder()
                .execute(() -> {
                    VotifierEvent event = new VotifierEvent(vote, Sponge.getCauseStackManager().getCurrentCause());
                    Sponge.getEventManager().post(event);
                }).submit(this);
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public LoggingAdapter getPluginLogger() {
        return loggerAdapter;
    }

    @Override
    public VotifierScheduler getScheduler() {
        return scheduler;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public Map<String, Key> getTokens() {
        return tokens;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return keyPair;
    }

    public File getConfigDir() {
        return configDir;
    }
}
