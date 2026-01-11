package com.vexsoftware.votifier.standalone.options;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;

public final class VotifierOptions {

    public static HelpFormatter HELP_FORMATTER = new HelpFormatter();

    public static final String DEFAULT_SYNTAX = "java -jar azuvotifier-standalone.jar [OPTIONS]";

    public static final Option HOST = Option.builder("h")
            .desc("The address azuvotifier should bind to.")
            .hasArg(true)
            .type(String.class)
            .longOpt("host")
            .required(false)
            .build();

    public static final Option PORT = Option.builder("p")
            .desc("The port azuvotifier should bind to.")
            .hasArg(true)
            .type(int.class)
            .longOpt("port")
            .required(false)
            .build();

    public static final Option CONFIG_FOLDER = Option.builder("c")
            .desc("The location where azuvotifier should store configuration files at.")
            .hasArg(true)
            .required(false)
            .type(File.class)
            .longOpt("config")
            .build();

    public static Options OPTIONS = new Options()
            .addOption(HOST)
            .addOption(PORT)
            .addOption(CONFIG_FOLDER);

    private VotifierOptions() {
        throw new UnsupportedOperationException();
    }

    public static void printHelp() {
        printHelp(DEFAULT_SYNTAX);
    }

    public static void printHelp(String syntax) {
        HELP_FORMATTER.printHelp(syntax, OPTIONS);
    }
}
