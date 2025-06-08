package com.vexsoftware.votifier.bukkit.bungee.platform.scheduler.task;

import com.vexsoftware.votifier.bukkit.platform.scheduler.VotifierTask;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public final class BungeeVotifierTask implements VotifierTask {

    private final ScheduledTask task;

    public BungeeVotifierTask(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        this.task.cancel();
    }
}