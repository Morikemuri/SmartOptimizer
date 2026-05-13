package com.smartoptimizer.detection;

import com.smartoptimizer.SmartOptimizerMod;

import java.lang.management.ManagementFactory;

public class HardwareDetector {

    public static HardwareInfo detect() {
        long totalRamMB = getTotalRAMMB();
        int cpuCores = Runtime.getRuntime().availableProcessors();

        SmartOptimizerMod.LOGGER.info("RAM: {}GB", totalRamMB / 1024);
        SmartOptimizerMod.LOGGER.info("CPU cores: {}", cpuCores);

        return new HardwareInfo(totalRamMB, cpuCores);
    }

    private static long getTotalRAMMB() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return os.getTotalPhysicalMemorySize() / (1024L * 1024L);
        } catch (Exception e) {
            return Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        }
    }
}
