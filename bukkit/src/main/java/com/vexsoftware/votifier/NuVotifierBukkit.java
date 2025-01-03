/*
 * Copyright (C) 2012 Vex Software LLC
 * This file is part of Votifier.
 *
 * Votifier is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Votifier is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Votifier.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.vexsoftware.votifier;

import com.vexsoftware.votifier.commands.TestVoteCommand;
import com.vexsoftware.votifier.commands.VotifierReloadCommand;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.platform.JavaUtilLogger;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.forwarding.BukkitPluginMessagingForwardingSink;
import com.vexsoftware.votifier.platform.scheduler.BukkitScheduler;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.support.forwarding.redis.RedisCredentials;
import com.vexsoftware.votifier.support.forwarding.redis.RedisForwardingSink;
import com.vexsoftware.votifier.util.IOUtil;
import com.vexsoftware.votifier.util.KeyCreator;
import com.vexsoftware.votifier.util.TokenUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * The main Votifier plugin class.
 *
 * @author Blake Beaupain
 * @author Kramer Campbell
 */
public class NuVotifierBukkit extends JavaPlugin implements VoteHandler, VotifierPlugin, ForwardedVoteListener {

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
    private VotifierScheduler scheduler;
    private LoggingAdapter pluginLogger;
    private boolean isFolia;

    private boolean loadAndBind() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            this.isFolia = true;

            getLogger().info("Using Folia; VotifierEvent will be fired asynchronously.");
        } catch (ClassNotFoundException e) {
            this.isFolia = false;
        }

        this.scheduler = new BukkitScheduler(this);
        this.pluginLogger = new JavaUtilLogger(getLogger());

        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                throw new RuntimeException("Unable to create the plugin data folder " + getDataFolder());
            }
        }

        // Handle configuration.
        File config = new File(getDataFolder(), "config.yml");

        /*
         * Use IP address from server.properties as a default for
         * configurations. Do not use InetAddress.getLocalHost() as it most
         * likely will return the main server address instead of the address
         * assigned to the server.
         */
        String hostAddr = Bukkit.getServer().getIp();
        if (hostAddr.isEmpty()) {
            hostAddr = "0.0.0.0";
        }

        /*
         * Create configuration file if it does not exist; otherwise, load it
         */
        if (!config.exists()) {
            try {
                // First time run - do some initialization.
                getLogger().info("Configuring Votifier for the first time...");

                // Initialize the configuration file.
                if (!config.createNewFile()) {
                    throw new IOException("Unable to create the config file at " + config);
                }

                // Load and manually replace variables in the configuration.
                InputStream defaults = getResource("bukkitConfig.yml");
                if (defaults == null) {
                    throw new IOException("Unable to obtain the default configuration file!");
                }

                String cfgStr = new String(IOUtil.readAllBytes(defaults), StandardCharsets.UTF_8);
                String token = TokenUtil.newToken();
                cfgStr = cfgStr.replace("%default_token%", token).replace("%ip%", hostAddr);

                Files.copy(
                        new ByteArrayInputStream(cfgStr.getBytes(StandardCharsets.UTF_8)),
                        config.toPath(), StandardCopyOption.REPLACE_EXISTING
                );

                /*
                 * Remind hosted server admins to be sure they have the right port number.
                 */
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Assigning NuVotifier to listen on port 8192. If you are running this server");
                getLogger().info("on a shared hosting, make sure to check with your hosting provider to verify");
                getLogger().info("that this port is available for your use. Chances are your hosting provider");
                getLogger().info("will assign a different port, which you'll need to change in the NuVotifier");
                getLogger().info("config.yml file.");
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Your default NuVotifier token is '" + token + "'. You'll need to provide");
                getLogger().info("this token when submitting your server to a voting list.");
                getLogger().info("------------------------------------------------------------------------------");
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, "Error creating configuration file", ex);
                return false;
            }
        }

        YamlConfiguration cfg;
        File rsaDirectory = new File(getDataFolder(), "rsa");

        // Load configuration.
        cfg = YamlConfiguration.loadConfiguration(config);

        /*
         * Create RSA directory and keys if it does not exist; otherwise, read keys.
         */
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
            getLogger().log(Level.SEVERE, "Error reading configuration file or RSA tokens", ex);
            return false;
        }

        // the quiet flag always runs priority to the debug flag
        if (cfg.isBoolean("quiet")) {
            this.debug = !cfg.getBoolean("quiet");
        } else {
            // otherwise, default to being noisy
            this.debug = cfg.getBoolean("debug", true);
        }

        // Load Votifier tokens.
        ConfigurationSection tokenSection = cfg.getConfigurationSection("tokens");

        if (tokenSection != null) {
            Map<String, Object> websites = tokenSection.getValues(false);
            for (Map.Entry<String, Object> website : websites.entrySet()) {
                tokens.put(website.getKey(), KeyCreator.createKeyFrom(website.getValue().toString()));
                getLogger().info("Loaded token for website: " + website.getKey());
            }
        } else {
            String token = TokenUtil.newToken();
            tokenSection = cfg.createSection("tokens");
            tokenSection.set("default", token);
            tokens.put("default", KeyCreator.createKeyFrom(token));

            try {
                cfg.save(config);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Error saving Votifier token", e);
                return false;
            }

            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("No tokens were found in your configuration, so we've generated one for you.");
            getLogger().info("Your default Votifier token is " + token + ".");
            getLogger().info("You will need to provide this token when you submit your server to a voting");
            getLogger().info("list.");
            getLogger().info("------------------------------------------------------------------------------");
        }

        // Initialize the receiver.
        final String host = cfg.getString("host", hostAddr);
        final int port = cfg.getInt("port", 8192);
        if (!debug) {
            getLogger().info("QUIET mode enabled!");
        }

        if (port >= 0) {
            final boolean disableV1 = cfg.getBoolean("disable-v1-protocol");
            if (disableV1) {
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
                getLogger().info("currently support the modern Votifier protocol in NuVotifier.");
                getLogger().info("------------------------------------------------------------------------------");
            }

            this.bootstrap = new VotifierServerBootstrap(host, port, this, disableV1);
            this.bootstrap.start(error -> {});
        } else {
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Your Votifier port is less than 0, so we assume you do NOT want to start the");
            getLogger().info("votifier port server! Votifier will not listen for votes over any port, and");
            getLogger().info("will only listen for pluginMessaging forwarded votes!");
            getLogger().info("------------------------------------------------------------------------------");
        }

        ConfigurationSection forwardingConfig = cfg.getConfigurationSection("forwarding");
        if (forwardingConfig == null) {
            return true;
        }

        String method = forwardingConfig.getString("method", "none");
        switch (method) {
            case "none": {
                getLogger().info("Method none selected for vote forwarding: Votes will not be received from a forwarder.");
                return true;
            }
            case "pluginmessaging": {
                String channel = forwardingConfig.getString("pluginMessaging.channel", "NuVotifier");

                try {
                    this.forwardingMethod = new BukkitPluginMessagingForwardingSink(
                            this, channel, this, getPluginLogger()
                    );

                    getLogger().info("Receiving votes over plugin messaging channel '" + channel + "'.");
                    return true;
                } catch (RuntimeException e) {
                    getLogger().log(Level.SEVERE, "NuVotifier could not set up PluginMessaging for vote forwarding!", e);
                    return false;
                }
            }
            case "redis": {
                ConfigurationSection redisSection = forwardingConfig.getConfigurationSection("redis");
                if (redisSection == null) {
                    getLogger().severe(
                            "Cannot set up Redis forwarding as the 'redis' configuration section is missing."
                                    + " Defaulting to noop implementation."
                    );

                    return false;
                }

                this.forwardingMethod = new RedisForwardingSink(
                        RedisCredentials.builder()
                                .host(redisSection.getString("address"))
                                .port(redisSection.getInt("port"))
                                .username(redisSection.getString("username"))
                                .password(redisSection.getString("password"))
                                .uri(redisSection.getString("uri"))
                                .channel(redisSection.getString("channel"))
                                .build(),

                        this,
                        getPluginLogger()
                );

                return true;
            }
            default: {
                getLogger().severe("No vote forwarding method '" + method + "' known. Defaulting to noop implementation.");
                return false;
            }
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

    @Override
    public void onEnable() {
        getCommand("nvreload").setExecutor(new VotifierReloadCommand(this));
        getCommand("testvote").setExecutor(new TestVoteCommand(this));

        if (!loadAndBind()) {
            gracefulExit();
            setEnabled(false); // safer to just bomb out
        }
    }

    @Override
    public void onDisable() {
        halt();
        getLogger().info("Votifier disabled.");
    }

    public boolean reload() {
        try {
            halt();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "On halt, an exception was thrown. This may be fine!", ex);
        }

        if (loadAndBind()) {
            getLogger().info("Reload was successful.");
            return true;
        } else {
            try {
                halt();
                getLogger().log(Level.SEVERE, "On reload, there was a problem with the configuration. Votifier currently does nothing!");
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, "On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex);
            }

            return false;
        }
    }

    private void gracefulExit() {
        getLogger().log(Level.SEVERE, "Votifier did not initialize properly!");
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
            getLogger().info("Got a " + protocolVersion.humanReadable
                    + " vote record from " + remoteAddress + " -> " + vote);
        }

        fireVotifierEvent(vote);
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
    public void onForward(final Vote v) {
        if (debug) {
            getLogger().info("Got a forwarded vote -> " + v);
        }

        fireVotifierEvent(v);
    }

    private void fireVotifierEvent(Vote vote) {
        if (VotifierEvent.getHandlerList().getRegisteredListeners().length == 0) {
            getLogger().log(Level.SEVERE, "A vote was received, but you don't have any listeners available to listen for it.");
            getLogger().log(Level.SEVERE, "See https://github.com/NuVotifier/NuVotifier/wiki/Setup-Guide#vote-listeners for");
            getLogger().log(Level.SEVERE, "a list of listeners you can configure.");
        }

        if (!isFolia) {
            getServer().getScheduler().runTask(
                    this, () -> getServer().getPluginManager().callEvent(new VotifierEvent(vote))
            );
        } else {
            getServer().getScheduler().runTaskAsynchronously(
                    this, () -> getServer().getPluginManager().callEvent(new VotifierEvent(vote, true))
            );
        }
    }
}
