package com.vexsoftware.votifier.standalone.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.vexsoftware.votifier.standalone.config.VotifierConfiguration;
import com.vexsoftware.votifier.standalone.options.VotifierOptions;
import com.vexsoftware.votifier.standalone.server.StandaloneVotifierServer;
import com.vexsoftware.votifier.standalone.server.builder.VotifierServerBuilder;
import com.vexsoftware.votifier.standalone.utils.NumberUtil;
import com.vexsoftware.votifier.util.CryptoUtil;
import com.vexsoftware.votifier.util.TokenUtil;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

public final class VotifierBootstrap {

    private final Logger logger;
    private final String[] args;

    private File directory;
    private File configFile;
    private KeyPair rsaKeyPair;
    private VotifierConfiguration config;
    private CommandLine commandLine;
    private InetSocketAddress socket;
    private StandaloneVotifierServer server;
    private ObjectMapper mapper;

    public VotifierBootstrap(String[] args) {
        this.logger = LoggerFactory.getLogger(VotifierBootstrap.class);
        this.args = args;
    }

    public void init() {
        logger.debug("Executing azuvotifier with command line arguments {}", String.join(" ", args));

        parseCommandLine();
        loadConfiguration();
        setupRSA();
        createServer();

        logger.info("Starting the Votifier server...");
        server.start(ex -> {
            if (ex == null) {
                return;
            }

            logger.error("An exception occurred while starting the Votifier server", ex);
            System.exit(1);
        });

        awaitShutdown();
    }

    public void shutdown() {
        logger.info("Votifier is now shutting down...");
        server.halt();
    }

    private void awaitShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "Shutdown Thread"));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void createServer() {
        String address = commandLine.hasOption(VotifierOptions.HOST)
                ? commandLine.getOptionValue(VotifierOptions.HOST)
                : config.getHost();

        try {
            if (commandLine.hasOption(VotifierOptions.PORT)) {
                int port = NumberUtil.toInt(commandLine.getOptionValue(VotifierOptions.PORT), 8192);
                this.socket = new InetSocketAddress(address, port);
            } else {
                this.socket = new InetSocketAddress(address, config.getPort());
            }
        } catch (IllegalArgumentException ex) {
            VotifierOptions.printHelp();
            System.exit(1);
        }

        var builder = new VotifierServerBuilder()
                .bind(socket)
                .v1Key(rsaKeyPair)
                .config(config)
                .backendServers(config.getForwardableServers());

        config.getTokens().forEach((service, token) -> {
            if ("default".equals(service) && "%default_token%".equals(token)) {
                token = TokenUtil.newToken();
                config.getTokens().put(service, token);

                try {
                    mapper.writeValue(configFile, config);
                    logger.info("------------------------------------------------------------------------------");
                    logger.info("No tokens were found in your configuration, so we've generated one for you.");
                    logger.info("Your default Votifier token is '{}'.", token);
                    logger.info("You will need to provide this token when you submit your server to a voting");
                    logger.info("list.");
                    logger.info("------------------------------------------------------------------------------");
                } catch (IOException e) {
                    logger.error("Failed to write a random default token", e);
                    System.exit(1);
                }
            }

            builder.addToken(service, token);
        });

        this.server = builder.create();
    }

    private void parseCommandLine() {
        try {
            this.commandLine = new DefaultParser().parse(VotifierOptions.OPTIONS, args);
        } catch (ParseException ex) {
            VotifierOptions.printHelp();
            System.exit(1);
        }
    }

    private void setupRSA() {
        File dir = new File(directory, "rsa" + File.separator);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                logger.error("Could not make RSA folder at {}. Please check your file permissions!", dir.getAbsolutePath());
                System.exit(1);
                return;
            }

            try {
                rsaKeyPair = CryptoUtil.generateKeyPair(2048);
                CryptoUtil.save(dir, rsaKeyPair);

                logger.info("Generated and saved a new RSA key pair at {}", dir.getAbsolutePath());
            } catch (IOException | GeneralSecurityException e) {
                logger.error("Failed to generate a new RSA key pair at {}", dir.getAbsolutePath(), e);
                System.exit(1);
            }
        } else {
            try {
                rsaKeyPair = CryptoUtil.load(dir);
            } catch (GeneralSecurityException | IOException e) {
                logger.error("Failed to read existing RSA key pair at {}", dir.getAbsolutePath(), e);
                System.exit(1);
            }
        }
    }

    private void loadConfiguration() {
        this.mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        this.mapper.findAndRegisterModules();

        if (commandLine.hasOption(VotifierOptions.CONFIG_FOLDER)) {
            try {
                this.directory = commandLine.getParsedOptionValue(VotifierOptions.CONFIG_FOLDER);
            } catch (ParseException ex) {
                VotifierOptions.printHelp();
                System.exit(1);
            }
        } else {
            this.directory = new File(Paths.get(".").toFile(), "config");
        }

        if (!directory.isDirectory()) {
            logger.error("{} must be a directory, not a file!", directory.getAbsolutePath());
            System.exit(1);
        }

        if (!directory.exists() && !directory.mkdirs()) {
            logger.error("Failed to create configuration directory at {}. Please check your file permissions!", directory.getAbsolutePath());
            System.exit(1);
        }

        if (!directory.canRead()) {
            logger.error("Votifier cannot read the configuration directory at {}", directory.getAbsolutePath());
            System.exit(1);
        }

        try {
            this.configFile = new File(directory, "config.yml");
            if (!configFile.exists()) {
                InputStream resource = this.getClass().getClassLoader().getResourceAsStream("standaloneConfig.yml");
                if (resource == null) {
                    logger.error("Failed to find default configuration file in JAR. Please re-download azuvotifier.");
                    System.exit(1);
                }

                Files.copy(resource, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Copied default configuration file from JAR.");
            }

            this.config = mapper.readValue(configFile, VotifierConfiguration.class);
        } catch (IOException e) {
            logger.error("Failed to read or copy defaults to configuration file:", e);
            System.exit(1);
        }
    }
}
