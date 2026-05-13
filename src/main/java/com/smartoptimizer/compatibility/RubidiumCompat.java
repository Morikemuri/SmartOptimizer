package com.smartoptimizer.compatibility;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.detection.ModDetector;

import java.util.Map;

public class RubidiumCompat {

    public static void init(Map<String, Boolean> mods) {
        if (!Boolean.TRUE.equals(mods.get("rubidium"))) return;

        String ver = ModDetector.getModVersion("rubidium");
        if (ver == null) {
            // Unknown version - skip potentially dangerous patches
            SmartOptimizerMod.LOGGER.warn(
                "[RubidiumCompat] Version unknown - running in fallback mode (no risky patches)");
            return;
        }

        if (ver.startsWith("0.6.") || ver.startsWith("0.7.1")) {
            // Known-bad versions: FluidRenderer NPE on flowing lava
            // Fix is handled by RubidiumFluidRendererFix Mixin (loaded via SmartOptimizerMixinPlugin)
            SmartOptimizerMod.LOGGER.warn(
                "[RubidiumCompat] {} - known FluidRenderer bug. Mixin patch active.", ver);
        } else if (ver.startsWith("0.7.") || ver.startsWith("0.8.") || ver.startsWith("0.9.")) {
            SmartOptimizerMod.LOGGER.info(
                "[RubidiumCompat] {} - stable version, no patches needed.", ver);
        } else {
            // Future/unknown version - safe fallback: only load non-risky integrations
            SmartOptimizerMod.LOGGER.warn(
                "[RubidiumCompat] {} - unrecognised version, fallback mode active.", ver);
        }
    }
}
