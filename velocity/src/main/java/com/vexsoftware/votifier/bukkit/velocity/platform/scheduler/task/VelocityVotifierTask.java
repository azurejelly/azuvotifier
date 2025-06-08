package com.vexsoftware.votifier.bukkit.velocity.platform.scheduler.task;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.vexsoftware.votifier.bukkit.platform.scheduler.VotifierTask;

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
