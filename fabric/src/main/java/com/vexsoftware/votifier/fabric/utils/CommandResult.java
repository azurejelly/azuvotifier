package com.vexsoftware.votifier.fabric.utils;

public final class CommandResult {

    // i truly do not know who was the genius at mojang that thought making '1' success was a great idea
    public static final int FAIL = 0;
    public static final int SUCCESS = 1;

    private CommandResult() {
        throw new UnsupportedOperationException();
    }
}
