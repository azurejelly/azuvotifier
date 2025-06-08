package com.vexsoftware.votifier.bukkit.util;

public class QuietException extends Exception {

    public QuietException() {}

    public QuietException(String s) {
        super(s);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
