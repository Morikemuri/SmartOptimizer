package com.smartoptimizer.presets;

import com.smartoptimizer.detection.HardwareInfo;
import com.smartoptimizer.rules.RuleEngine;

import java.util.Map;

public class PresetSelector {

    public static Preset select(HardwareInfo hardware, Map<String, Boolean> mods) {
        return RuleEngine.evaluate(hardware, mods);
    }
}
