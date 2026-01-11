package com.vexsoftware.votifier.fabric.platform.provider;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;

/**
 * Provides access to an instance of {@link MinecraftServer}.
 *
 * @author azurejelly
 * @since 3.4.0
 */
public class MinecraftServerProvider {

    /**
     * The {@link MinecraftServer} instance currently in use.
     */
    private static MinecraftServer server;

    /**
     * Provides a {@link MinecraftServer} instance.
     *
     * <p>This method should only be called after the server has
     * already started, and may not be available during or before
     * the {@link ServerLifecycleEvents#SERVER_STARTING} phase.
     *
     * @return a {@link MinecraftServer} instance.
     */
    public static MinecraftServer getServer() {
        return server;
    }

    /**
     * Sets the {@link MinecraftServer} instance for this provider.
     *
     * @param instance a {@link MinecraftServer} instance.
     * @throws IllegalStateException if the server is already set
     */
    @ApiStatus.Internal
    public static void setServer(MinecraftServer instance) {
        if (server != null) {
            throw new IllegalStateException("server was already set!");
        }

        server = instance;
    }
}
