package com.smartoptimizer.compatibility;

import com.smartoptimizer.SmartOptimizerMod;

import java.util.Map;

/**
 * Central compat orchestrator. Each external mod gets its own compat block;
 * failures are isolated so one broken integration never affects the others.
 */
public class CompatManager {

    public static void init(Map<String, Boolean> mods) {
        run("rubidium",   () -> RubidiumCompat.init(mods));
        // Future slots: EmbeddiumCompat, OculusCompat, etc.
    }

    private static void run(String label, Runnable block) {
        try {
            block.run();
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[CompatManager] {} integration error: {}", label, e.getMessage());
        }
    }
}
