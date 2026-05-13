package com.smartoptimizer.compatibility;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.config.ConfigWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes typed fix-actions from known_issues.json.
 *
 * Supported action strings:
 *   setOption:key=value        - writes key=value to options.txt
 *   forceSafeMode              - applies SafeModeManager (render dist, particles, shaders)
 *   disableMixin:ClassName     - marks Mixin disabled for next launch via DisabledMixinsRegistry
 *   disableIntegration:modid   - logs that an integration is skipped (future: hooks CompatManager)
 *   warnOnly                   - no config change; warning is shown in overlay by CompatibilityChecker
 */
public class FixActionExecutor {

    public static List<String> execute(String issueId, List<String> actions,
                                       Path gameDir, boolean[] safeModeTriggered) {
        List<String> applied = new ArrayList<>();
        for (String action : actions) {
            try {
                if (action.startsWith("setOption:")) {
                    applySetOption(action, gameDir, applied);
                } else if ("forceSafeMode".equals(action)) {
                    if (!safeModeTriggered[0]) {
                        SmartOptimizerMod.LOGGER.warn("[FixAction] forceSafeMode - cause: {}", issueId);
                        applied.addAll(SafeModeManager.apply(gameDir));
                        safeModeTriggered[0] = true;
                    }
                } else if (action.startsWith("disableMixin:")) {
                    String name = action.substring("disableMixin:".length()).trim();
                    DisabledMixinsRegistry.markDisabledNextLaunch(gameDir, name);
                    applied.add("disableMixin:" + name + " (next launch)");
                    SmartOptimizerMod.LOGGER.warn("[FixAction] Mixin {} disabled for next launch", name);
                } else if (action.startsWith("disableIntegration:")) {
                    String mod = action.substring("disableIntegration:".length()).trim();
                    applied.add("disableIntegration:" + mod);
                    SmartOptimizerMod.LOGGER.warn("[FixAction] Integration disabled: {}", mod);
                } else if ("warnOnly".equals(action)) {
                    // warning is shown in overlay via CompatibilityChecker - no config change here
                }
            } catch (Exception e) {
                SmartOptimizerMod.LOGGER.warn("[FixAction] action '{}' failed: {}", action, e.getMessage());
            }
        }
        return applied;
    }

    private static void applySetOption(String action, Path gameDir, List<String> applied) {
        String kv = action.substring("setOption:".length());
        int eq = kv.indexOf('=');
        if (eq <= 0) return;
        String key   = kv.substring(0, eq).trim();
        String value = kv.substring(eq + 1).trim();
        ConfigWriter.writeKeyValue(gameDir.resolve("options.txt"), key, value);
        applied.add("setOption: " + key + "=" + value);
        SmartOptimizerMod.LOGGER.info("[FixAction] setOption {}={}", key, value);
    }
}
