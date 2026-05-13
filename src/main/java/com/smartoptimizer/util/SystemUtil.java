package com.smartoptimizer.util;

import java.lang.management.ManagementFactory;

public class SystemUtil {

    public static long getTotalRamMB() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return os.getTotalPhysicalMemorySize() / (1024L * 1024L);
        } catch (Exception e) {
            return Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        }
    }

    public static int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }
}
