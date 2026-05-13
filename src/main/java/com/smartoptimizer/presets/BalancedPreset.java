package com.smartoptimizer.presets;

import com.smartoptimizer.config.ConfigEntry;

import java.util.Arrays;
import java.util.List;

public class BalancedPreset {

    public static List<ConfigEntry> getEntries() {
        return Arrays.asList(
            new ConfigEntry("minecraft", "options.txt", "renderDistance",       "10"),
            new ConfigEntry("minecraft", "options.txt", "simulationDistance",   "8"),
            new ConfigEntry("minecraft", "options.txt", "particles",            "1"),
            new ConfigEntry("minecraft", "options.txt", "entityDistanceScaling","0.75")
        );
    }
}
