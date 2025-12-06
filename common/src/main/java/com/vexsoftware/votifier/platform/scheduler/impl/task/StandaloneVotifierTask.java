package com.vexsoftware.votifier.platform.scheduler.impl.task;

import com.vexsoftware.votifier.platform.scheduler.VotifierTask;

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