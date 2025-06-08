package com.vexsoftware.votifier.bukkit.velocity.platform.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.vexsoftware.votifier.bukkit.platform.BackendServer;
import com.vexsoftware.votifier.bukkit.velocity.utils.VelocityUtil;

public class VelocityBackendServer implements BackendServer {

    private final RegisteredServer server;

    public VelocityBackendServer(RegisteredServer server) {
        this.server = server;
    }

    @Override
    public String getName() {
        return server.getServerInfo().getName();
    }

    @Override
    public boolean sendPluginMessage(String channel, byte[] data) {
        return server.sendPluginMessage(VelocityUtil.getId(channel), data);
    }

    @Override
    public String toString() {
        return server.getServerInfo().getName();
    }
}
