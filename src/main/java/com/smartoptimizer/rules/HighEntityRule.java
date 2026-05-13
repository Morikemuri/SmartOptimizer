package com.smartoptimizer.rules;

import com.smartoptimizer.detection.HardwareInfo;
import com.smartoptimizer.presets.Preset;

import java.util.Map;

public class HighEntityRule implements PerformanceRule {

    @Override
    public boolean applies(HardwareInfo hardware, Map<String, Boolean> mods) {
        return Boolean.TRUE.equals(mods.get("entityculling")) && hardware.getTotalRamMB() < 8192;
    }

    @Override
    public Preset evaluate(HardwareInfo hardware, Map<String, Boolean> mods) {
        return Preset.BALANCED;
    }
}
