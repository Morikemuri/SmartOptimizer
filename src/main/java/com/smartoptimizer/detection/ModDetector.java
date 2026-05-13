package com.smartoptimizer.detection;

import com.smartoptimizer.SmartOptimizerMod;
import net.minecraftforge.fml.ModList;

import java.util.HashMap;
import java.util.Map;

public class ModDetector {

    private static final String[] TARGET_MODS = {
        // Rendering
        "rubidium", "embeddium", "oculus", "iris", "optifine",
        // Memory
        "ferritecore", "memoryleakfix",
        // Entity / World
        "entityculling", "starlight", "lithium", "phosphor",
        // Startup / General optimisation
        "modernfix", "lazydfu", "smoothboot", "performant",
        // Crash recovery (nice to know they're present)
        "notenoughcrashes", "vanillafix"
    };

    public static Map<String, Boolean> detect() {
        Map<String, Boolean> result = new HashMap<>();
        for (String modId : TARGET_MODS) {
            boolean found = safeIsLoaded(modId);
            result.put(modId, found);
            if (found) SmartOptimizerMod.LOGGER.info("  mod detected: {}", modId);
        }
        return result;
    }

    /** Returns the version string of a mod, or null if not loaded / error. */
    public static String getModVersion(String modId) {
        try {
            return ModList.get().getModContainerById(modId)
                    .map(mc -> mc.getModInfo().getVersion().toString())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** True if any GPU-accelerated renderer is installed. */
    public static boolean hasAnyRenderer(Map<String, Boolean> mods) {
        return Boolean.TRUE.equals(mods.get("rubidium"))
            || Boolean.TRUE.equals(mods.get("embeddium"))
            || Boolean.TRUE.equals(mods.get("optifine"));
    }

    private static boolean safeIsLoaded(String modId) {
        try {
            if (!ModList.get().isLoaded(modId)) return false;
            // Also verify the JAR still exists on disk — the user may have deleted it
            // without restarting; ModList keeps the entry but the file may be gone.
            try {
                java.nio.file.Path jar = ModList.get()
                        .getModContainerById(modId)
                        .orElseThrow()
                        .getModInfo()
                        .getOwningFile()
                        .getFile()
                        .getFilePath();
                if (jar != null && !java.nio.file.Files.exists(jar)) return false;
            } catch (Exception ignored) {
                // Forge API unavailable — fall back to trusting ModList
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
