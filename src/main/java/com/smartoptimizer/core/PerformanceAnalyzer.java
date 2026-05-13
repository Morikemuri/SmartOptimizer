package com.smartoptimizer.core;

import com.smartoptimizer.detection.CompatibilityChecker;
import com.smartoptimizer.detection.HardwareInfo;
import com.smartoptimizer.presets.Preset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PerformanceAnalyzer {

    public static PerformanceReport analyze(HardwareInfo hardware, Map<String, Boolean> mods,
                                            Preset preset,
                                            List<CompatibilityChecker.CompatWarning> compatWarnings) {
        long ramGB = hardware.getTotalRamGB();
        int  cores = hardware.getCpuCores();

        // Treat Embeddium as equivalent to Rubidium (it's a maintained fork)
        boolean hasRubid  = bool(mods, "rubidium") || bool(mods, "embeddium");
        boolean hasFerr   = bool(mods, "ferritecore");
        boolean hasEntity = bool(mods, "entityculling");
        boolean hasOculus = bool(mods, "oculus");
        boolean hasIris   = bool(mods, "iris");
        boolean hasMFix   = bool(mods, "modernfix");
        boolean hasLazyDfu= bool(mods, "lazydfu");
        boolean hasMemLeak= bool(mods, "memoryleakfix");

        // Store i18n keys - translated at render time (I18n not ready during mod init)
        // Keys with numeric args use "key:number" format, parsed by OverlayRenderer.tr()
        List<String> bottlenecks = new ArrayList<>();
        if (ramGB < 4)      bottlenecks.add("smartoptimizer.report.bottleneck.critical_ram:" + ramGB);
        else if (ramGB < 6) bottlenecks.add("smartoptimizer.report.bottleneck.low_ram:" + ramGB);
        if (cores <= 4)     bottlenecks.add("smartoptimizer.report.bottleneck.weak_cpu:" + cores);
        if (!hasRubid)      bottlenecks.add("smartoptimizer.report.bottleneck.no_gpu_mod");
        if (!hasFerr)       bottlenecks.add("smartoptimizer.report.bottleneck.no_ferritecore");
        if (hasEntity)      bottlenecks.add("smartoptimizer.report.bottleneck.entities");
        if (bottlenecks.isEmpty()) bottlenecks.add("smartoptimizer.report.bottleneck.none");

        List<String> changes = new ArrayList<>();
        switch (preset) {
            case POTATO -> {
                changes.add("smartoptimizer.report.change.render_potato");
                changes.add("smartoptimizer.report.change.sim_potato");
                changes.add("smartoptimizer.report.change.particles_potato");
                changes.add("smartoptimizer.report.change.entity_potato");
            }
            case BALANCED -> {
                changes.add("smartoptimizer.report.change.render_balanced");
                changes.add("smartoptimizer.report.change.particles_balanced");
                changes.add("smartoptimizer.report.change.entity_balanced");
            }
            case HIGH_END -> {
                changes.add("smartoptimizer.report.change.render_high");
                changes.add("smartoptimizer.report.change.particles_high");
                changes.add("smartoptimizer.report.change.entity_high");
            }
        }
        if (hasEntity) changes.add("smartoptimizer.report.change.entityculling");
        if (hasFerr)   changes.add("smartoptimizer.report.change.ferritecore");
        if (hasRubid)  changes.add("smartoptimizer.report.change.rubidium");
        if (hasOculus || hasIris) changes.add("smartoptimizer.report.change.shaders");
        if (hasMFix)   changes.add("smartoptimizer.report.change.modernfix");
        if (hasLazyDfu)changes.add("smartoptimizer.report.change.lazydfu");
        if (hasMemLeak)changes.add("smartoptimizer.report.change.memoryleakfix");

        // --- Estimated improvement % ---
        int improvement = switch (preset) {
            case POTATO   -> (ramGB < 4 ? 35 : ramGB < 6 ? 28 : 20);
            case BALANCED -> (cores <= 4 ? 18 : 12);
            case HIGH_END -> 6;
        };
        if (hasFerr)    improvement += 5;
        if (hasEntity)  improvement += 5;
        if (hasRubid)   improvement += 10;
        if (hasMFix)    improvement += 3;
        if (hasLazyDfu) improvement += 2;
        if (hasMemLeak) improvement += 3;
        improvement = Math.min(improvement, 70);

        return new PerformanceReport(preset, improvement, bottlenecks, changes, compatWarnings);
    }

    private static boolean bool(Map<String, Boolean> mods, String key) {
        return Boolean.TRUE.equals(mods.get(key));
    }
}
