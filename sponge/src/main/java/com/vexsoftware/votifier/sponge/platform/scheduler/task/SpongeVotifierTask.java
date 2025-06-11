package com.vexsoftware.votifier.sponge.platform.scheduler.task;

import com.vexsoftware.votifier.platform.scheduler.VotifierTask;
import org.spongepowered.api.scheduler.ScheduledTask;

public final class SpongeVotifierTask implements VotifierTask {

    private final ScheduledTask task;

    public SpongeVotifierTask(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}