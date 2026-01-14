package com.vexsoftware.votifier.platform.scheduler.folia;

import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.platform.scheduler.VotifierTask;
import com.vexsoftware.votifier.platform.scheduler.folia.task.FoliaVotifierTask;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class FoliaScheduler implements VotifierScheduler {

    private final Plugin plugin;
    private final AsyncScheduler asyncScheduler;
    private final GlobalRegionScheduler globalRegionScheduler;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.asyncScheduler = plugin.getServer().getAsyncScheduler();
        this.globalRegionScheduler = plugin.getServer().getGlobalRegionScheduler();
    }

    @Override
    public VotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new FoliaVotifierTask(
                asyncScheduler.runDelayed(plugin, (t) -> runnable.run(), delay, unit)
        );
    }

    @Override
    public VotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new FoliaVotifierTask(
                asyncScheduler.runAtFixedRate(plugin, (t) -> runnable.run(), delay, repeat, unit)
        );
    }

    @Override
    public VotifierTask run(Runnable runnable) {
        return new FoliaVotifierTask(
                globalRegionScheduler.run(plugin, (t) -> runnable.run())
        );
    }

    @Override
    public VotifierTask runAsync(Runnable runnable) {
        return new FoliaVotifierTask(
                asyncScheduler.runNow(plugin, (t) -> runnable.run())
        );
    }
}
