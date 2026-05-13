package com.smartoptimizer.client;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.config.ConfigBackupSystem;
import com.smartoptimizer.config.ConfigManager;
import com.smartoptimizer.config.ConfigPatchEngine;
import com.smartoptimizer.detection.HardwareDetector;
import com.smartoptimizer.detection.HardwareInfo;
import com.smartoptimizer.detection.ModDetector;
import com.smartoptimizer.jvm.JvmArgWriter;
import com.smartoptimizer.jvm.JvmProfile;
import com.smartoptimizer.presets.Preset;
import com.smartoptimizer.presets.PresetSelector;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OneClickOptimizer {

    public record Result(
            boolean backupCreated,
            Path    backupPath,
            int     modsPatched,
            boolean jvmWritten,
            Preset  preset,
            List<String> log
    ) {}

    /** Runs the full optimization chain synchronously. */
    public static Result run() {
        List<String> log = new ArrayList<>();
        boolean backupCreated = false;
        Path backupPath = null;
        int modsPatched = 0;
        boolean jvmWritten = false;
        Preset preset = Preset.BALANCED;

        try {
            HardwareInfo hw  = HardwareDetector.detect();
            Map<String, Boolean> mods = ModDetector.detect();

            // 1 - Backup configs first
            backupPath = ConfigBackupSystem.backup();
            backupCreated = backupPath != null;
            if (backupCreated) log.add("[+] Backup created: " + backupPath.getFileName());
            else log.add("[!] Backup failed (non-fatal)");

            // 2 - Apply graphics preset
            preset = PresetSelector.select(hw, mods);
            ConfigManager.apply(preset, mods);
            log.add("[+] Applied graphics preset: " + preset.name());

            // 3 - Patch mod configs
            var patches = ConfigPatchEngine.patch(hw, mods);
            modsPatched = patches.size();
            patches.forEach(p -> log.add("[+] Optimised " + p.modName() + " (" + p.applied().size() + " settings)"));
            if (modsPatched == 0) log.add("  No tunable mod configs found");

            // 4 - Write JVM profile
            String jvmArgs = pickJvmProfile(hw);
            jvmWritten = JvmArgWriter.applyArgs(jvmArgs);
            if (jvmWritten) log.add("[+] JVM profile written (restart required)");
            else log.add("[!] JVM write failed - apply manually");

            // 5 - Summary
            log.add("[+] Optimized " + (modsPatched + 1) + " configs");

        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[OneClick] Error: {}", e.getMessage(), e);
            log.add("[!] Error: " + e.getMessage());
        }

        return new Result(backupCreated, backupPath, modsPatched, jvmWritten, preset, log);
    }

    private static String pickJvmProfile(HardwareInfo hw) {
        long ram = hw.getTotalRamGB();
        // High RAM + many cores → Performance profile, otherwise Balanced
        return (ram >= 12 && hw.getCpuCores() >= 8)
                ? JvmProfile.PERFORMANCE.args
                : JvmProfile.BALANCED.args;
    }
}
