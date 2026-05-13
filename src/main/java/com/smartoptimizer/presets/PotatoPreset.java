package com.smartoptimizer.presets;

import com.smartoptimizer.config.ConfigEntry;

import java.util.Arrays;
import java.util.List;

public class PotatoPreset {

    public static List<ConfigEntry> getEntries() {
        return Arrays.asList(
            new ConfigEntry("minecraft", "options.txt", "renderDistance",       "6"),
            new ConfigEntry("minecraft", "options.txt", "simulationDistance",   "5"),
            new ConfigEntry("minecraft", "options.txt", "particles",            "2"),
            new ConfigEntry("minecraft", "options.txt", "entityDistanceScaling","0.5")
        );
    }
}
