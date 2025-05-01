package com.tim.qmorph.viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Msg {
    private static final Logger logger = LoggerFactory.getLogger(Msg.class);
    public static boolean debugMode = false;

    public static void error(String msg) {
        logger.error(msg);
        System.exit(1);
    }

    public static void debug(String msg) {
        if (debugMode) {
            logger.debug(msg);
        }
    }

    public static void info(String msg) {
        logger.info(msg);
    }

    public static void warn(String msg) {
        logger.warn(msg);
    }

    // For backward compatibility
    public static void warning(String msg) {
        if (debugMode) {
            warn(msg);
        }
    }
}
