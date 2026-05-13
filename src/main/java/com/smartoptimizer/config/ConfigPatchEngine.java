package com.smartoptimizer.config;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.detection.HardwareInfo;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Applies hardware-adaptive configuration patches to known optimisation mods.
 * All patches are safe - they only improve performance, never break gameplay.
 */
public class ConfigPatchEngine {

    public record PatchResult(String modName, List<String> applied) {}

    /**
     * Patches configs for all detected mods.
     * @return list of results (one per patched mod)
     */
    public static List<PatchResult> patch(HardwareInfo hw, Map<String, Boolean> mods) {
        List<PatchResult> results = new ArrayList<>();
        Path gameDir   = FMLPaths.GAMEDIR.get();
        Path configDir = gameDir.resolve("config");

        long ramGB  = hw.getTotalRamGB();
        int  cores  = hw.getCpuCores();

        // ── Embeddium / Rubidium ─────────────────────────────────────────────
        boolean hasEmbeddium = Boolean.TRUE.equals(mods.get("embeddium"));
        boolean hasRubidium  = Boolean.TRUE.equals(mods.get("rubidium"));
        if (hasEmbeddium || hasRubidium) {
            String file = hasEmbeddium ? "embeddium-options.properties" : "rubidium-options.properties";
            Path p = configDir.resolve(file);
            if (Files.exists(p)) {
                List<String> applied = new ArrayList<>();
                int threads = Math.min(8, Math.max(2, cores / 2));
                if (set(p, "chunk_builder_threads", String.valueOf(threads)))
                    applied.add("chunk_builder_threads=" + threads);
                if (ramGB >= 6 && set(p, "use_compact_vertex_format", "false"))
                    applied.add("use_compact_vertex_format=false (quality)");
                if (ramGB < 6 && set(p, "use_compact_vertex_format", "true"))
                    applied.add("use_compact_vertex_format=true (saves RAM)");
                if (!applied.isEmpty()) results.add(new PatchResult(hasEmbeddium ? "Embeddium" : "Rubidium", applied));
            }
        }

        // ── EntityCulling ────────────────────────────────────────────────────
        if (Boolean.TRUE.equals(mods.get("entityculling"))) {
            Path p = configDir.resolve("entityculling.toml");
            if (Files.exists(p)) {
                int dist = ramGB < 8 ? 32 : ramGB < 16 ? 64 : 128;
                List<String> applied = new ArrayList<>();
                if (set(p, "cullingDistance", String.valueOf(dist)))
                    applied.add("cullingDistance=" + dist);
                if (!applied.isEmpty()) results.add(new PatchResult("EntityCulling", applied));
            }
        }

        // ── ModernFix ────────────────────────────────────────────────────────
        if (Boolean.TRUE.equals(mods.get("modernfix"))) {
            Path p = configDir.resolve("modernfix-forge.toml");
            if (Files.exists(p)) {
                List<String> applied = new ArrayList<>();
                if (set(p, "dynamicResources.enabled", "true"))
                    applied.add("dynamicResources.enabled=true");
                if (set(p, "perf.clear_other_threads.enabled", "true"))
                    applied.add("clear_other_threads=true");
                if (!applied.isEmpty()) results.add(new PatchResult("ModernFix", applied));
            }
        }

        // ── ImmediatelyFast ──────────────────────────────────────────────────
        if (Boolean.TRUE.equals(mods.get("immediatelyfast"))) {
            Path p = configDir.resolve("immediatelyfast.toml");
            if (Files.exists(p)) {
                List<String> applied = new ArrayList<>();
                if (set(p, "enabled", "true"))
                    applied.add("enabled=true");
                if (set(p, "hud_batching", "true"))
                    applied.add("hud_batching=true");
                if (!applied.isEmpty()) results.add(new PatchResult("ImmediatelyFast", applied));
            }
        }

        // ── Distant Horizons ─────────────────────────────────────────────────
        if (Boolean.TRUE.equals(mods.get("distanthorizons"))) {
            Path p = configDir.resolve("DistantHorizons.toml");
            if (!Files.exists(p)) p = configDir.resolve("distanthorizons.toml");
            if (Files.exists(p)) {
                List<String> applied = new ArrayList<>();
                // LOD chunk radius: lower = faster, higher = better view
                int lodRadius = ramGB < 8 ? 128 : ramGB < 16 ? 256 : 512;
                if (set(p, "chunkRenderDistance", String.valueOf(lodRadius)))
                    applied.add("chunkRenderDistance=" + lodRadius);
                int dhThreads = Math.min(6, Math.max(2, cores / 3));
                if (set(p, "numberOfWorldGenerationThreads", String.valueOf(dhThreads)))
                    applied.add("worldGenThreads=" + dhThreads);
                if (!applied.isEmpty()) results.add(new PatchResult("DistantHorizons", applied));
            }
        }

        // ── FerriteCore ──────────────────────────────────────────────────────
        if (Boolean.TRUE.equals(mods.get("ferritecore"))) {
            Path p = configDir.resolve("ferritecore.toml");
            if (Files.exists(p)) {
                List<String> applied = new ArrayList<>();
                if (set(p, "replaceNeighborLookup", "true"))
                    applied.add("replaceNeighborLookup=true");
                if (!applied.isEmpty()) results.add(new PatchResult("FerriteCore", applied));
            }
        }

        SmartOptimizerMod.LOGGER.info("[ConfigPatch] Patched {} mods", results.size());
        return results;
    }

    private static boolean set(Path file, String key, String value) {
        try {
            ConfigWriter.writeKeyValue(file, key, value);
            return true;
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.debug("[ConfigPatch] Skip {}/{}: {}", file.getFileName(), key, e.getMessage());
            return false;
        }
    }
}
