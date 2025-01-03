package com.vexsoftware.votifier.bungee.listeners;

import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ReloadListener implements Listener {

    private final NuVotifierBungee nuVotifier;

    public ReloadListener(NuVotifierBungee nuVotifier) {
        this.nuVotifier = nuVotifier;
    }

    @EventHandler
    public void onProxyReload(ProxyReloadEvent event) {
        this.nuVotifier.reload();
    }
}
