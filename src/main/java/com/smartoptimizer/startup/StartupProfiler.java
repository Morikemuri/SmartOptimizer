package com.smartoptimizer.startup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smartoptimizer.SmartOptimizerMod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StartupProfiler {

    // Called at the very first line of SmartOptimizerMod constructor
    public static final long PROCESS_START_MS = System.currentTimeMillis();

    private static volatile long titleScreenMs = -1;
    private static volatile ProfileResult lastResult = null;
    private static volatile boolean resultShown = false;

    /**
     * Known relative load cost of heavy mods (0–100).
     * Used to distribute the measured startup time proportionally.
     */
    private static final Map<String, Integer> MOD_WEIGHTS = new LinkedHashMap<>();
    static {
        MOD_WEIGHTS.put("create",             100);
        MOD_WEIGHTS.put("distanthorizons",     90);
        MOD_WEIGHTS.put("mekanism",            75);
        MOD_WEIGHTS.put("biomesoplenty",       65);
        MOD_WEIGHTS.put("twilightforest",      60);
        MOD_WEIGHTS.put("immersiveengineering",55);
        MOD_WEIGHTS.put("thermal",             50);
        MOD_WEIGHTS.put("botania",             45);
        MOD_WEIGHTS.put("ae2",                 45);
        MOD_WEIGHTS.put("refinedstorage",      40);
        MOD_WEIGHTS.put("bloodmagic",          35);
        MOD_WEIGHTS.put("jaopca",              35);
        MOD_WEIGHTS.put("ftbquests",           30);
        MOD_WEIGHTS.put("jei",                 25);
        MOD_WEIGHTS.put("journeymap",          25);
    }

    public record ModTiming(String modId, String displayName, long estimatedMs) {}
    public record ProfileResult(long totalMs, List<ModTiming> slowMods, int modCount) {}

    /** Called when the title screen first appears. Computes and saves the profile. */
    public static void onTitleScreenReady() {
        if (titleScreenMs >= 0) return; // already captured
        titleScreenMs = System.currentTimeMillis();
        long total = titleScreenMs - PROCESS_START_MS;

        Set<String> loadedIds = new HashSet<>();
        ModList.get().getMods().forEach(m -> loadedIds.add(m.getModId().toLowerCase()));

        // Build proportional estimates for heavy loaded mods
        int totalWeight = 0;
        List<Map.Entry<String, Integer>> present = new ArrayList<>();
        for (var entry : MOD_WEIGHTS.entrySet()) {
            if (loadedIds.contains(entry.getKey())) {
                present.add(entry);
                totalWeight += entry.getValue();
            }
        }

        // Attribute ~60% of startup to heavy mods, the rest to base Forge + small mods
        long heavyBudget = (long)(total * 0.60);
        List<ModTiming> slowMods = new ArrayList<>();
        for (var entry : present) {
            long ms = totalWeight > 0 ? heavyBudget * entry.getValue() / totalWeight : 0;
            if (ms > 500) { // only show mods with >0.5 s estimated impact
                String display = capitalize(entry.getKey());
                slowMods.add(new ModTiming(entry.getKey(), display, ms));
            }
        }
        slowMods.sort(Comparator.comparingLong(ModTiming::estimatedMs).reversed());

        lastResult = new ProfileResult(total, slowMods, loadedIds.size());
        saveProfile(lastResult);
        SmartOptimizerMod.LOGGER.info("[Startup] Total: {}ms | heavy mods: {}", total, slowMods.size());
    }

    private static void saveProfile(ProfileResult r) {
        try {
            Path file = FMLPaths.GAMEDIR.get()
                    .resolve("config/smartoptimizer/startup_profile.json");
            Files.createDirectories(file.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(file, gson.toJson(r));
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.debug("[Startup] Save failed: {}", e.getMessage());
        }
    }

    private static String capitalize(String id) {
        if (id.isEmpty()) return id;
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    public static ProfileResult getLastResult()   { return lastResult; }
    public static boolean       isResultShown()   { return resultShown; }
    public static void          markResultShown() { resultShown = true; }
    public static boolean       isTriggered()     { return titleScreenMs >= 0; }

    public static boolean shouldShowResult() {
        return !resultShown && lastResult != null;
    }
}
