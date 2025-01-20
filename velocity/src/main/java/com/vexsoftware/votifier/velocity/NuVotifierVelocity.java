package com.vexsoftware.votifier.velocity;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.ProxyVotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSource;
import com.vexsoftware.votifier.support.forwarding.ServerFilter;
import com.vexsoftware.votifier.support.forwarding.cache.FileVoteCache;
import com.vexsoftware.votifier.support.forwarding.cache.MemoryVoteCache;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.support.forwarding.proxy.ProxyForwardingVoteSource;
import com.vexsoftware.votifier.support.forwarding.redis.RedisCredentials;
import com.vexsoftware.votifier.support.forwarding.redis.RedisForwardingVoteSource;
import com.vexsoftware.votifier.util.IOUtil;
import com.vexsoftware.votifier.util.KeyCreator;
import com.vexsoftware.votifier.util.TokenUtil;
import com.vexsoftware.votifier.velocity.commands.TestVoteCommand;
import com.vexsoftware.votifier.velocity.commands.VotifierReloadCommand;
import com.vexsoftware.votifier.velocity.event.VotifierEvent;
import com.vexsoftware.votifier.velocity.forwarding.OnlineForwardPluginMessagingForwardingSource;
import com.vexsoftware.votifier.velocity.forwarding.PluginMessagingForwardingSource;
import com.vexsoftware.votifier.velocity.platform.logger.SLF4JLogger;
import com.vexsoftware.votifier.velocity.platform.scheduler.VelocityScheduler;
import com.vexsoftware.votifier.velocity.platform.server.VelocityBackendServer;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.KeyPair;
import java.util.*;
import java.util.stream.Collectors;

@Plugin(id = "nuvotifier", name = "NuVotifier", version = "@version@", authors = "Ichbinjoe",
        description = "Safe, smart, and secure Votifier server plugin")
public class NuVotifierVelocity implements VoteHandler, ProxyVotifierPlugin {

    @Inject
    public Logger logger;
    private LoggingAdapter loggingAdapter;

    @Inject
    @DataDirectory
    public Path configDir;

    @Inject
    public ProxyServer server;

    private VotifierScheduler scheduler;

    private boolean loadAndBind() {
        Toml config;

        try {
            config = loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration.", e);
        }

        /*
         * Create RSA directory and keys if it does not exist; otherwise, read
         * keys.
         */
        File rsaDirectory = new File(configDir.toFile(), "rsa");
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

        if (config.contains("quiet")) {
            this.debug = !config.getBoolean("quiet");
        } else {
            this.debug = config.getBoolean("debug", true);
        }

        // Load Votifier tokens.
        config.getTable("tokens").toMap().forEach((service, key) -> {
            if (key instanceof String) {
                tokens.put(service, KeyCreator.createKeyFrom((String) key));
                logger.info("Loaded token for website: {}", service);
            }
        });

        // Initialize the receiver.
        final String host = config.getString("host");
        final int port = Math.toIntExact(config.getLong("port"));
        if (!debug) {
            logger.info("QUIET mode enabled!");
        }

        final boolean disableV1 = config.getBoolean("disable-v1-protocol", false);
        if (disableV1) {
            logger.info("------------------------------------------------------------------------------");
            logger.info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
            logger.info("currently support the modern Votifier protocol in NuVotifier.");
            logger.info("------------------------------------------------------------------------------");
        }

        this.bootstrap = new VotifierServerBootstrap(host, port, this, disableV1);
        this.bootstrap.start(err -> {});

        Toml fwd = config.getTable("forwarding");
        String method = fwd.getString("method", "none").toLowerCase();

        switch (method) {
            case "none": {
                getLogger().info("Method none selected for vote forwarding: Votes will not be forwarded to backend servers.");
                return true;
            }
            case "pluginmessaging": {
                Toml table = fwd.getTable("pluginMessaging");
                String channel = table.getString("channel", "NuVotifier");
                String cacheMethod = table.getString("cache", "file").toLowerCase();
                int dumpRate = table.getLong("dumpRate", 5L).intValue();
                VoteCache cache = null;

                switch (cacheMethod) {
                    case "none": {
                        getLogger().info("Vote cache none selected for caching: votes that cannot be immediately delivered will be lost.");
                        break;
                    }
                    case "memory": {
                        long time = fwd.getTable("memory-cache").getLong("cacheTime", -1L);
                        cache = new MemoryVoteCache(this, time);

                        getLogger().info("Using in-memory cache for votes that are not able to be delivered.");
                        break;
                    }
                    case "file": {
                        try {
                            String filename = fwd.getTable("file-cache").getString("name");
                            File file = configDir.resolve(filename).toFile();
                            long time = fwd.getTable("file-cache").getLong("cacheTime", -1L);

                            cache = new FileVoteCache(this, file, time);
                            getLogger().info("Using file cache for votes that are not able to be delivered.");
                        } catch (IOException e) {
                            getLogger().error("Unable to load file cache. Votes will be lost!", e);
                        }

                        break;
                    }
                    default: {
                        getLogger().info("No vote caching method named '{}' known." +
                                " Votes that cannot be immediately delivered will be lost.", method);

                        break;
                    }
                }

                ServerFilter filter = new ServerFilter(
                        table.getList("excludedServers", Collections.emptyList()),
                        table.getBoolean("whitelist", false)
                );

                if (!table.getBoolean("onlySendToJoinedServer")) {
                    try {
                        this.forwardingMethod = new PluginMessagingForwardingSource(
                                channel, filter, this, cache, dumpRate
                        );

                        this.forwardingMethod.init();
                    } catch (RuntimeException e) {
                        getLogger().error("Could not set up plugin messaging for vote forwarding", e);
                        return false;
                    }
                } else {
                    String fallbackServer = table.getString("joinedServerFallback", null);
                    if (fallbackServer != null && fallbackServer.isEmpty()) {
                        fallbackServer = null;
                    }

                    try {
                        this.forwardingMethod = new OnlineForwardPluginMessagingForwardingSource(
                                channel, filter, this, cache, fallbackServer, dumpRate
                        );

                        this.forwardingMethod.init();
                    } catch (RuntimeException e) {
                        getLogger().error("Could not set up plugin messaging for vote forwarding", e);
                        return false;
                    }
                }

                return true;
            }
            case "proxy": {
                Toml table = fwd.getTable("proxy");
                List<ProxyForwardingVoteSource.BackendServer> serverList = new ArrayList<>();
                for (String name : table.toMap().keySet()) {
                    Toml section = table.getTable(name);
                    InetAddress address;

                    try {
                        address = InetAddress.getByName(section.getString("address"));
                    } catch (UnknownHostException e) {
                        getLogger().info("Couldn't look up {}! Ignoring!", section.getString("address"));
                        continue;
                    }

                    Key token = null;
                    try {
                        token = KeyCreator.createKeyFrom(section.getString("token", section.getString("key")));
                    } catch (IllegalArgumentException ex) {
                        getLogger().error("An exception occurred while attempting to add proxy target '{}' - " +
                                "maybe your token is wrong? Votes will not be forwarded to this server!", name, ex);
                    }

                    if (token != null) {
                        int serverPort = Math.toIntExact(section.getLong("port"));
                        InetSocketAddress socket = new InetSocketAddress(address, serverPort);

                        ProxyForwardingVoteSource.BackendServer server = new ProxyForwardingVoteSource.BackendServer(
                                name, socket, token
                        );

                        serverList.add(server);
                    }
                }

                this.forwardingMethod = bootstrap.createForwardingSource(serverList, null);
                getLogger().info("Forwarding votes from this NuVotifier instance to another NuVotifier server.");
                return true;
            }
            case "redis": {
                if (!fwd.containsTable("redis")) {
                    getLogger().error(
                            "Cannot set up Redis forwarding as the 'redis' configuration section is missing "
                                    + "or incomplete. Defaulting to noop implementation."
                    );

                    return false;
                }

                Toml redis = fwd.getTable("redis");

                try {
                    this.forwardingMethod = new RedisForwardingVoteSource(
                            RedisCredentials.builder()
                                    .host(redis.getString("address"))
                                    .port(redis.getLong("port").intValue())
                                    .username(redis.getString("username"))
                                    .password(redis.getString("password"))
                                    .uri(redis.getString("uri"))
                                    .channel(redis.getString("channel"))
                                    .build(),

                            getPluginLogger()
                    );

                    this.forwardingMethod.init();
                    return true;
                } catch (RuntimeException ex) {
                    logger.error("Could not set up Redis for vote forwarding", ex);
                    return false;
                }
            }
            default: {
                getLogger().error("No vote forwarding method '{}' known. Defaulting to noop implementation.", method);
                return false;
            }
        }
    }

    void halt() {
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
            getLogger().error("On halt, an exception was thrown. This may be fine!", ex);
        }

        if (loadAndBind()) {
            getLogger().info("Reload was successful.");
            return true;
        } else {
            try {
                halt();
                getLogger().error("On reload, there was a problem with the configuration. Votifier currently does nothing!");
            } catch (Exception ex) {
                getLogger().error("On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex);
            }

            return false;
        }
    }

    @Subscribe
    public void onServerStart(ProxyInitializeEvent event) {
        this.scheduler = new VelocityScheduler(server, this);
        this.loggingAdapter = new SLF4JLogger(logger);

        this.getServer().getCommandManager().register("pnvreload", new VotifierReloadCommand(this));
        this.getServer().getCommandManager().register("ptestvote", new TestVoteCommand(this));

        if (!loadAndBind()) {
            gracefulExit();
        }
    }

    @Subscribe
    public void onServerStop(ProxyShutdownEvent event) {
        this.halt();
        logger.info("Votifier disabled.");
    }

    @Subscribe
    public void onProxyReload(ProxyReloadEvent event) {
        this.reload();
    }

    public ProxyServer getServer() {
        return server;
    }

    private Toml loadConfig() throws IOException {
        if (!Files.exists(configDir)) {
            Files.createDirectory(configDir);
        }

        Path configPath = configDir.resolve("config.toml");
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            return new Toml().read(reader);
        } catch (NoSuchFileException e) {
            // This is ok. Just copy the default and load that.
            // First time run - do some initialization.
            getLogger().info("Configuring Votifier for the first time...");

            // Initialize the configuration file.
            String cfgStr = new String(
                    IOUtil.readAllBytes(
                            Objects.requireNonNull(NuVotifierVelocity.class.getResourceAsStream("/config.toml"))
                    ), StandardCharsets.UTF_8
            );

            String token = TokenUtil.newToken();
            cfgStr = cfgStr.replace("%ip%", server.getBoundAddress().getAddress().getHostAddress());
            cfgStr = cfgStr.replace("%default_token%", token);

            /*
             * Remind hosted server admins to be sure they have the right
             * port number.
             */
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Assigning NuVotifier to listen on port 8192. If you are hosting BungeeCord on a");
            getLogger().info("shared server please check with your hosting provider to verify that this port");
            getLogger().info("is available for your use. Chances are that your hosting provider will assign");
            getLogger().info("a different port, which you need to specify in config.toml.");
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Assigning NuVotifier to listen to interface 0.0.0.0. This is usually alright,");
            getLogger().info("however, if you want NuVotifier to only listen to one interface for security ");
            getLogger().info("reasons (or you use a shared host), you may change this in the config.toml.");
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Your default Votifier token is {}.", token);
            getLogger().info("You will need to provide this token when you submit your server to a voting");
            getLogger().info("list.");
            getLogger().info("------------------------------------------------------------------------------");

            Files.copy(
                    new ByteArrayInputStream(cfgStr.getBytes(StandardCharsets.UTF_8)),
                    configPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return new Toml().read(cfgStr);
        }
    }

    public Logger getLogger() {
        return logger;
    }

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

    /**
     * Method used to forward votes to downstream servers
     */
    private ForwardingVoteSource forwardingMethod;

    private void gracefulExit() {
        logger.error("Votifier did not initialize properly!");
    }

    @Override
    public LoggingAdapter getPluginLogger() {
        return loggingAdapter;
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

    @Override
    public void onVoteReceived(final Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) {
        if (debug) {
            if (protocolVersion == VotifierSession.ProtocolVersion.ONE) {
                logger.info("Got a protocol v1 vote record from {} -> {}", remoteAddress, vote);
            } else {
                logger.info("Got a protocol v2 vote record from {} -> {}", remoteAddress, vote);
            }
        }

        server.getEventManager().fire(new VotifierEvent(vote)).thenAccept(v -> {
            if (v.getResult().isAllowed() && forwardingMethod != null) {
                forwardingMethod.forward(v.getVote());
            }
        });
    }

    @Override
    public void onError(Throwable throwable, boolean alreadyHandledVote, String remoteAddress) {
        if (debug) {
            if (alreadyHandledVote) {
                logger.warn("Vote processed, however an exception occurred with a vote from {}",
                        remoteAddress, throwable);
            } else {
                logger.warn("Unable to process vote from {}", remoteAddress, throwable);
            }
        } else if (!alreadyHandledVote) {
            logger.warn("Unable to process vote from {}", remoteAddress);
        }
    }

    @Override
    public Collection<BackendServer> getAllBackendServers() {
        return server.getAllServers()
                .stream()
                .map(VelocityBackendServer::new)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BackendServer> getServer(String name) {
        return server.getServer(name).map(VelocityBackendServer::new);
    }
}