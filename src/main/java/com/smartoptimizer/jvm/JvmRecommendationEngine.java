package com.smartoptimizer.jvm;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.detection.HardwareInfo;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JvmRecommendationEngine {

    public record Recommendation(String id, String description, String args) {}

    public static List<Recommendation> analyze(HardwareInfo hardware, Map<String, Boolean> mods) {
        List<Recommendation> recs = new ArrayList<>();
        try {
            String currentArgs = String.join(" ",
                    ManagementFactory.getRuntimeMXBean().getInputArguments());
            String cpuVendor   = JvmBugDatabase.detectCpuVendor();
            int    cores       = hardware.getCpuCores();
            long   ramGB       = hardware.getTotalRamGB();
            int    javaVersion = Runtime.version().feature();
            boolean hasGpu = Boolean.TRUE.equals(mods.get("embeddium"))
                          || Boolean.TRUE.equals(mods.get("rubidium"));

            boolean hasZGC    = currentArgs.contains("UseZGC");
            boolean hasG1     = currentArgs.contains("UseG1GC");
            boolean hasNUMA   = currentArgs.contains("UseNUMA");
            boolean hasTouch  = currentArgs.contains("AlwaysPreTouch");
            boolean tieredSafe = currentArgs.contains("TieredStopAtLevel=3")
                              || currentArgs.contains("TieredStopAtLevel=1");

            // AMD multi-core: ZGC + NUMA for better GC behavior
            if (cpuVendor.contains("AMD") && cores >= 8 && javaVersion >= 15 && !hasZGC) {
                recs.add(new Recommendation(
                    "amd_zgc_numa",
                    "AMD multi-core: ZGC + NUMA reduces GC pauses",
                    "-XX:+UseZGC -XX:+UnlockExperimentalVMOptions -XX:+UseNUMA"
                ));
            }

            // Intel + G1GC + large RAM: tune region size
            if (cpuVendor.contains("Intel") && hasG1 && ramGB >= 16) {
                recs.add(new Recommendation(
                    "intel_g1_tune",
                    "Intel + G1GC: larger regions reduce fragmentation",
                    "-XX:G1HeapRegionSize=32M"
                ));
            }

            // AlwaysPreTouch if not set and RAM >= 8GB
            if (!hasTouch && ramGB >= 8) {
                recs.add(new Recommendation(
                    "always_pre_touch",
                    "AlwaysPreTouch: pre-allocate heap to cut startup GC",
                    "-XX:+AlwaysPreTouch"
                ));
            }

            // AMD + Java 17 + GPU mod: proactive JIT safety (if no crash-based entry took priority)
            if (cpuVendor.contains("AMD") && javaVersion == 17 && hasGpu && !tieredSafe) {
                recs.add(new Recommendation(
                    "amd_jit_safety",
                    "AMD + Java 17 + Embeddium: prevent potential JIT crash",
                    "-XX:TieredStopAtLevel=3"
                ));
            }

        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[JvmRecommendationEngine] Analysis failed: {}", e.getMessage());
        }

        // Send report if any recommendations were generated (means something was suboptimal)
        if (!recs.isEmpty()) {
            try {
                List<String> currentArgsList = ManagementFactory.getRuntimeMXBean().getInputArguments();
                String javaVersion = String.valueOf(Runtime.version().feature());
                String recIds = recs.stream().map(Recommendation::id).collect(java.util.stream.Collectors.joining(","));
                JvmRemoteFetcher.sendReport("jvm_args_suboptimal", javaVersion, currentArgsList,
                        "Recommendations triggered: " + recIds);
            } catch (Exception ignored) {}
        }

        return recs;
    }
}
