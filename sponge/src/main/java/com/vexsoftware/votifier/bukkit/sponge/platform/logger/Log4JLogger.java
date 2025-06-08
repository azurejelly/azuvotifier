package com.vexsoftware.votifier.bukkit.sponge.platform.logger;

import com.vexsoftware.votifier.bukkit.platform.LoggingAdapter;
import org.apache.logging.log4j.Logger;

public class Log4JLogger implements LoggingAdapter {

    private final Logger l;

    public Log4JLogger(Logger l) {
        this.l = l;
    }

    @Override
    public void error(String s) {
        l.error(s);
    }

    @Override
    public void error(String s, Object... o) {
        l.error(s, o);
    }

    @Override
    public void error(String s, Throwable e, Object... o) {
        // FIXME: this should handle the 'o' parameter as well
        l.error(s, e/*, o*/);
    }

    @Override
    public void warn(String s) {
        l.warn(s);
    }

    @Override
    public void warn(String s, Object... o) {
        l.warn(s, o);
    }

    @Override
    public void info(String s) {
        l.info(s);
    }

    @Override
    public void info(String s, Object... o) {
        l.info(s, o);
    }
}
