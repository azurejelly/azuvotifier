package com.vexsoftware.votifier.bukkit.standalone.platform.scheduler;

import com.vexsoftware.votifier.bukkit.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.bukkit.platform.scheduler.VotifierTask;
import com.vexsoftware.votifier.bukkit.standalone.platform.scheduler.task.StandaloneVotifierTask;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class StandaloneVotifierScheduler implements VotifierScheduler {

    private final ScheduledExecutorService service;

    public StandaloneVotifierScheduler(ScheduledExecutorService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    public VotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new StandaloneVotifierTask(service.schedule(runnable, delay, unit));
    }

    @Override
    public VotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new StandaloneVotifierTask(service.scheduleAtFixedRate(runnable, delay, repeat, unit));
    }
}
