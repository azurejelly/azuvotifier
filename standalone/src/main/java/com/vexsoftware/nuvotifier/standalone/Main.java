package com.vexsoftware.nuvotifier.standalone;

import com.vexsoftware.nuvotifier.standalone.bootstrap.VotifierBootstrap;

public class Main {

    public static void main(String[] args) {
        VotifierBootstrap bootstrap = new VotifierBootstrap(args);
        bootstrap.init();

        Runtime.getRuntime().addShutdownHook(new Thread(bootstrap::shutdown, "shutdown"));

        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}