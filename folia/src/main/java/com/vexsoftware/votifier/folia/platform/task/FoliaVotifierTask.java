package com.vexsoftware.votifier.folia.platform.task;

import com.vexsoftware.votifier.platform.scheduler.VotifierTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public final class FoliaVotifierTask implements VotifierTask {

    private final ScheduledTask task;

    public FoliaVotifierTask(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        this.task.cancel();
    }
}
