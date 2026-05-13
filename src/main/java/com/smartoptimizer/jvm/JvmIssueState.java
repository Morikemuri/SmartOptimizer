package com.smartoptimizer.jvm;

import java.nio.file.Path;
import java.util.List;

public class JvmIssueState {

    private static volatile JvmBugEntry                            detectedCrash    = null;
    private static volatile List<JvmRecommendationEngine.Recommendation> recommendations = List.of();
    private static volatile List<JvmArgsValidator.ValidationIssue> validationIssues = List.of();
    private static volatile boolean alertShown      = false;
    private static volatile boolean gateShown       = false;
    private static volatile boolean wizardPending   = false;
    private static volatile Path    crashFilePath   = null;

    // ── Crash / Recommendations ──────────────────────────────────────────────
    public static void setCrash(JvmBugEntry entry)                              { detectedCrash = entry; }
    public static void setRecommendations(List<JvmRecommendationEngine.Recommendation> r) { recommendations = List.copyOf(r); }
    public static void setValidationIssues(List<JvmArgsValidator.ValidationIssue> v)     { validationIssues = List.copyOf(v); }
    public static void setCrashFilePath(Path p)                                 { crashFilePath = p; }
    public static Path getCrashFilePath()                                       { return crashFilePath; }

    public static JvmBugEntry                             getDetectedCrash()    { return detectedCrash; }
    public static List<JvmRecommendationEngine.Recommendation> getRecommendations() { return recommendations; }
    public static List<JvmArgsValidator.ValidationIssue>  getValidationIssues() { return validationIssues; }
    public static boolean hasCrash()                                            { return detectedCrash != null; }

    // ── Alert screen (in-world, shown once per session) ──────────────────────
    public static boolean shouldShowAlertScreen() {
        if (alertShown) return false;
        if (detectedCrash == null && recommendations.isEmpty() && validationIssues.isEmpty()) return false;
        // Suppress if the fix is already written in user_jvm_args.txt
        if (detectedCrash != null && JvmArgWriter.isAlreadyApplied(detectedCrash.fixArgs())) return false;
        if (detectedCrash == null && !recommendations.isEmpty()) {
            String recArgs = recommendations.stream()
                    .map(r -> r.args())
                    .reduce((a, b) -> a + " " + b).orElse("");
            if (!recArgs.isEmpty() && JvmArgWriter.isAlreadyApplied(recArgs)) return false;
        }
        return true;
    }
    public static void markAlertShown() { alertShown = true; }

    // ── Gate screen (title screen, shown once per session) ───────────────────
    public static boolean shouldShowGateScreen() {
        if (gateShown) return false;
        return JvmSentinelManager.exists() || LaunchLoopDetector.isLoopDetected();
    }
    public static void markGateShown() { gateShown = true; }

    // ── First-launch wizard ──────────────────────────────────────────────────
    public static void setWizardPending(boolean v) { wizardPending = v; }
    public static boolean isWizardPending()         { return wizardPending; }
}
