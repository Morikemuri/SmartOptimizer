package com.smartoptimizer.util;

import com.smartoptimizer.SmartOptimizerMod;

public class LogUtil {

    public static void info(String message, Object... args) {
        SmartOptimizerMod.LOGGER.info(message, args);
    }

    public static void warn(String message, Object... args) {
        SmartOptimizerMod.LOGGER.warn(message, args);
    }

    public static void debug(String message, Object... args) {
        SmartOptimizerMod.LOGGER.debug(message, args);
    }
}
