package com.vexsoftware.votifier.bukkit.sponge.platform.scheduler;

import com.vexsoftware.votifier.bukkit.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.bukkit.platform.scheduler.VotifierTask;
import com.vexsoftware.votifier.bukkit.sponge.platform.scheduler.task.SpongeVotifierTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;

import java.util.concurrent.TimeUnit;

public final class SpongeScheduler implements VotifierScheduler {

    private final PluginContainer plugin;

    public SpongeScheduler(PluginContainer plugin) {
        this.plugin = plugin;
    }

    private Task.Builder taskBuilder(Runnable runnable) {
        return Task.builder().execute(runnable);
    }

    @Override
    public VotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        ScheduledTask task = Sponge.asyncScheduler().submit(
                taskBuilder(runnable)
                        .delay(delay, unit)
                        .plugin(plugin)
                        .build()
        );

        return new SpongeVotifierTask(task);
    }

    @Override
    public VotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        ScheduledTask task = Sponge.server().scheduler().submit(
                taskBuilder(runnable)
                        .delay(delay, unit)
                        .interval(repeat, unit)
                        .plugin(plugin)
                        .build()
        );

        return new SpongeVotifierTask(task);
    }
}
