package com.smartoptimizer.core;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.compatibility.CompatManager;
import com.smartoptimizer.compatibility.CrashPreventor;
import com.smartoptimizer.config.ConfigManager;
import com.smartoptimizer.detection.CompatibilityChecker;
import com.smartoptimizer.detection.HardwareDetector;
import com.smartoptimizer.detection.HardwareInfo;
import com.smartoptimizer.detection.ModDetector;
import com.smartoptimizer.jvm.JvmArgsValidator;
import com.smartoptimizer.jvm.JvmBugDatabase;
import com.smartoptimizer.jvm.JvmBugEntry;
import com.smartoptimizer.jvm.JvmCrashDetector;
import com.smartoptimizer.jvm.JvmIssueState;
import com.smartoptimizer.jvm.JvmRecommendationEngine;
import com.smartoptimizer.jvm.LaunchLoopDetector;
import com.smartoptimizer.presets.Preset;
import com.smartoptimizer.presets.PresetSelector;
import com.smartoptimizer.startup.StartupOptimizerManager;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OptimizerManager {

    public static void initialize() {
        try {
            HardwareInfo hardware = HardwareDetector.detect();
            Map<String, Boolean> mods = ModDetector.detect();
            SmartOptimizerMod.LOGGER.info("Detected: {}MB RAM / {} cores",
                    hardware.getTotalRamMB(), hardware.getCpuCores());

            List<CompatibilityChecker.CompatWarning> compatWarnings =
                    CompatibilityChecker.check(mods);

            CompatManager.init(mods);

            FirstLaunchController controller = new FirstLaunchController();
            Preset preset = PresetSelector.select(hardware, mods);
            Path gameDir  = FMLPaths.GAMEDIR.get();

            if (controller.isFirstLaunch()) {
                // Show wizard instead of auto-applying - wizard will call markApplied()
                JvmIssueState.setWizardPending(true);
                SmartOptimizerMod.LOGGER.info("First launch - wizard pending");
                // Apply balanced preset silently as default in case wizard is dismissed
                ConfigManager.apply(preset, mods);
            } else {
                SmartOptimizerMod.LOGGER.info("Profile already applied ({}), showing report.", preset.name());
            }

            CrashPreventor.PreventionResult prevention = CrashPreventor.apply(gameDir, mods, compatWarnings);
            PerformanceReport report = PerformanceAnalyzer.analyze(hardware, mods, preset, compatWarnings);
            if (prevention.safeModeApplied()) {
                report.setSafeModeApplied(true);
                report.getChanges().addAll(prevention.actions());
            }
            OverlayState.setReport(report);
            logReport(report);

            StartupOptimizerManager.initialize(preset);

            // Launch loop detection (must run before JVM analysis)
            LaunchLoopDetector.check(gameDir);

            // JVM analysis: crash detection + proactive recommendations + validator
            runJvmAnalysis(gameDir, hardware, mods);

        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.error("SmartOptimizer init error: {}", e.getMessage(), e);
        }
    }

    private static void runJvmAnalysis(Path gameDir, HardwareInfo hardware, Map<String, Boolean> mods) {
        try {
            JvmCrashDetector.ParsedJvmCrash crash = JvmCrashDetector.findRecentCrash(gameDir);
            if (crash != null) JvmIssueState.setCrashFilePath(crash.filePath());

            Optional<JvmBugEntry> bug = JvmBugDatabase.match(crash, mods);
            if (bug.isPresent()) {
                JvmBugEntry entry = bug.get();
                JvmIssueState.setCrash(entry);
                SmartOptimizerMod.LOGGER.warn("[JVM] Issue detected: {} - fix: {}",
                        entry.description(), entry.fixArgs());
            } else {
                List<JvmRecommendationEngine.Recommendation> recs =
                        JvmRecommendationEngine.analyze(hardware, mods);
                JvmIssueState.setRecommendations(recs);
                recs.forEach(r -> SmartOptimizerMod.LOGGER.info("[JVM] Recommendation: {}", r.description()));
            }

            // Always validate current JVM args
            List<JvmArgsValidator.ValidationIssue> valIssues = JvmArgsValidator.validate();
            if (!valIssues.isEmpty()) {
                JvmIssueState.setValidationIssues(valIssues);
                valIssues.forEach(v -> SmartOptimizerMod.LOGGER.warn("[JVM] Arg issue: {}", v.message()));
            }

        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[JVM] Analysis failed: {}", e.getMessage());
        }
    }

    private static void logReport(PerformanceReport report) {
        SmartOptimizerMod.LOGGER.info("Performance Report: +{}% estimated",
                report.getEstimatedImprovementPct());
        report.getBottlenecks().forEach(b ->
                SmartOptimizerMod.LOGGER.info("  bottleneck: {}", b));
        report.getChanges().forEach(c ->
                SmartOptimizerMod.LOGGER.info("  configured: {}", c));
        report.getCompatWarnings().forEach(w ->
                SmartOptimizerMod.LOGGER.warn("  compat{}: {}", w.fatal() ? " FATAL" : "", w.message()));
    }
}
