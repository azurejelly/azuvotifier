package com.vexsoftware.votifier.velocity.platform.scheduler.task;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierTask;

public final class VelocityVotifierTask implements VotifierTask {

    private final ScheduledTask task;

    public VelocityVotifierTask(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
