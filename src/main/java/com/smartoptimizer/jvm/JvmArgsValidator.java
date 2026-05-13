package com.smartoptimizer.jvm;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class JvmArgsValidator {

    public record ValidationIssue(String message, boolean critical) {}

    public static List<ValidationIssue> validate() {
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        String joined = String.join(" ", args);
        List<ValidationIssue> issues = new ArrayList<>();

        // Deprecated CMS GC (removed in Java 14+)
        if (joined.contains("UseConcMarkSweepGC")) {
            issues.add(new ValidationIssue("CMS GC is deprecated/removed in Java 14+", true));
        }

        // Conflicting GC flags
        int gcCount = 0;
        if (joined.contains("UseG1GC"))      gcCount++;
        if (joined.contains("UseZGC"))       gcCount++;
        if (joined.contains("UseSerialGC"))  gcCount++;
        if (joined.contains("UseParallelGC")) gcCount++;
        if (gcCount > 1) {
            issues.add(new ValidationIssue("Multiple GC flags detected - may conflict", true));
        }

        // Heap size checks
        long xmx = parseHeapMB(args, "-Xmx");
        long xms = parseHeapMB(args, "-Xms");
        if (xmx > 0 && xmx < 1024) {
            issues.add(new ValidationIssue("Very low -Xmx: " + xmx + "MB (< 1 GB)", true));
        }
        if (xms > 0 && xmx > 0 && xms > xmx) {
            issues.add(new ValidationIssue("-Xms (" + xms + "MB) is larger than -Xmx (" + xmx + "MB)", true));
        }
        if (xmx > 0 && xmx > 16384) {
            issues.add(new ValidationIssue("-Xmx is very large (" + (xmx/1024) + "GB) - may hurt GC", false));
        }

        // UseNUMA + UseSerialGC combination
        if (joined.contains("UseNUMA") && joined.contains("UseSerialGC")) {
            issues.add(new ValidationIssue("UseNUMA is incompatible with SerialGC", true));
        }

        return issues;
    }

    private static long parseHeapMB(List<String> args, String prefix) {
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                String val = arg.substring(prefix.length()).trim().toLowerCase();
                try {
                    if (val.endsWith("g")) return Long.parseLong(val.substring(0, val.length() - 1)) * 1024;
                    if (val.endsWith("m")) return Long.parseLong(val.substring(0, val.length() - 1));
                    if (val.endsWith("k")) return Long.parseLong(val.substring(0, val.length() - 1)) / 1024;
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }
}
