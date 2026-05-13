package com.smartoptimizer.rules;

import com.smartoptimizer.detection.HardwareInfo;
import com.smartoptimizer.presets.Preset;

import java.util.Map;

public interface PerformanceRule {
    boolean applies(HardwareInfo hardware, Map<String, Boolean> mods);
    Preset evaluate(HardwareInfo hardware, Map<String, Boolean> mods);
}
