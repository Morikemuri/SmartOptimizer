package com.smartoptimizer.jvm;

import com.smartoptimizer.SmartOptimizerMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

public class JvmSentinelManager {

    private static Path sentinelPath() {
        return FMLPaths.GAMEDIR.get()
                .resolve("config/smartoptimizer/jvm_safe_mode.flag");
    }

    public static boolean exists() {
        return Files.exists(sentinelPath());
    }

    public static void create(String args, String reason) {
        try {
            Path p = sentinelPath();
            Files.createDirectories(p.getParent());
            Files.writeString(p, "recommendedArgs=" + args + "\nreason=" + reason + "\n");
            SmartOptimizerMod.LOGGER.info("[JvmSentinel] Created: reason={}", reason);
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[JvmSentinel] Create failed: {}", e.getMessage());
        }
    }

    public static void delete() {
        try { Files.deleteIfExists(sentinelPath()); }
        catch (Exception ignored) {}
    }

    /** Returns the recommended args from the sentinel file, or null. */
    public static String readArgs() {
        try {
            if (!exists()) return null;
            for (String line : Files.readAllLines(sentinelPath())) {
                if (line.startsWith("recommendedArgs="))
                    return line.substring("recommendedArgs=".length()).trim();
            }
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[JvmSentinel] Read failed: {}", e.getMessage());
        }
        return null;
    }
}
