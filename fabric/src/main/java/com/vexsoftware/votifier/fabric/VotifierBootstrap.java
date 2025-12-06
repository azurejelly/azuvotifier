package com.vexsoftware.votifier.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VotifierBootstrap implements DedicatedServerModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("azuvotifier");

    @Override
    public void onInitializeServer() {
        LOGGER.info("Hello from azuvotifier!");
    }
}
