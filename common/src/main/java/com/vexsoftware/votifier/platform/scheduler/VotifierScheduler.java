package com.vexsoftware.votifier.platform.scheduler;

import java.util.concurrent.TimeUnit;

public interface VotifierScheduler {

    VotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit);

    VotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit);

    default VotifierTask run(Runnable runnable) {
        throw new UnsupportedOperationException();
    }

    default VotifierTask runAsync(Runnable runnable) {
        throw new UnsupportedOperationException();
    }
}
