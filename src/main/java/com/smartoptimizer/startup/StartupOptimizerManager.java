package com.smartoptimizer.startup;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.core.OverlayState;
import com.smartoptimizer.presets.Preset;

import java.util.List;

public class StartupOptimizerManager {

    private static volatile ModpackProfile profile = null;

    public static void initialize(Preset activePreset) {
        long start = System.currentTimeMillis();
        try {
            int modCount       = ModpackAnalyzer.getModCount();
            ModpackType type   = ModpackAnalyzer.classify(modCount);
            List<String> crit  = CriticalModDetector.detect();
            long elapsed       = System.currentTimeMillis() - start;

            profile = new ModpackProfile(modCount, type, crit, elapsed);
            SmartOptimizerMod.LOGGER.info("Startup: {} mods, type={}, critical={}, {}ms",
                    modCount, type, crit, elapsed);

            EarlyConfigOptimizer.apply(profile, activePreset);
            StartupCacheManager.save(profile);

            OverlayState.setStartupInfo(modCount, type.name());
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.error("StartupOptimizer failed: {}", e.getMessage(), e);
        }
    }

    public static ModpackProfile getProfile() { return profile; }
}
