package com.vexsoftware.votifier.sponge.platform.logger;

import com.vexsoftware.votifier.platform.logger.LoggingAdapter;
import org.apache.logging.log4j.Logger;

public class Log4JLoggingAdapter implements LoggingAdapter {

    private final Logger logger;

    public Log4JLoggingAdapter(Logger logger) {
        this.logger = logger;
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
        logger.error(s, e, o);
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
