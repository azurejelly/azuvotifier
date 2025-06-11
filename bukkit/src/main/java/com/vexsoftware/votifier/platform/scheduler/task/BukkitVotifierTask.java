package com.vexsoftware.votifier.platform.scheduler.task;

import com.vexsoftware.votifier.platform.scheduler.VotifierTask;
import org.bukkit.scheduler.BukkitTask;

public final class BukkitVotifierTask implements VotifierTask {

    private final BukkitTask task;

    public BukkitVotifierTask(BukkitTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}