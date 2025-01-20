package com.vexsoftware.votifier.sponge.platform.scheduler;

import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
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
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        ScheduledTask task = Sponge.asyncScheduler().submit(
                taskBuilder(runnable)
                        .delay(delay, unit)
                        .plugin(plugin)
                        .build()
        );

        return new SpongeTaskWrapper(task);
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        ScheduledTask task = Sponge.server().scheduler().submit(
                taskBuilder(runnable)
                        .delay(delay, unit)
                        .interval(repeat, unit)
                        .plugin(plugin)
                        .build()
        );

        return new SpongeTaskWrapper(task);
    }

    private static class SpongeTaskWrapper implements ScheduledVotifierTask {

        private final ScheduledTask task;

        private SpongeTaskWrapper(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
