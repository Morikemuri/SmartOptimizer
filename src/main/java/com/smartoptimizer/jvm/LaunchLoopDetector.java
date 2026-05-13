package com.smartoptimizer.jvm;

import com.smartoptimizer.SmartOptimizerMod;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class LaunchLoopDetector {

    private static final int  CRASH_THRESHOLD  = 3;
    private static final long CRASH_WINDOW_MS  = 30L * 60 * 1000; // 30 minutes

    private static volatile boolean loopDetected = false;
    private static volatile int     crashCount   = 0;

    public static void check(Path gameDir) {
        try {
            Path configDir = gameDir.resolve("config/smartoptimizer");
            Files.createDirectories(configDir);
            Path counterFile = configDir.resolve("launch_counter.txt");

            long lastCheckTime  = 0;
            int  existingCount  = 0;
            if (Files.exists(counterFile)) {
                for (String line : Files.readAllLines(counterFile)) {
                    if (line.startsWith("time="))  lastCheckTime  = Long.parseLong(line.substring(5).trim());
                    if (line.startsWith("count=")) existingCount  = Integer.parseInt(line.substring(6).trim());
                }
            }

            long now = System.currentTimeMillis();

            // Reset if outside the window
            if (now - lastCheckTime > CRASH_WINDOW_MS) existingCount = 0;

            // Count hs_err files newer than last check
            int newCrashes = countNewCrashes(gameDir, lastCheckTime);
            int total      = existingCount + newCrashes;

            if (total >= CRASH_THRESHOLD) {
                loopDetected = true;
                crashCount   = total;
                SmartOptimizerMod.LOGGER.warn("[LaunchLoop] {} crashes in last 30 min - loop detected!", total);
            }

            Files.writeString(counterFile, "time=" + now + "\ncount=" + total + "\n");
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[LaunchLoop] Check failed: {}", e.getMessage());
        }
    }

    private static int countNewCrashes(Path gameDir, long since) {
        try {
            File[] files = gameDir.toFile().listFiles(
                    f -> f.isFile()
                      && f.getName().startsWith("hs_err_pid")
                      && f.getName().endsWith(".log")
                      && f.lastModified() > since);
            return files != null ? files.length : 0;
        } catch (Exception e) { return 0; }
    }

    public static boolean isLoopDetected() { return loopDetected; }
    public static int     getCrashCount()   { return crashCount; }

    public static void reset(Path configDir) {
        loopDetected = false;
        crashCount   = 0;
        try { Files.deleteIfExists(configDir.resolve("launch_counter.txt")); }
        catch (Exception ignored) {}
    }
}
