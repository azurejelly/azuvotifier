package com.vexsoftware.votifier.bukkit.standalone.platform.scheduler.task;

import com.vexsoftware.votifier.bukkit.platform.scheduler.VotifierTask;

import java.util.concurrent.Future;

public final class StandaloneVotifierTask implements VotifierTask {

    private final Future<?> future;

    public StandaloneVotifierTask(Future<?> future) {
        this.future = future;
    }

    @Override
    public void cancel() {
        future.cancel(false);
    }
}