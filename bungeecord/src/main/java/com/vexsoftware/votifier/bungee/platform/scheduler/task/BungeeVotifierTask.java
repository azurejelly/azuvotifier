package com.vexsoftware.votifier.bungee.platform.scheduler.task;

import com.vexsoftware.votifier.platform.scheduler.VotifierTask;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public final class BungeeVotifierTask implements VotifierTask {

    private final ScheduledTask task;

    public BungeeVotifierTask(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}