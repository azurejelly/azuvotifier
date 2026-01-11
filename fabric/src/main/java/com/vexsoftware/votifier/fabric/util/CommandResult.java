package com.vexsoftware.votifier.fabric.util;

// i truly do not know who was the genius at mojang that thought making '1' success was a great idea
public final class CommandResult {

    /**
     * Indicates that the command execution has failed.
     */
    public static final int FAIL = 0;

    /**
     * Indicates that the command execution succeeded.
     */
    public static final int SUCCESS = 1;

    private CommandResult() {
        throw new UnsupportedOperationException();
    }
}
