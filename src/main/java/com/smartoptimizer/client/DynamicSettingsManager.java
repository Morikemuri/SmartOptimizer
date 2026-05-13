package com.smartoptimizer.client;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.core.OverlayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import org.lwjgl.glfw.GLFW;

/**
 * Tracks Minecraft settings in real-time and auto-adjusts them based on:
 * - Window focus (minimised → minimum resources)
 * - FPS (low FPS → reduce quality)
 * - User overrides (if user changed a setting manually, never touch it again)
 */
public class DynamicSettingsManager {

    // Baseline = what was set when we first initialised
    private static int            origRender    = -1;
    private static int            origSim       = -1;
    private static ParticleStatus origParticles = null;
    private static int            origFps       = -1;

    // Last value WE wrote (to detect user edits)
    private static int            autoRender    = -1;
    private static int            autoSim       = -1;
    private static ParticleStatus autoParticles = null;

    // True once a user has manually overridden a setting (we won't touch it again)
    private static boolean renderUserSet    = false;
    private static boolean simUserSet       = false;
    private static boolean particlesUserSet = false;

    // Focus state
    private static boolean wasFocused    = true;
    private static long    focusLostAt   = -1;           // time focus was lost, -1 = focused
    private static final long DEBOUNCE_MS = 350;

    // Rolling FPS
    private static int tickCounter   = 0;
    private static int fpsSampleSum  = 0;
    private static int fpsSampleCnt  = 0;
    private static int rollingAvgFps = 60;

    /**
     * Called every 20 client-ticks (~1 s) from AnalyticsCollector.
     */
    public static void tick(Minecraft mc) {
        if (mc.level == null) return;
        tickCounter++;

        Options opt = mc.options;
        int            curRender    = opt.renderDistance().get();
        int            curSim       = opt.simulationDistance().get();
        ParticleStatus curParticles = opt.particles().get();
        int            curFpsCap    = opt.framerateLimit().get();
        int            curFps       = mc.getFps();

        // ── Initialise baseline once ─────────────────────────────────────────
        if (origRender < 0) {
            origRender    = curRender;
            origSim       = curSim;
            origParticles = curParticles;
            origFps       = curFpsCap;
            autoRender    = curRender;
            autoSim       = curSim;
            autoParticles = curParticles;
        }

        // ── Detect user overrides ────────────────────────────────────────────
        if (!renderUserSet    && curRender    != autoRender)    renderUserSet    = true;
        if (!simUserSet       && curSim       != autoSim)       simUserSet       = true;
        if (!particlesUserSet && curParticles != autoParticles) particlesUserSet = true;

        // ── Update overlay with live values ──────────────────────────────────
        OverlayState.setLiveSettings(curRender, curSim, curParticles, curFps);

        // ── Rolling FPS average ───────────────────────────────────────────────
        fpsSampleSum += curFps;
        if (++fpsSampleCnt >= 5) {
            rollingAvgFps = fpsSampleSum / fpsSampleCnt;
            fpsSampleSum  = 0;
            fpsSampleCnt  = 0;
        }

        // ── Focus detection (every tick, not throttled) ───────────────────────
        boolean nowFocused = isFocused(mc);

        if (!nowFocused && wasFocused) {
            // Start or continue debounce
            if (focusLostAt < 0) focusLostAt = System.currentTimeMillis();
            if (System.currentTimeMillis() - focusLostAt >= DEBOUNCE_MS) {
                wasFocused  = false;
                focusLostAt = -1;
                if (!renderUserSet)    setRender(mc, 2);
                if (!simUserSet)       setSim(mc, 2);
                if (!particlesUserSet) setParticles(mc, ParticleStatus.MINIMAL);
                setFps(mc, 30);
                mc.options.save();
                SmartOptimizerMod.LOGGER.debug("[Dynamic] Focus lost - reduced settings");
            }
        } else if (nowFocused && !wasFocused) {
            // Restore immediately on focus return
            focusLostAt = -1;
            wasFocused  = true;
            if (!renderUserSet)    setRender(mc, origRender);
            if (!simUserSet)       setSim(mc, origSim);
            if (!particlesUserSet) setParticles(mc, origParticles);
            setFps(mc, origFps);
            mc.options.save();
            SmartOptimizerMod.LOGGER.debug("[Dynamic] Focus returned - restored settings");
        } else if (nowFocused) {
            focusLostAt = -1; // cancel debounce if focus came back mid-debounce
        }

        // ── FPS-based auto-tuning (only when focused, every 5 s) ─────────────
        if (tickCounter % 5 != 0) return;
        if (!nowFocused) return;

        if (rollingAvgFps < 20 && rollingAvgFps > 0) {
            if (!renderUserSet && autoRender > 4) {
                setRender(mc, Math.max(4, autoRender - 2));
                mc.options.save();
                SmartOptimizerMod.LOGGER.debug("[Dynamic] Low FPS ({}) → render={}", rollingAvgFps, autoRender);
            }
            if (!particlesUserSet && autoParticles != ParticleStatus.MINIMAL) {
                setParticles(mc, ParticleStatus.MINIMAL);
                mc.options.save();
            }
        } else if (rollingAvgFps > 50) {
            if (!renderUserSet && autoRender < origRender) {
                setRender(mc, Math.min(origRender, autoRender + 2));
                mc.options.save();
            }
            if (!particlesUserSet && autoParticles == ParticleStatus.MINIMAL
                    && origParticles != ParticleStatus.MINIMAL) {
                setParticles(mc, ParticleStatus.DECREASED);
                mc.options.save();
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setRender(Minecraft mc, int val) {
        autoRender = val;
        mc.options.renderDistance().set(val);
    }

    private static void setSim(Minecraft mc, int val) {
        autoSim = val;
        mc.options.simulationDistance().set(val);
    }

    private static void setParticles(Minecraft mc, ParticleStatus val) {
        autoParticles = val;
        mc.options.particles().set(val);
    }

    private static void setFps(Minecraft mc, int val) {
        mc.options.framerateLimit().set(val);
    }

    private static boolean isFocused(Minecraft mc) {
        try {
            return GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(), GLFW.GLFW_FOCUSED) == 1;
        } catch (Exception e) { return true; }
    }

    // ── Public accessors for the overlay ─────────────────────────────────────

    public static boolean isRenderUserSet()    { return renderUserSet; }
    public static boolean isSimUserSet()       { return simUserSet; }
    public static boolean isParticlesUserSet() { return particlesUserSet; }
    public static int     getRollingFps()      { return rollingAvgFps; }
}
