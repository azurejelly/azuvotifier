package com.vexsoftware.votifier.bukkit.standalone.platform.logger;

import com.vexsoftware.votifier.bukkit.platform.LoggingAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandaloneVotifierLoggingAdapter implements LoggingAdapter {

    private final Logger logger;

    public StandaloneVotifierLoggingAdapter(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    @Override
    public void error(String s) {
        logger.error(s);
    }

    @Override
    public void error(String s, Object... o) {
        logger.error(s, o);
    }

    @Override
    public void error(String s, Throwable e, Object... o) {
        // FIXME: this should handle the 'o' parameter as well
        logger.error(s, e);
    }

    @Override
    public void warn(String s) {
        logger.warn(s);
    }

    @Override
    public void warn(String s, Object... o) {
        logger.warn(s, o);
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void info(String s, Object... o) {
        logger.info(s, o);
    }
}
