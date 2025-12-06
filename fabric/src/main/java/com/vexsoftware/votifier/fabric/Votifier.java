package com.vexsoftware.votifier.fabric;

import com.vexsoftware.votifier.fabric.event.VoteListener;
import com.vexsoftware.votifier.fabric.event.listener.DefaultVoteListener;
import com.vexsoftware.votifier.fabric.platform.FabricVotifierPlugin;
import com.vexsoftware.votifier.fabric.provider.MinecraftServerProvider;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Votifier implements DedicatedServerModInitializer {

    private static Votifier instance;
    private FabricVotifierPlugin plugin;
    private Logger logger;

    @Override
    public void onInitializeServer() {
        instance = this;

        ServerLifecycleEvents.SERVER_STARTING.register(this::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::stop);
        VoteListener.EVENT.register(new DefaultVoteListener());
    }

    public void start(MinecraftServer server) {
        MinecraftServerProvider.setServer(server);

        this.logger = LoggerFactory.getLogger("azuvotifier");
        this.plugin = new FabricVotifierPlugin();

        try {
            plugin.init();
        } catch (Exception ex) {
            logger.error("Votifier did not initialize properly!", ex);
        }
    }

    public void stop(MinecraftServer server) {
        if (plugin != null) {
            plugin.halt();
        }

        logger.info("Votifier disabled.");
    }

    public static Votifier getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }
}
