package com.smartoptimizer.jvm;

public enum JvmProfile {

    BALANCED("Balanced",
            "-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16M -XX:+ParallelRefProcEnabled"),

    PERFORMANCE("Performance",
            "-XX:+UseZGC -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch -Xms8G -Xmx8G"),

    SAFE("Safe",
            "-XX:TieredStopAtLevel=3 -XX:+UseSerialGC -Xms4G -Xmx4G"),

    COMPATIBILITY("Compatibility",
            "-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xms2G -Xmx4G");

    public final String displayName;
    public final String args;

    JvmProfile(String displayName, String args) {
        this.displayName = displayName;
        this.args = args;
    }
}
