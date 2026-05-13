package com.smartoptimizer.rules;

import com.smartoptimizer.detection.HardwareInfo;
import com.smartoptimizer.presets.Preset;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RuleEngine {

    private static final List<PerformanceRule> RULES = Arrays.asList(
        new LowRAMRule(),
        new WeakCPURule(),
        new HighEntityRule()
    );

    public static Preset evaluate(HardwareInfo hardware, Map<String, Boolean> mods) {
        for (PerformanceRule rule : RULES) {
            if (rule.applies(hardware, mods)) {
                return rule.evaluate(hardware, mods);
            }
        }
        return defaultPreset(hardware);
    }

    private static Preset defaultPreset(HardwareInfo hardware) {
        long ramGB = hardware.getTotalRamGB();
        if (ramGB < 6)  return Preset.POTATO;
        if (ramGB < 12) return Preset.BALANCED;
        return Preset.HIGH_END;
    }
}
