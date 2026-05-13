package com.smartoptimizer.rules;

import com.smartoptimizer.detection.HardwareInfo;
import com.smartoptimizer.presets.Preset;

import java.util.Map;

public class LowRAMRule implements PerformanceRule {

    @Override
    public boolean applies(HardwareInfo hardware, Map<String, Boolean> mods) {
        return hardware.getTotalRamMB() < 4096;
    }

    @Override
    public Preset evaluate(HardwareInfo hardware, Map<String, Boolean> mods) {
        return Preset.POTATO;
    }
}
