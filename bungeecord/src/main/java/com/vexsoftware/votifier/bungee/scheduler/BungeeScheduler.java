package com.vexsoftware.votifier.bungee.scheduler;

import com.vexsoftware.votifier.bungee.NuVotifierBungee;
import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;

public class BungeeScheduler implements VotifierScheduler {

    private final NuVotifierBungee plugin;

    public BungeeScheduler(NuVotifierBungee plugin) {
        this.plugin = plugin;
    }

    @Override
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new BungeeTaskWrapper(plugin.getProxy().getScheduler().schedule(plugin, runnable, delay, unit));
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new BungeeTaskWrapper(plugin.getProxy().getScheduler().schedule(plugin, runnable, delay, repeat, unit));
    }

    private static class BungeeTaskWrapper implements ScheduledVotifierTask {

        private final ScheduledTask task;

        private BungeeTaskWrapper(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            this.task.cancel();
        }
    }
}
