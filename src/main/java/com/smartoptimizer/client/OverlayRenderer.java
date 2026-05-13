package com.smartoptimizer.client;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.config.ConfigManager;
import com.smartoptimizer.core.OverlayState;
import com.smartoptimizer.core.PerformanceReport;
import com.smartoptimizer.detection.CompatibilityChecker;
import com.smartoptimizer.detection.ModDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class OverlayRenderer {

    private static final int BOX_W  = 230;
    private static final int PAD    = 5;
    private static final int LH     = 11;
    private static final int BTN_H  = 13;

    private static boolean dragging   = false;
    private static double  dragOffX   = 0;
    private static double  dragOffY   = 0;
    private static boolean prevLMB    = false;
    private static boolean wasInLevel = false;

    // Apply-button rect (updated each frame when visible)
    private static int     applyBtnX       = -1;
    private static int     applyBtnY       = -1;
    private static boolean applyBtnVisible = false;

    // Feedback line shown in overlay after pressing Apply
    private static String  applyFeedbackMsg  = "";
    private static long    applyFeedbackAt   = 0;
    private static final long FEEDBACK_MS    = 3_000;
    private static boolean presetApplied     = false;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        try {
            Minecraft mc = Minecraft.getInstance();
            boolean inLevel = mc.level != null;

            if (inLevel && !wasInLevel) OverlayState.onWorldJoin();
            wasInLevel = inLevel;
            if (!inLevel) return;

            OverlayState.startTimerIfNeeded();

            if (mc.screen != null) {
                handleInput(mc);
            } else {
                dragging = false;
            }

            render(event, mc);
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("OverlayRenderer error: {}", e.getMessage());
        }
    }

    private static void handleInput(Minecraft mc) {
        try {
            double guiScale = mc.getWindow().getGuiScale();
            double[] xArr = new double[1], yArr = new double[1];
            GLFW.glfwGetCursorPos(mc.getWindow().getWindow(), xArr, yArr);
            double mx = xArr[0] / guiScale;
            double my = yArr[0] / guiScale;

            boolean lmb = GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),
                    GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

            if (lmb && !prevLMB) {
                // Check apply-preset button first
                if (applyBtnVisible && mx >= applyBtnX && mx <= applyBtnX + BOX_W + PAD * 2
                        && my >= applyBtnY && my <= applyBtnY + BTN_H) {
                    applyRecommendedPreset();
                    prevLMB = lmb;
                    return;
                }
                // Start drag if cursor is over the overlay box
                int bx = resolveX(mc);
                int by = resolveY(mc);
                if (mx >= bx - PAD && mx <= bx + BOX_W + PAD && my >= by - PAD) {
                    dragging = true;
                    dragOffX = mx - bx;
                    dragOffY = my - by;
                }
            }
            if (!lmb) dragging = false;

            if (dragging) {
                OverlayState.posX = (int) (mx - dragOffX);
                OverlayState.posY = (int) (my - dragOffY);
            }
            prevLMB = lmb;
        } catch (Exception e) {
            dragging = false;
        }
    }

    private static void applyRecommendedPreset() {
        PerformanceReport rpt = OverlayState.getReport();
        if (rpt == null) return;
        com.smartoptimizer.presets.Preset preset = rpt.getPreset();

        Minecraft mc = Minecraft.getInstance();
        // Apply immediately via options API (same approach as DynamicSettingsManager)
        mc.execute(() -> {
            try {
                applyPresetToOptions(mc, preset);
                mc.options.save();
            } catch (Exception e) {
                SmartOptimizerMod.LOGGER.warn("[Overlay] Options apply failed: {}", e.getMessage());
            }
        });

        // Write mod configs in background
        Thread t = new Thread(() -> {
            try {
                java.util.Map<String, Boolean> mods = ModDetector.detect();
                ConfigManager.apply(preset, mods);
            } catch (Exception e) {
                SmartOptimizerMod.LOGGER.warn("[Overlay] Mod config apply failed: {}", e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();

        // Show feedback inside the overlay for 3 seconds
        applyFeedbackMsg = I18n.get("smartoptimizer.overlay.btn.applied", preset.name());
        applyFeedbackAt  = System.currentTimeMillis();
        presetApplied    = true;
    }

    private static void applyPresetToOptions(Minecraft mc, com.smartoptimizer.presets.Preset preset) {
        switch (preset) {
            case POTATO -> {
                mc.options.renderDistance().set(6);
                mc.options.simulationDistance().set(5);
                mc.options.particles().set(ParticleStatus.MINIMAL);
                mc.options.entityDistanceScaling().set(0.5d);
            }
            case BALANCED -> {
                mc.options.renderDistance().set(10);
                mc.options.simulationDistance().set(8);
                mc.options.particles().set(ParticleStatus.DECREASED);
                mc.options.entityDistanceScaling().set(0.75d);
            }
            case HIGH_END -> {
                mc.options.renderDistance().set(16);
                mc.options.simulationDistance().set(12);
                mc.options.particles().set(ParticleStatus.ALL);
                mc.options.entityDistanceScaling().set(1.0d);
            }
        }
    }

    private static void render(RenderGuiEvent.Post event, Minecraft mc) {
        try {
            record Line(String text, int color) {}
            List<Line> lines = new ArrayList<>();

            boolean showStartup = OverlayState.isStartupReportVisible();
            boolean showPinned  = OverlayState.pinned;
            String  alert       = OverlayState.getActiveAlert();
            PerformanceReport report = OverlayState.getReport();

            if (!showPinned) {
                applyBtnVisible = false;
                return;
            }

            String preset = report != null ? report.getPreset().name() : "?";

            // Auto-detect: if settings already match the preset, skip the "Apply" prompt
            if (!presetApplied && report != null) {
                int[] exp   = presetExpected(report.getPreset());
                String expP = presetExpectedParticles(report.getPreset());
                int curRend = mc.options.renderDistance().get();
                int curSim  = mc.options.simulationDistance().get();
                String curP = particlesLabel(mc.options.particles().get());
                int curEnt  = (int) Math.round(mc.options.entityDistanceScaling().get() * 100);
                if (curRend == exp[0] && curSim == exp[1] && curP.equals(expP) && curEnt == exp[2]) {
                    presetApplied = true;
                }
            }

            if (showPinned || showStartup) {
                lines.add(new Line(I18n.get("smartoptimizer.overlay.title", preset), 0xFFAA00));

                if (report != null && report.isSafeModeApplied()) {
                    lines.add(new Line(I18n.get("smartoptimizer.overlay.safemode"), 0xFF2222));
                }
                if (report != null) {
                    lines.add(new Line(
                            I18n.get("smartoptimizer.overlay.boost", report.getEstimatedImprovementPct()),
                            0x55FF55));

                    // "Was slow" only shows during the startup window, then fades away
                    if (showStartup && !report.getBottlenecks().isEmpty()) {
                        lines.add(new Line(I18n.get("smartoptimizer.overlay.section.slow"), 0x888888));
                        for (String b : report.getBottlenecks())
                            lines.add(new Line("  - " + tr(b), 0xFF8855));
                    }

                    if (presetApplied) {
                        // After user pressed Apply: show only settings that drifted from the applied preset
                        int[] exp   = presetExpected(report.getPreset());
                        String expP = presetExpectedParticles(report.getPreset());
                        int curRend = mc.options.renderDistance().get();
                        int curSim  = mc.options.simulationDistance().get();
                        String curP = particlesLabel(mc.options.particles().get());
                        int curEnt  = (int) Math.round(mc.options.entityDistanceScaling().get() * 100);
                        boolean anyDrift = curRend != exp[0] || curSim != exp[1]
                                || !curP.equals(expP) || curEnt != exp[2];
                        if (anyDrift) {
                            lines.add(new Line(I18n.get("smartoptimizer.overlay.section.drift"), 0x888888));
                            if (curRend != exp[0])
                                lines.add(new Line("  ! Render: " + curRend + " (rec. " + exp[0] + ")", 0xFFFF8844));
                            if (curSim != exp[1])
                                lines.add(new Line("  ! Sim: " + curSim + " (rec. " + exp[1] + ")", 0xFFFF8844));
                            if (!curP.equals(expP))
                                lines.add(new Line("  ! Particles: " + curP + " (rec. " + expP + ")", 0xFFFF8844));
                            if (curEnt != exp[2])
                                lines.add(new Line("  ! Entities: " + curEnt + "% (rec. " + exp[2] + "%)", 0xFFFF8844));
                        }
                    } else if (!report.getChanges().isEmpty()) {
                        lines.add(new Line(I18n.get("smartoptimizer.overlay.section.configured"), 0x888888));
                        for (String c : report.getChanges())
                            lines.add(new Line("  + " + tr(c), 0x55FF55));
                    }
                    if (!report.getCompatWarnings().isEmpty()) {
                        lines.add(new Line(I18n.get("smartoptimizer.overlay.section.warnings"), 0x888888));
                        for (CompatibilityChecker.CompatWarning w : report.getCompatWarnings()) {
                            int color = w.fatal() ? 0xFF2222 : 0xFFAA00;
                            lines.add(new Line("  ! " + w.message(), color));
                        }
                    }
                }

                if (OverlayState.hasLiveData()) {
                    lines.add(new Line(I18n.get("smartoptimizer.overlay.section.live"), 0x6688AA));
                    int fps = OverlayState.getLiveFps();
                    lines.add(new Line(
                            "  " + I18n.get("smartoptimizer.overlay.live.fps", fps),
                            fps < 20 ? 0xFF4444 : fps < 40 ? 0xFFAA00 : 0x55FF55));
                    lines.add(new Line(
                            "  " + I18n.get("smartoptimizer.overlay.live.render", OverlayState.getLiveRender()),
                            DynamicSettingsManager.isRenderUserSet() ? 0xFFCC88 : 0x55CCFF));
                    lines.add(new Line(
                            "  " + I18n.get("smartoptimizer.overlay.live.sim", OverlayState.getLiveSim()),
                            DynamicSettingsManager.isSimUserSet() ? 0xFFCC88 : 0x55CCFF));
                    lines.add(new Line(
                            "  " + I18n.get("smartoptimizer.overlay.live.particles",
                                    particlesLabel(OverlayState.getLiveParticles())),
                            DynamicSettingsManager.isParticlesUserSet() ? 0xFFCC88 : 0x55CCFF));
                }

                if (showStartup && !OverlayState.startupModpackType.isEmpty())
                    lines.add(new Line(I18n.get("smartoptimizer.overlay.modpack",
                            OverlayState.startupModpackType, OverlayState.startupModCount), 0x777777));
                if (showPinned)
                    lines.add(new Line(I18n.get("smartoptimizer.overlay.drag_hint"), 0x444444));

            } else {
                lines.add(new Line(I18n.get("smartoptimizer.overlay.alert", alert), 0xFF4444));
            }

            if (!applyFeedbackMsg.isEmpty() && System.currentTimeMillis() - applyFeedbackAt < FEEDBACK_MS)
                lines.add(new Line(applyFeedbackMsg, 0xFF55FF88));

            if (lines.isEmpty()) {
                applyBtnVisible = false;
                return;
            }

            var gui  = event.getGuiGraphics();
            var font = mc.font;

            // Word-wrap
            List<Line> wrapped = new ArrayList<>();
            for (Line line : lines) {
                if (font.width(line.text()) <= BOX_W) {
                    wrapped.add(line);
                } else {
                    String[] words = line.text().split(" ");
                    StringBuilder cur = new StringBuilder();
                    for (String word : words) {
                        String candidate = cur.length() == 0 ? word : cur + " " + word;
                        if (font.width(candidate) <= BOX_W) {
                            cur = new StringBuilder(candidate);
                        } else {
                            if (cur.length() > 0) wrapped.add(new Line(cur.toString(), line.color()));
                            cur = new StringBuilder("    " + word);
                        }
                    }
                    if (cur.length() > 0) wrapped.add(new Line(cur.toString(), line.color()));
                }
            }

            int totalH = wrapped.size() * LH + PAD * 2;
            int bx = resolveX(mc);
            int by = resolveY(mc);

            gui.fill(bx - PAD, by - PAD, bx + BOX_W + PAD, by + totalH,        0xAA000000);
            gui.fill(bx - PAD, by - PAD, bx + BOX_W + PAD, by - PAD + 1,       0xFF666666);
            gui.fill(bx - PAD, by + totalH - 1, bx + BOX_W + PAD, by + totalH, 0xFF666666);

            int cy = by;
            for (Line line : wrapped) {
                gui.drawString(font, line.text(), bx, cy, line.color(), false);
                cy += LH;
            }

            // Apply-preset button (visible only when pinned, report available, and not yet matching preset)
            if (showPinned && report != null && !presetApplied) {
                String label = I18n.get("smartoptimizer.overlay.btn.apply", report.getPreset().name());
                int btnX = bx - PAD;
                int btnY2 = by + totalH + 1;
                int btnFullW = BOX_W + PAD * 2;

                // Hover detection using GLFW cursor pos
                boolean hover = false;
                try {
                    double guiScale = mc.getWindow().getGuiScale();
                    double[] xArr = new double[1], yArr = new double[1];
                    GLFW.glfwGetCursorPos(mc.getWindow().getWindow(), xArr, yArr);
                    double curX = xArr[0] / guiScale;
                    double curY = yArr[0] / guiScale;
                    hover = mc.screen != null
                            && curX >= btnX && curX <= btnX + btnFullW
                            && curY >= btnY2 && curY <= btnY2 + BTN_H;
                } catch (Exception ignored) {}

                gui.fill(btnX, btnY2, btnX + btnFullW, btnY2 + BTN_H,
                        hover ? 0xBB004433 : 0x99002222);
                gui.fill(btnX, btnY2, btnX + btnFullW, btnY2 + 1, 0xFF336644);
                gui.fill(btnX, btnY2 + BTN_H - 1, btnX + btnFullW, btnY2 + BTN_H, 0xFF336644);
                gui.drawCenteredString(font, label, btnX + btnFullW / 2, btnY2 + 3,
                        hover ? 0xFF88FFAA : 0xFF55FF88);

                applyBtnX       = btnX;
                applyBtnY       = btnY2;
                applyBtnVisible = true;
            } else {
                applyBtnVisible = false;
            }

        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("OverlayRenderer render error: {}", e.getMessage());
        }
    }

    private static String particlesLabel(ParticleStatus p) {
        if (p == null) return "?";
        return switch (p) {
            case ALL       -> "All";
            case DECREASED -> "Decreased";
            case MINIMAL   -> "Minimal";
        };
    }

    private static String tr(String s) {
        if (!s.startsWith("smartoptimizer.report.")) return s;
        int col = s.indexOf(':');
        if (col > 0 && col < s.lastIndexOf('.') + 20) {
            String key = s.substring(0, col);
            if (key.startsWith("smartoptimizer.report.")) {
                try {
                    long arg = Long.parseLong(s.substring(col + 1));
                    return I18n.get(key, arg);
                } catch (NumberFormatException ignored) {}
            }
        }
        return I18n.get(s);
    }

    private static int[] presetExpected(com.smartoptimizer.presets.Preset preset) {
        return switch (preset) {
            case POTATO   -> new int[]{ 6,  5,  50 };
            case BALANCED -> new int[]{ 10, 8,  75 };
            case HIGH_END -> new int[]{ 16, 12, 100 };
        };
    }

    private static String presetExpectedParticles(com.smartoptimizer.presets.Preset preset) {
        return switch (preset) {
            case POTATO   -> "Minimal";
            case BALANCED -> "Decreased";
            case HIGH_END -> "All";
        };
    }

    private static int resolveX(Minecraft mc) {
        if (OverlayState.posX >= 0) return OverlayState.posX;
        return mc.getWindow().getGuiScaledWidth() - BOX_W - PAD - 7;
    }

    private static int resolveY(Minecraft mc) {
        if (OverlayState.posY >= 0) return OverlayState.posY;
        return mc.getWindow().getGuiScaledHeight() - 187;
    }
}
