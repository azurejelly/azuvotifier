package com.vexsoftware.votifier.velocity.platform.scheduler;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.velocity.NuVotifierVelocity;

import java.util.concurrent.TimeUnit;

public class VelocityScheduler implements VotifierScheduler {

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
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new TaskWrapper(builder(runnable).delay(delay, unit).schedule());
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new TaskWrapper(builder(runnable).delay(delay, unit).repeat(repeat, unit).schedule());
    }

    private static class TaskWrapper implements ScheduledVotifierTask {

        private final ScheduledTask task;

        private TaskWrapper(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
