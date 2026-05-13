package com.smartoptimizer.presets;

import com.smartoptimizer.config.ConfigEntry;

import java.util.Arrays;
import java.util.List;

public class HighEndPreset {

    public static List<ConfigEntry> getEntries() {
        return Arrays.asList(
            new ConfigEntry("minecraft", "options.txt", "renderDistance",       "16"),
            new ConfigEntry("minecraft", "options.txt", "simulationDistance",   "12"),
            new ConfigEntry("minecraft", "options.txt", "particles",            "0"),
            new ConfigEntry("minecraft", "options.txt", "entityDistanceScaling","1.0")
        );
    }
}
