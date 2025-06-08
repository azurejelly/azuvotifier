package com.vexsoftware.votifier.bukkit.platform;

public interface BackendServer {

    String getName();

    boolean sendPluginMessage(String channel, byte[] data);
}
