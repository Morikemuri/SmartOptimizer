package com.smartoptimizer.startup;

import com.smartoptimizer.SmartOptimizerMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class StartupCacheManager {

    private static final String CACHE_FILE = "smartoptimizer_startup.properties";

    public static void save(ModpackProfile profile) {
        Path path = FMLPaths.CONFIGDIR.get().resolve(CACHE_FILE);
        Properties props = new Properties();
        props.setProperty("modCount",     String.valueOf(profile.getModCount()));
        props.setProperty("type",         profile.getType().name());
        props.setProperty("criticalMods", String.join(",", profile.getCriticalMods()));
        props.setProperty("analysisMs",   String.valueOf(profile.getAnalysisMs()));
        try {
            props.store(Files.newBufferedWriter(path), "SmartOptimizer Startup Cache");
        } catch (IOException e) {
            SmartOptimizerMod.LOGGER.warn("Could not save startup cache: {}", e.getMessage());
        }
    }

    public static ModpackProfile load() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(CACHE_FILE);
        if (!Files.exists(path)) return null;
        try {
            Properties props = new Properties();
            props.load(Files.newBufferedReader(path));
            int modCount       = Integer.parseInt(props.getProperty("modCount", "0"));
            ModpackType type   = ModpackType.valueOf(props.getProperty("type", "LIGHT"));
            String critStr     = props.getProperty("criticalMods", "");
            List<String> crit  = critStr.isEmpty() ? List.of() : Arrays.asList(critStr.split(","));
            long analysisMs    = Long.parseLong(props.getProperty("analysisMs", "0"));
            return new ModpackProfile(modCount, type, crit, analysisMs);
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("Could not load startup cache: {}", e.getMessage());
            return null;
        }
    }
}
