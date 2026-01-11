package com.vexsoftware.votifier.standalone;

import com.vexsoftware.votifier.standalone.bootstrap.VotifierBootstrap;
import org.apache.commons.cli.*;

public final class Main {

    public static void main(String[] args) {
        VotifierBootstrap bootstrap = new VotifierBootstrap(args);
        bootstrap.init();
    }
}