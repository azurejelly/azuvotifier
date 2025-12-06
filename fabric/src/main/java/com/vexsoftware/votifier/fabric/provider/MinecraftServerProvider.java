package com.vexsoftware.votifier.fabric.provider;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;

public class MinecraftServerProvider {

    private static MinecraftServer server;

    public static MinecraftServer getServer() {
        return server;
    }

    @ApiStatus.Internal
    public static void setServer(MinecraftServer instance) {
        if (server != null) {
            throw new UnsupportedOperationException("server was already set!");
        }

        server = instance;
    }
}
