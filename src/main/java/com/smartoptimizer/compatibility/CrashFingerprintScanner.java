package com.smartoptimizer.compatibility;

import com.smartoptimizer.SmartOptimizerMod;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Scans the most recent crash report and looks for known stack-trace signatures.
 * If a match is found, returns the corresponding issue IDs so CrashPreventor
 * can apply fixes - even if the issue isn't matched by KnownIssuesDatabase
 * (e.g., because the version detection didn't catch it).
 */
public class CrashFingerprintScanner {

    private record Fingerprint(String issueId, List<String> mustContain, String description) {}

    private static final List<Fingerprint> FINGERPRINTS = List.of(
        new Fingerprint("rubidium_lava_crash",
            List.of("me.jellysquid.mods.sodium.client.render", "NullPointerException"),
            "Rubidium FluidRenderer NPE in crash report"),
        new Fingerprint("rubidium_lava_crash",
            List.of("FluidRenderer", "NullPointerException", "rubidium"),
            "Rubidium FluidRenderer NPE (short path)"),
        new Fingerprint("mixin_apply_failed",
            List.of("mixin apply failed", "FluidRenderer"),
            "Mixin application failure on FluidRenderer"),
        new Fingerprint("optifine_method_conflict",
            List.of("NoSuchMethodError", "optifine"),
            "OptiFine missing method - version mismatch"),
        new Fingerprint("optifine_class_conflict",
            List.of("ClassNotFoundException", "optifine"),
            "OptiFine class not found - installation broken"),
        new Fingerprint("embeddium_amd_crash",
            List.of("embeddium", "NullPointerException", "chunk"),
            "Embeddium chunk mesh NPE in crash report")
    );

    /** Returns list of issue IDs whose fingerprint matched the latest crash report. */
    public static List<String> scan(Path gameDir) {
        List<String> detected = new ArrayList<>();
        try {
            String content = readLatestCrashReport(gameDir);
            if (content == null) return detected;
            String lower = content.toLowerCase();

            for (Fingerprint fp : FINGERPRINTS) {
                boolean allMatch = fp.mustContain().stream()
                        .allMatch(token -> lower.contains(token.toLowerCase()));
                if (allMatch && !detected.contains(fp.issueId())) {
                    SmartOptimizerMod.LOGGER.warn("[CrashFingerprint] Detected: {} - {}",
                            fp.issueId(), fp.description());
                    detected.add(fp.issueId());
                }
            }
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[CrashFingerprint] Scan failed: {}", e.getMessage());
        }
        return detected;
    }

    private static String readLatestCrashReport(Path gameDir) {
        try {
            Path crashDir = gameDir.resolve("crash-reports");
            if (!crashDir.toFile().exists()) return null;

            File[] files = crashDir.toFile().listFiles(
                    f -> f.isFile() && f.getName().endsWith(".txt"));
            if (files == null || files.length == 0) return null;

            // Pick the most recent crash report
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            File latest = files[0];

            // Only read crash reports from the last 48 hours to avoid stale matches
            long ageMs = System.currentTimeMillis() - latest.lastModified();
            if (ageMs > 48L * 60 * 60 * 1000) return null;

            SmartOptimizerMod.LOGGER.info("[CrashFingerprint] Scanning: {}", latest.getName());
            return Files.readString(latest.toPath());
        } catch (Exception e) {
            return null;
        }
    }
}
