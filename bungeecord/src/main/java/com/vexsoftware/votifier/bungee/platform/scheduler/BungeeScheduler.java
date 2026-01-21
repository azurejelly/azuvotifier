package com.vexsoftware.votifier.bungee.platform.scheduler;

import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import com.vexsoftware.votifier.bungee.platform.scheduler.task.BungeeVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.platform.scheduler.VotifierTask;

import java.util.concurrent.TimeUnit;

public final class BungeeScheduler implements VotifierScheduler {

    private final NuVotifierBungee plugin;

    public BungeeScheduler(NuVotifierBungee plugin) {
        this.plugin = plugin;
    }

    @Override
    public VotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new BungeeVotifierTask(plugin.getProxy().getScheduler().schedule(plugin, runnable, delay, unit));
    }

    @Override
    public VotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new BungeeVotifierTask(plugin.getProxy().getScheduler().schedule(plugin, runnable, delay, repeat, unit));
    }

    @Override
    public VotifierTask runAsync(Runnable runnable) {
        return new BungeeVotifierTask(plugin.getProxy().getScheduler().runAsync(plugin, runnable));
    }
}
