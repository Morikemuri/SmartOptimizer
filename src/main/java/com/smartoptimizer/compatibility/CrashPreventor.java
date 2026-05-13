package com.smartoptimizer.compatibility;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.detection.CompatibilityChecker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrashPreventor {

    public record PreventionResult(boolean safeModeApplied, List<String> actions) {}

    public static PreventionResult apply(Path gameDir, Map<String, Boolean> mods,
                                         List<CompatibilityChecker.CompatWarning> warnings) {
        List<String> allActions = new ArrayList<>();
        boolean[] safeModeTriggered = {false};

        try {
            // --- 1. Crash fingerprint: detect patterns from previous crash reports ---
            List<String> fingerprintIds = CrashFingerprintScanner.scan(gameDir);
            for (String issueId : fingerprintIds) {
                KnownIssuesDatabase.KnownIssue issue = KnownIssuesDatabase.getById(issueId);
                if (issue != null) {
                    SmartOptimizerMod.LOGGER.warn(
                        "[CrashPreventor] Fingerprint match: {} - applying fix preemptively", issueId);
                    allActions.addAll(FixActionExecutor.execute(
                        issueId, issue.actions(), gameDir, safeModeTriggered));
                }
            }

            // --- 2. Known issues database: match current mod list ---
            List<KnownIssuesDatabase.KnownIssue> issues = KnownIssuesDatabase.getIssuesFor(mods);
            for (KnownIssuesDatabase.KnownIssue issue : issues) {
                SmartOptimizerMod.LOGGER.info("[CrashPreventor] Issue: {} [{}]",
                        issue.id(), issue.severity());
                allActions.addAll(FixActionExecutor.execute(
                    issue.id(), issue.actions(), gameDir, safeModeTriggered));
            }

            // --- 3. Severity-based escalation ---
            // If any issue is severe/fatal and safe mode wasn't triggered yet, force it
            boolean hasSevere = issues.stream()
                    .anyMatch(i -> "severe".equals(i.severity()) || "fatal".equals(i.severity()));
            if (hasSevere && !safeModeTriggered[0]) {
                SmartOptimizerMod.LOGGER.warn("[CrashPreventor] Severe issue - triggering safe mode");
                allActions.addAll(SafeModeManager.apply(gameDir));
                safeModeTriggered[0] = true;
            }

            // --- 4. Belt-and-suspenders: any fatal CompatWarning triggers safe mode ---
            if (!safeModeTriggered[0]) {
                for (CompatibilityChecker.CompatWarning w : warnings) {
                    if (w.fatal()) {
                        SmartOptimizerMod.LOGGER.warn(
                            "[CrashPreventor] SAFE MODE - fatal compat warning: {}", w.message());
                        allActions.addAll(SafeModeManager.apply(gameDir));
                        safeModeTriggered[0] = true;
                        break;
                    }
                }
            }

        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.error("[CrashPreventor] apply failed: {}", e.getMessage(), e);
        }

        return new PreventionResult(safeModeTriggered[0], allActions);
    }
}
