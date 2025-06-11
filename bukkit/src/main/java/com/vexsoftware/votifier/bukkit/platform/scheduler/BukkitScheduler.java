package com.vexsoftware.votifier.bukkit.platform.scheduler;

import com.vexsoftware.votifier.bukkit.NuVotifierBukkit;
import com.vexsoftware.votifier.bukkit.platform.scheduler.task.BukkitVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.platform.scheduler.VotifierTask;

import java.util.concurrent.TimeUnit;

public final class BukkitScheduler implements VotifierScheduler {

    private final NuVotifierBukkit plugin;

    public BukkitScheduler(NuVotifierBukkit plugin) {
        this.plugin = plugin;
    }

    private int toTicks(int time, TimeUnit unit) {
        return (int) (unit.toMillis(time) / 50);
    }

    @Override
    public VotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new BukkitVotifierTask(
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, toTicks(delay, unit))
        );
    }

    @Override
    public VotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new BukkitVotifierTask(
                plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                        plugin, runnable, toTicks(delay, unit), toTicks(repeat, unit)
                )
        );
    }

    @Override
    public VotifierTask run(Runnable runnable) {
        return new BukkitVotifierTask(
                plugin.getServer().getScheduler().runTask(plugin, runnable)
        );
    }

    @Override
    public VotifierTask runAsync(Runnable runnable) {
        return new BukkitVotifierTask(
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable)
        );
    }
}
