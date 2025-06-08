package com.vexsoftware.votifier.bukkit.velocity.platform.scheduler;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import com.vexsoftware.votifier.bukkit.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.bukkit.platform.scheduler.VotifierTask;
import com.vexsoftware.votifier.bukkit.velocity.NuVotifierVelocity;
import com.vexsoftware.votifier.bukkit.velocity.platform.scheduler.task.VelocityVotifierTask;

import java.util.concurrent.TimeUnit;

public final class VelocityScheduler implements VotifierScheduler {

    private final ProxyServer server;
    private final NuVotifierVelocity plugin;

    public VelocityScheduler(ProxyServer server, NuVotifierVelocity plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    private Scheduler.TaskBuilder builder(Runnable runnable) {
        return server.getScheduler().buildTask(plugin, runnable);
    }

    @Override
    public VotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new VelocityVotifierTask(builder(runnable).delay(delay, unit).schedule());
    }

    @Override
    public VotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new VelocityVotifierTask(builder(runnable).delay(delay, unit).repeat(repeat, unit).schedule());
    }
}
