package com.vexsoftware.votifier.standalone.options;

import org.apache.commons.cli.Option;

import java.io.File;

public final class VotifierOptions {

    public static final Option HOST = Option.builder("h")
            .desc("The address NuVotifier should bind to.")
            .hasArg(true)
            .type(String.class)
            .longOpt("host")
            .required(false)
            .build();

    public static final Option PORT = Option.builder("p")
            .desc("The port NuVotifier should bind to.")
            .hasArg(true)
            .type(int.class)
            .longOpt("port")
            .required(false)
            .build();

    public static final Option CONFIG_FOLDER = Option.builder("c")
            .desc("The location where NuVotifier should store configuration files at.")
            .hasArg(true)
            .required(false)
            .type(File.class)
            .longOpt("config")
            .build();

    private VotifierOptions() {
        throw new UnsupportedOperationException();
    }
}
