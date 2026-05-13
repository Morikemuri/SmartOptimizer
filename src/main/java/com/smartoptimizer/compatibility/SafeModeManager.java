package com.smartoptimizer.compatibility;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.config.ConfigWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SafeModeManager {

    public static List<String> apply(Path gameDir) {
        List<String> applied = new ArrayList<>();
        try {
            Path options = gameDir.resolve("options.txt");
            write(options, "renderDistance",    "6",   applied, "smartoptimizer.report.safe.render");
            write(options, "simulationDistance","5",   applied, "smartoptimizer.report.safe.sim");
            write(options, "particles",         "2",   applied, "smartoptimizer.report.safe.particles");
            write(options, "entityDistanceSim", "0.5", applied, "smartoptimizer.report.safe.entity");

            Path shaderOpts = gameDir.resolve("optionsshaders.txt");
            if (shaderOpts.toFile().exists()) {
                write(shaderOpts, "shaderPack", "(internal)", applied, "smartoptimizer.report.safe.shaders");
            }
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[SafeMode] apply failed: {}", e.getMessage());
        }
        return applied;
    }

    private static void write(Path path, String key, String value,
                               List<String> applied, String label) {
        try {
            ConfigWriter.writeKeyValue(path, key, value);
            applied.add(label);
            SmartOptimizerMod.LOGGER.info("[SafeMode] {}", label);
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[SafeMode] Failed {}: {}", key, e.getMessage());
        }
    }
}
