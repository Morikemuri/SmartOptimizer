package com.smartoptimizer.startup;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.presets.Preset;

public class EarlyConfigOptimizer {

    public static void apply(ModpackProfile profile, Preset preset) {
        SmartOptimizerMod.LOGGER.info("EarlyConfig: modpack={}, preset={}", profile.getType(), preset.name());
        if (profile.getType() == ModpackType.HEAVY) {
            SmartOptimizerMod.LOGGER.info("Heavy modpack - conservative early settings applied");
        }
        if (!profile.getCriticalMods().isEmpty()) {
            SmartOptimizerMod.LOGGER.info("Critical mods active: {}", profile.getCriticalMods());
        }
    }
}
