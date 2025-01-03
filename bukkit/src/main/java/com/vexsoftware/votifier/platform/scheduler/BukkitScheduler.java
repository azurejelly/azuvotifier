package com.vexsoftware.votifier.platform.scheduler;

import com.vexsoftware.votifier.NuVotifierBukkit;
import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public class BukkitScheduler implements VotifierScheduler {

    private final NuVotifierBukkit plugin;

    public BukkitScheduler(NuVotifierBukkit plugin) {
        this.plugin = plugin;
    }

    private int toTicks(int time, TimeUnit unit) {
        return (int) (unit.toMillis(time) / 50);
    }

    @Override
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new BukkitTaskWrapper(
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, toTicks(delay, unit))
        );
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new BukkitTaskWrapper(
                plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                        plugin, runnable, toTicks(delay, unit), toTicks(repeat, unit)
                )
        );
    }

    private static class BukkitTaskWrapper implements ScheduledVotifierTask {

        private final BukkitTask task;

        private BukkitTaskWrapper(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
