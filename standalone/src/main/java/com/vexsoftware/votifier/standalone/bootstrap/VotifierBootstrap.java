package com.vexsoftware.votifier.standalone.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.standalone.config.VotifierConfiguration;
import com.vexsoftware.votifier.standalone.config.options.CommandArguments;
import com.vexsoftware.votifier.standalone.plugin.StandaloneVotifierPlugin;
import com.vexsoftware.votifier.standalone.plugin.builder.VotifierServerBuilder;
import com.vexsoftware.votifier.util.TokenUtil;
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

public class VotifierBootstrap {

    private final Logger logger;
    private final String[] args;
    private File directory;
    private File configFile;
    private VotifierConfiguration config;
    private CommandLine commandLine;
    private InetSocketAddress socket;
    private StandaloneVotifierPlugin plugin;

    public VotifierBootstrap(String[] args) {
        this.args = args;
        this.logger = LoggerFactory.getLogger(VotifierBootstrap.class);
    }

    public void init() {
        logger.info("Initializing Votifier...");

        Options options = new Options();
        options.addOption(CommandArguments.CONFIG_FOLDER);
        options.addOption(CommandArguments.HOST);
        options.addOption(CommandArguments.PORT);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        mapper.findAndRegisterModules();

        try {
            CommandLineParser parser = new DefaultParser();
            this.commandLine = parser.parse(options, args);
        } catch (ParseException ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar nuvotifier-standalone.jar [OPTIONS]", options);
            System.exit(1);
        }

        if (commandLine.hasOption(CommandArguments.CONFIG_FOLDER)) {
            try {
                this.directory = commandLine.getParsedOptionValue(CommandArguments.CONFIG_FOLDER);
                if (!this.directory.exists() && !this.directory.mkdirs()) {
                    logger.error("Failed to create configuration directory at '{}'", this.directory.getAbsolutePath());
                    System.exit(1);
                }
            } catch (ParseException ex) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar nuvotifier-standalone.jar [OPTIONS]", options);
                System.exit(1);
            } catch (SecurityException ex) {
                logger.error("An exception was caught while attempting to create the configuration directory", ex);
                System.exit(1);
            }
        } else {
            Path currentRelativePath = Paths.get(".");
            this.directory = new File(currentRelativePath.toFile(), "config");

            if (!directory.exists() && !directory.mkdirs()) {
                logger.error("Failed to create configuration directory at {}", directory.getAbsolutePath());
                System.exit(1);
            }
        }

        try {
            this.configFile = new File(directory, "config.yml");

            if (!configFile.exists()) {
                InputStream resource = this.getClass().getClassLoader().getResourceAsStream("standaloneConfig.yml");
                if (resource == null) {
                    logger.error("Failed to find default configuration file in JAR.");
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

        String address = commandLine.hasOption(CommandArguments.HOST)
                ? commandLine.getOptionValue(CommandArguments.HOST)
                : config.getHost();

        try {
            if (commandLine.hasOption(CommandArguments.PORT)) {
                String str = commandLine.getOptionValue(CommandArguments.PORT);
                this.socket = new InetSocketAddress(address, Integer.parseInt(str));
            } else {
                this.socket = new InetSocketAddress(address, config.getPort());
            }
        } catch (IllegalArgumentException ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar nuvotifier-standalone.jar [OPTIONS]", options);
            System.exit(1);
        }

        File rsaFolder = new File(directory, "rsa" + File.separator);
        if (!rsaFolder.exists()) {
            if (!rsaFolder.mkdirs()) {
                logger.error(
                        "Cannot make RSA folder at {}, unable to continue creating standalone Votifier server.",
                        rsaFolder.getAbsolutePath()
                );

                System.exit(1);
            }

            try {
                RSAIO.save(rsaFolder, RSAKeygen.generate(2048));
                logger.info("Generated new RSA key pair at {}", rsaFolder.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Failed to generate RSA key pair at {}", rsaFolder.getAbsolutePath(), e);
                System.exit(1);
            }
        }

        try {
            VotifierServerBuilder builder = new VotifierServerBuilder()
                    .bind(socket)
                    .v1KeyFolder(rsaFolder)
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

            this.plugin = builder.create();
        } catch (Exception ex) {
            logger.error("Failed to build the standalone Votifier server", ex);
            System.exit(1);
        }

        this.plugin.start(ex -> {
            if (ex == null) {
                return;
            }

            logger.error("Could not initialize standalone Votifier server", ex);
            System.exit(1);
        });
    }

    public void shutdown() {
        logger.info("Votifier is now shutting down...");
        plugin.halt();
    }
}
