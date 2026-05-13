package com.smartoptimizer.jvm;

import com.smartoptimizer.SmartOptimizerMod;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class JvmCrashDetector {

    private static final long MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000; // 7 days
    private static final int  MAX_LINES  = 300;

    public record ParsedJvmCrash(String content, String fileName, long ageMs, java.nio.file.Path filePath) {}

    /** Scans for hs_err_pid*.log JVM crash files in the game directory. */
    public static ParsedJvmCrash findRecentCrash(Path gameDir) {
        try {
            File[] files = gameDir.toFile().listFiles(
                f -> f.isFile() && f.getName().startsWith("hs_err_pid") && f.getName().endsWith(".log"));
            if (files == null || files.length == 0) return null;

            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            File latest = files[0];

            long ageMs = System.currentTimeMillis() - latest.lastModified();
            if (ageMs > MAX_AGE_MS) return null;

            List<String> lines = Files.readAllLines(latest.toPath());
            int limit = Math.min(lines.size(), MAX_LINES);
            String content = String.join("\n", lines.subList(0, limit));

            SmartOptimizerMod.LOGGER.info("[JvmCrashDetector] Found: {} ({} hours old)",
                    latest.getName(), ageMs / 3_600_000L);

            // Send anonymous report to SmartOptimizer Worker
            String javaVersion = String.valueOf(Runtime.version().feature());
            List<String> jvmArgs = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
            String crashType = "jvm_crash_" + latest.getName().replaceAll("[^a-zA-Z0-9_]", "_");
            JvmRemoteFetcher.sendReport(crashType, javaVersion, jvmArgs, "JVM crash file detected: " + latest.getName());

            return new ParsedJvmCrash(content, latest.getName(), ageMs, latest.toPath());

        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[JvmCrashDetector] Scan failed: {}", e.getMessage());
            return null;
        }
    }
}
