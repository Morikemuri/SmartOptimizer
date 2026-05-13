package com.smartoptimizer.core;

import net.minecraft.client.ParticleStatus;

public class OverlayState {

    // --- Startup report (10s per world join) ---
    private static volatile PerformanceReport report      = null;
    private static volatile long worldJoinTime            = -1;
    private static final long STARTUP_SHOW_MS             = 10_000;

    // --- Startup info from StartupOptimizer ---
    public static volatile int    startupModCount         = 0;
    public static volatile String startupModpackType      = "";

    // --- Alert system (significant events only) ---
    private static volatile String alertMessage           = null;
    private static volatile long   alertExpiry            = 0;
    private static final long ALERT_SHOW_MS               = 5_000;

    // --- Pinned mode (user toggled via keybind) ---
    public static volatile boolean pinned                 = false;

    // --- Draggable position ---
    public static volatile int posX                       = -1;
    public static volatile int posY                       = -1;

    // --- Live settings (updated every 20 ticks by DynamicSettingsManager) ---
    private static volatile int            liveRender    = -1;
    private static volatile int            liveSim       = -1;
    private static volatile ParticleStatus liveParticles = null;
    private static volatile int            liveFps       = -1;

    // ── Report ───────────────────────────────────────────────────────────────
    public static void setReport(PerformanceReport r) { report = r; }
    public static PerformanceReport getReport()       { return report; }

    public static void setStartupInfo(int modCount, String type) {
        startupModCount    = modCount;
        startupModpackType = type;
    }

    public static void onWorldJoin() { worldJoinTime = System.currentTimeMillis(); }

    public static void startTimerIfNeeded() {
        if (worldJoinTime < 0) worldJoinTime = System.currentTimeMillis();
    }

    public static boolean isStartupReportVisible() {
        if (worldJoinTime < 0) return false;
        return System.currentTimeMillis() - worldJoinTime < STARTUP_SHOW_MS;
    }

    // ── Alerts ───────────────────────────────────────────────────────────────
    public static void triggerAlert(String message) {
        alertMessage = message;
        alertExpiry  = System.currentTimeMillis() + ALERT_SHOW_MS;
    }

    public static String getActiveAlert() {
        if (alertMessage == null) return null;
        if (System.currentTimeMillis() > alertExpiry) { alertMessage = null; return null; }
        return alertMessage;
    }

    // ── Live settings ─────────────────────────────────────────────────────────
    public static void setLiveSettings(int render, int sim, ParticleStatus particles, int fps) {
        liveRender    = render;
        liveSim       = sim;
        liveParticles = particles;
        liveFps       = fps;
    }

    public static int            getLiveRender()    { return liveRender; }
    public static int            getLiveSim()       { return liveSim; }
    public static ParticleStatus getLiveParticles() { return liveParticles; }
    public static int            getLiveFps()       { return liveFps; }
    public static boolean        hasLiveData()      { return liveRender >= 0; }
}
