package com.smartoptimizer.detection;

public class HardwareInfo {

    private final long totalRamMB;
    private final int cpuCores;

    public HardwareInfo(long totalRamMB, int cpuCores) {
        this.totalRamMB = totalRamMB;
        this.cpuCores = cpuCores;
    }

    public long getTotalRamMB() { return totalRamMB; }
    public int getCpuCores()    { return cpuCores; }
    public long getTotalRamGB() { return totalRamMB / 1024; }
}
