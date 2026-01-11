package com.vexsoftware.votifier.bungee.listeners;

import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ProxyReloadListener implements Listener {

    private final NuVotifierBungee plugin;

    public ProxyReloadListener(NuVotifierBungee plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProxyReload(ProxyReloadEvent event) {
        plugin.reload();
    }
}
