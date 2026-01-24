package com.vexsoftware.votifier.bungee;

import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.bungee.commands.VotifierProxyCommand;
import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import com.vexsoftware.votifier.bungee.listeners.ProxyReloadListener;
import com.vexsoftware.votifier.bungee.platform.forwarding.OnlineForwardPluginMessagingForwardingSource;
import com.vexsoftware.votifier.bungee.platform.forwarding.PluginMessagingForwardingSource;
import com.vexsoftware.votifier.bungee.platform.scheduler.BungeeScheduler;
import com.vexsoftware.votifier.bungee.platform.server.BungeeBackendServer;
import com.vexsoftware.votifier.bungee.util.BungeeConstants;
import com.vexsoftware.votifier.update.UpdateChecker;
import com.vexsoftware.votifier.update.impl.GitHubUpdateChecker;
import com.vexsoftware.votifier.util.CommonConstants;
import com.vexsoftware.votifier.util.CryptoUtil;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.network.VotifierServerBootstrap;
import com.vexsoftware.votifier.network.protocol.session.VotifierSession;
import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.platform.plugin.proxy.ProxyVotifierPlugin;
import com.vexsoftware.votifier.platform.logger.LoggingAdapter;
import com.vexsoftware.votifier.platform.logger.impl.JavaLoggingAdapter;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.platform.forwarding.source.ForwardingVoteSource;
import com.vexsoftware.votifier.platform.forwarding.ServerFilter;
import com.vexsoftware.votifier.cache.file.FileVoteCache;
import com.vexsoftware.votifier.cache.memory.MemoryVoteCache;
import com.vexsoftware.votifier.cache.VoteCache;
import com.vexsoftware.votifier.platform.forwarding.source.proxy.ProxyForwardingVoteSource;
import com.vexsoftware.votifier.redis.RedisCredentials;
import com.vexsoftware.votifier.platform.forwarding.source.redis.RedisForwardingVoteSource;
import com.vexsoftware.votifier.util.IOUtil;
import com.vexsoftware.votifier.util.TokenUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SimplePie;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class NuVotifierBungee extends Plugin implements VoteHandler, ProxyVotifierPlugin {

    /**
     * The server channel.
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

    private UpdateChecker updateChecker;
    private VotifierScheduler scheduler;
    private LoggingAdapter pluginLogger;

    private void loadAndBind() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                throw new RuntimeException("Unable to create the plugin data folder " + getDataFolder());
            }
        }

        // Handle configuration.
        File config = new File(getDataFolder(), "config.yml");
        File rsaDirectory = new File(getDataFolder(), "rsa");
        Configuration configuration;

        if (!config.exists()) {
            try {
                // First time run - do some initialization.
                getLogger().info("Configuring Votifier for the first time...");

                // Initialize the configuration file.
                if (!config.createNewFile()) {
                    throw new IOException("Unable to create the config file at " + config);
                }

                String token = TokenUtil.newToken();
                String cfg = new String(
                        IOUtil.readAllBytes(getResourceAsStream("bungeeConfig.yml")),
                        StandardCharsets.UTF_8
                ).replace("%default_token%", token);

                Files.copy(
                        new ByteArrayInputStream(cfg.getBytes(StandardCharsets.UTF_8)),
                        config.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );

                /*
                 * Remind hosted server admins to be sure they have the right
                 * port number.
                 */
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Assigning NuVotifier to listen on port 8192. If you are running this server");
                getLogger().info("on a shared hosting, make sure to check with your hosting provider to verify");
                getLogger().info("that this port is available for your use. Chances are your hosting provider");
                getLogger().info("will assign a different port, which you'll need to change in the NuVotifier");
                getLogger().info("config.yml file.");
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Assigning NuVotifier to listen to interface 0.0.0.0. This is usually alright,");
                getLogger().info("however, if you want NuVotifier to only listen to one interface for security ");
                getLogger().info("reasons (or you use a shared host), you may change this in the config.yml.");
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Your default Votifier token is " + token + ".");
                getLogger().info("You will need to provide this token when you submit your server to a voting");
                getLogger().info("list.");
                getLogger().info("------------------------------------------------------------------------------");
            } catch (Exception ex) {
                throw new RuntimeException("Unable to create configuration file", ex);
            }
        }

        try {
            // Load the configuration.
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(config);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }

        /*
         * Create RSA directory and keys if it does not exist; otherwise, read keys.
         */
        try {
            if (!rsaDirectory.exists()) {
                if (!rsaDirectory.mkdir()) {
                    throw new RuntimeException("Unable to create the RSA key folder " + rsaDirectory);
                }

                this.keyPair = CryptoUtil.generateKeyPair(2048);
                CryptoUtil.save(rsaDirectory, keyPair);
            } else {
                this.keyPair = CryptoUtil.load(rsaDirectory);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error reading RSA tokens", ex);
        }

        // Load Votifier tokens.
        Configuration tokenSection = configuration.getSection("tokens");

        if (configuration.get("tokens") != null) {
            for (String s : tokenSection.getKeys()) {
                tokens.put(s, TokenUtil.toKey(tokenSection.getString(s)));
                getLogger().info("Loaded token for website: " + s);
            }
        } else {
            String token = TokenUtil.newToken();
            configuration.set("tokens", Collections.singletonMap("default", token));
            tokens.put("default", TokenUtil.toKey(token));

            try {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, config);
            } catch (IOException e) {
                throw new RuntimeException("Error saving Votifier token", e);
            }

            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("No tokens were found in your configuration, so we've generated one for you.");
            getLogger().info("Your default Votifier token is " + token + ".");
            getLogger().info("You will need to provide this token when you submit your server to a voting");
            getLogger().info("list.");
            getLogger().info("------------------------------------------------------------------------------");
        }

        // Initialize the receiver.
        final String host = configuration.getString("host", "0.0.0.0");
        final int port = configuration.getInt("port", 8192);

        if (configuration.get("quiet") != null) {
            this.debug = !configuration.getBoolean("quiet");
        } else {
            this.debug = configuration.getBoolean("debug", true);
        }

        if (!debug) {
            getLogger().info("QUIET mode enabled!");
        }

        final boolean disableV1 = configuration.getBoolean("disable-v1-protocol");
        if (disableV1) {
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
            getLogger().info("currently support the modern Votifier protocol in NuVotifier.");
            getLogger().info("------------------------------------------------------------------------------");
        }

        // Must set up server asynchronously due to BungeeCord goofiness.
        FutureTask<?> initTask = new FutureTask<>(Executors.callable(() -> {
            this.bootstrap = new VotifierServerBootstrap(host, port, NuVotifierBungee.this, disableV1);
            this.bootstrap.start(err -> {});
        }));

        getProxy().getScheduler().runAsync(this, initTask);

        try {
            initTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Unable to start server", ex);
        }

        Configuration fwd = configuration.getSection("forwarding");
        String method = fwd.getString("method", "none").toLowerCase();

        if (configuration.getBoolean("bstats", true)) {
            Metrics metrics = new Metrics(this, BungeeConstants.BSTATS_ID);
            metrics.addCustomChart(new SimplePie("forwarding_method", () -> method));
        }

        switch (method) {
            case "none": {
                getLogger().info("Method none selected for vote forwarding: Votes will not be forwarded to backend servers.");
                break;
            }
            case "pluginmessaging": {
                Configuration section = fwd.getSection("pluginMessaging");
                String channel = section.getString("channel", "NuVotifier");
                String cacheMethod = section.getString("cache", "file").toLowerCase();
                VoteCache cache = null;

                switch (cacheMethod) {
                    case "none": {
                        getLogger().info("Vote cache 'none' selected: votes that cannot be immediately delivered will be lost.");
                        break;
                    }
                    case "memory": {
                        cache = new MemoryVoteCache(
                                this,
                                section.getInt("memory.cacheTime", -1)
                        );

                        getLogger().info("Using in-memory cache for votes that are not able to be delivered.");
                        break;
                    }
                    case "file": {
                        try {
                            cache = new FileVoteCache(
                                    this,
                                    new File(getDataFolder(), section.getString("file.name")),
                                    section.getInt("file.cacheTime", -1)
                            );
                        } catch (IOException e) {
                            getLogger().log(Level.SEVERE, "Unable to load file cache. Votes will be lost!", e);
                        }

                        break;
                    }
                    default: {
                        getLogger().info("No vote caching method named '" + cacheMethod + "' known." +
                                " Votes that cannot be immediately delivered will be lost.");
                    }
                }

                int dumpRate = section.getInt("dumpRate", 5);
                ServerFilter filter = new ServerFilter(
                        section.getStringList("excludedServers"),
                        section.getBoolean("whitelist", false)
                );

                if (!section.getBoolean("onlySendToJoinedServer")) {
                    this.forwardingMethod = new PluginMessagingForwardingSource(channel, filter, this, cache, dumpRate);
                    this.forwardingMethod.init();
                } else {
                    String fallback = section.getString("joinedServerFallback", null);
                    if (fallback != null && fallback.isEmpty()) {
                        fallback = null;
                    }

                    this.forwardingMethod = new OnlineForwardPluginMessagingForwardingSource(channel, this, filter, cache, fallback, dumpRate);
                    this.forwardingMethod.init();
                }

                break;
            }
            case "proxy": {
                Configuration proxySection = fwd.getSection("proxy");
                List<ProxyForwardingVoteSource.BackendServer> serverList = new ArrayList<>();
                for (String s : proxySection.getKeys()) {
                    Configuration section = proxySection.getSection(s);
                    InetAddress address;

                    try {
                        address = InetAddress.getByName(section.getString("address"));
                    } catch (UnknownHostException e) {
                        getLogger().info("Couldn't look up " + section.getString("address") + "! Ignoring!");
                        continue;
                    }

                    Key token = null;
                    try {
                        token = TokenUtil.toKey(section.getString("token", section.getString("key")));
                    } catch (IllegalArgumentException ex) {
                        getLogger().log(Level.SEVERE,
                                "An exception occurred while attempting to add proxy target '" + s
                                        + "' - maybe your token is wrong? Votes will not be forwarded"
                                        + "to this server!", ex
                        );
                    }

                    if (token != null) {
                        ProxyForwardingVoteSource.BackendServer server = new ProxyForwardingVoteSource.BackendServer(
                                s,
                                new InetSocketAddress(address, section.getInt("port")),
                                token
                        );

                        serverList.add(server);
                    }
                }

                this.forwardingMethod = bootstrap.createForwardingSource(serverList, null);
                getLogger().info("Forwarding votes from this NuVotifier instance to another NuVotifier server.");
                break;
            }
            case "redis": {
                Configuration section = fwd.getSection("redis");
                if (!fwd.contains("redis")) {
                    throw new IllegalStateException("Attempted to use Redis forwarding without a valid Redis configuration");
                }

                this.forwardingMethod = new RedisForwardingVoteSource(
                        RedisCredentials.builder()
                                .host(section.getString("address"))
                                .port(section.getInt("port"))
                                .username(section.getString("username"))
                                .password(section.getString("password"))
                                .uri(section.getString("uri"))
                                .channel(section.getString("channel"))
                                .build(),

                        getPluginLogger()
                );

                this.forwardingMethod.init();
                this.getLogger().info("Forwarding votes from this NuVotifier instance using Redis.");
                break;
            }
            default: {
                getLogger().severe("No vote forwarding method '" + method+ "' known. " +
                        "Defaulting to noop implementation.");
            }
        }

        if (configuration.getBoolean("check-for-updates", true)) {
            scheduler.runAsync(() -> {
                String current = getDescription().getVersion();
                String latest = updateChecker.fetchLatest();

                if (current.equalsIgnoreCase(latest)) {
                    return;
                }

                getLogger().info("There's a new version of azuvotifier available! (" + latest + ", you're currently on " + current + ")");
                getLogger().info("Get the update on Modrinth: " + CommonConstants.MODRINTH_URL);
            });
        }
    }

    @Override
    public void onEnable() {
        this.updateChecker = new GitHubUpdateChecker(CommonConstants.GITHUB_REPOSITORY);
        this.scheduler = new BungeeScheduler(this);
        this.pluginLogger = new JavaLoggingAdapter(getLogger());

        PluginManager pluginManager = ProxyServer.getInstance().getPluginManager();
        pluginManager.registerCommand(this, new VotifierProxyCommand(this));
        pluginManager.registerListener(this, new ProxyReloadListener(this));

        try {
            loadAndBind();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to load Votifier", ex);
        }
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
            getLogger().log(Level.SEVERE, "On halt, an exception was thrown. This may be fine!", ex);
        }

        try {
            loadAndBind();
            getLogger().info("Reload was successful.");
            return true;
        } catch (Exception ex) {
            try {
                halt();
                getLogger().log(Level.SEVERE, "On reload, there was a problem with the configuration. Votifier currently does nothing!", ex);
            } catch (Exception ex2) {
                getLogger().log(Level.SEVERE, "On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex);
                getLogger().log(Level.SEVERE, "(halt exception)", ex2);
            }
            return false;
        }
    }

    @Override
    public void onDisable() {
        halt();
        getLogger().info("Votifier disabled.");
    }

    @Override
    public void onVoteReceived(final Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) {
        if (debug) {
            getLogger().info("Got a " + protocolVersion.getHumanReadable() + " vote record from "
                    + remoteAddress + " -> " + vote);
        }

        getProxy().getScheduler().runAsync(this, () -> {
            VotifierEvent event = getProxy().getPluginManager().callEvent(new VotifierEvent(vote));
            if (!event.isCancelled() && forwardingMethod != null) {
                forwardingMethod.forward(vote);
            }
        });
    }

    @Override
    public void onError(Throwable throwable, boolean alreadyHandledVote, String remoteAddress) {
        if (debug) {
            if (alreadyHandledVote) {
                getLogger().log(Level.WARNING, "Vote processed, however an exception " +
                        "occurred with a vote from " + remoteAddress, throwable);
            } else {
                getLogger().log(Level.WARNING, "Unable to process vote from " + remoteAddress, throwable);
            }
        } else if (!alreadyHandledVote) {
            getLogger().log(Level.WARNING, "Unable to process vote from " + remoteAddress);
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
        return pluginLogger;
    }

    @Override
    public VotifierScheduler getScheduler() {
        return scheduler;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public Collection<BackendServer> getAllBackendServers() {
        return getProxy().getServers().values().stream()
                .map(BungeeBackendServer::new)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BackendServer> getServer(String name) {
        ServerInfo info = getProxy().getServerInfo(name);
        return Optional.ofNullable(info).map(BungeeBackendServer::new);
    }
}
