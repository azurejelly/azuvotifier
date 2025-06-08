package com.vexsoftware.votifier.bukkit.standalone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.vexsoftware.votifier.bukkit.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.bukkit.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.bukkit.standalone.config.VotifierConfiguration;
import com.vexsoftware.votifier.bukkit.standalone.options.VotifierOptions;
import com.vexsoftware.votifier.bukkit.standalone.platform.server.StandaloneVotifierServer;
import com.vexsoftware.votifier.bukkit.standalone.platform.server.builder.VotifierServerBuilder;
import com.vexsoftware.votifier.bukkit.util.TokenUtil;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final String[] args;
    private File directory;
    private File rsaDirectory;
    private File configFile;
    private VotifierConfiguration config;
    private CommandLine commandLine;
    private InetSocketAddress socket;
    private StandaloneVotifierServer server;
    private ObjectMapper mapper;
    private HelpFormatter helpFormatter;
    private Options options;

    public Main(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        Main main = new Main(args);
        main.init();
    }

    public void init() {
        LOGGER.info("Initializing Votifier...");

        this.setupCommandLine();
        this.setupConfiguration();
        this.setupRSA();
        this.setupServer();
        this.setupShutdownHook();
    }

    public void shutdown() {
        LOGGER.info("Votifier is now shutting down...");

        this.server.halt();
    }

    private void setupShutdownHook() {
        // Make sure we shut down safely as long as we don't get SIGKILL'd or something
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "Shutdown Thread"));

        try {
            // Keep the program running
            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void setupServer() {
        String address = commandLine.hasOption(VotifierOptions.HOST)
                ? commandLine.getOptionValue(VotifierOptions.HOST)
                : config.getHost();

        try {
            if (commandLine.hasOption(VotifierOptions.PORT)) {
                String str = commandLine.getOptionValue(VotifierOptions.PORT);
                this.socket = new InetSocketAddress(address, Integer.parseInt(str));
            } else {
                this.socket = new InetSocketAddress(address, config.getPort());
            }
        } catch (IllegalArgumentException ex) {
            printHelp();
            System.exit(1);
        }

        try {
            VotifierServerBuilder builder = new VotifierServerBuilder()
                    .bind(socket)
                    .v1KeyFolder(rsaDirectory)
                    .disableV1Protocol(config.isDisableV1Protocol())
                    .debug(config.isDebug())
                    .redis(config.getRedis())
                    .backendServers(config.getBackendServers());

            this.config.getTokens().forEach((service, token) -> {
                if ("default".equals(service) && "%default_token%".equals(token)) {
                    token = TokenUtil.newToken();
                    config.getTokens().put(service, token);

                    try {
                        mapper.writeValue(configFile, config);
                        LOGGER.info("------------------------------------------------------------------------------");
                        LOGGER.info("No tokens were found in your configuration, so we've generated one for you.");
                        LOGGER.info("Your default Votifier token is '{}'.", token);
                        LOGGER.info("You will need to provide this token when you submit your server to a voting");
                        LOGGER.info("list.");
                        LOGGER.info("------------------------------------------------------------------------------");
                    } catch (IOException e) {
                        LOGGER.error("Failed to write a random default token", e);
                        System.exit(1);
                    }
                }

                builder.addToken(service, token);
            });

            this.server = builder.create();
        } catch (Exception ex) {
            LOGGER.error("Failed to build the standalone Votifier server", ex);
            System.exit(1);
        }

        this.server.start(ex -> {
            if (ex == null) {
                return;
            }

            LOGGER.error("An exception occurred while initializing the Votifier server", ex);
            System.exit(1);
        });
    }

    private void setupCommandLine() {
        this.options = new Options();
        this.options.addOption(VotifierOptions.CONFIG_FOLDER);
        this.options.addOption(VotifierOptions.HOST);
        this.options.addOption(VotifierOptions.PORT);

        try {
            CommandLineParser parser = new DefaultParser();
            this.commandLine = parser.parse(options, args);
        } catch (ParseException ex) {
            this.printHelp();
            System.exit(1);
        }
    }

    private void setupRSA() {
        this.rsaDirectory = new File(directory, "rsa" + File.separator);

        if (!rsaDirectory.exists()) {
            if (!rsaDirectory.mkdirs()) {
                LOGGER.error(
                        "Could not make RSA folder at {}, unable to continue creating standalone Votifier server.",
                        rsaDirectory.getAbsolutePath()
                );

                System.exit(1);
            }

            try {
                RSAIO.save(rsaDirectory, RSAKeygen.generate(2048));
                LOGGER.info("Generated new RSA key pair at {}", rsaDirectory.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("Failed to generate RSA key pair at {}", rsaDirectory.getAbsolutePath(), e);
                System.exit(1);
            }
        }
    }

    private void setupConfiguration() {
        setupMapper();
        setupConfigurationDirectory();
        setupConfigurationFile();
    }

    private void setupMapper() {
        this.mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        this.mapper.findAndRegisterModules();
    }

    private void setupConfigurationFile() {
        try {
            this.configFile = new File(directory, "config.yml");

            if (!configFile.exists()) {
                InputStream resource = this.getClass().getClassLoader().getResourceAsStream("standaloneConfig.yml");
                if (resource == null) {
                    LOGGER.error("Failed to find default configuration file in JAR.");
                    System.exit(1);
                }

                Files.copy(resource, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.debug("Copied default configuration file from JAR.");
            }

            this.config = mapper.readValue(configFile, VotifierConfiguration.class);
        } catch (IOException e) {
            LOGGER.error("Failed to read or copy defaults to configuration file:", e);
            System.exit(1);
        }
    }

    private void setupConfigurationDirectory() {
        if (commandLine.hasOption(VotifierOptions.CONFIG_FOLDER)) {
            try {
                this.directory = commandLine.getParsedOptionValue(VotifierOptions.CONFIG_FOLDER);
                if (!this.directory.exists() && !this.directory.mkdirs()) {
                    LOGGER.error("Failed to create configuration directory at '{}'", this.directory.getAbsolutePath());
                    System.exit(1);
                }
            } catch (ParseException ex) {
                printHelp();
                System.exit(1);
            } catch (SecurityException ex) {
                LOGGER.error("An exception was caught while attempting to create the configuration directory", ex);
                System.exit(1);
            }
        } else {
            Path currentRelativePath = Paths.get(".");
            this.directory = new File(currentRelativePath.toFile(), "config");

            if (!directory.exists() && !directory.mkdirs()) {
                LOGGER.error("Failed to create configuration directory at {}", directory.getAbsolutePath());
                System.exit(1);
            }
        }
    }

    private void printHelp() {
        if (helpFormatter == null) {
            this.helpFormatter = new HelpFormatter();
        }

        helpFormatter.printHelp("java -jar nuvotifier-standalone.jar [OPTIONS]", options);
    }
}