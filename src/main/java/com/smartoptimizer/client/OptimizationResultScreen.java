package com.smartoptimizer.client;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.detection.HardwareDetector;
import com.smartoptimizer.detection.HardwareInfo;
import com.smartoptimizer.detection.ModDetector;
import com.smartoptimizer.jvm.JvmArgWriter;
import com.smartoptimizer.jvm.JvmIssueState;
import com.smartoptimizer.presets.Preset;
import com.smartoptimizer.presets.PresetSelector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OptimizationResultScreen extends Screen {

    private static final int PW = 420;
    private static final int PH = 280;

    private enum State { PREVIEW, RESULT }

    private final Screen parent;
    private State state = State.PREVIEW;
    private OneClickOptimizer.Result result = null;

    // Preview data built once on first init
    private Preset           previewPreset = Preset.BALANCED;
    private final List<Entry> previewEntries = new ArrayList<>();
    private boolean          previewBuilt   = false;

    // colour-tagged line for rendering
    private record Entry(String text, int color) {}

    public OptimizationResultScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    // ── init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        if (!previewBuilt) { buildPreview(); previewBuilt = true; }

        int cx   = this.width  / 2;
        int py   = this.height / 2 - PH / 2;
        int btnY = py + PH - 28;

        if (state == State.PREVIEW) {
            // "Fix" — actually applies; "Done" — go back without doing anything
            this.addRenderableWidget(Button.builder(
                Component.literal(I18n.get("smartoptimizer.optimize.btn.fix")),
                btn -> { result = OneClickOptimizer.run(); state = State.RESULT; rebuildWidgets(); }
            ).bounds(cx - 105, btnY, 100, 20).build());

            this.addRenderableWidget(Button.builder(
                Component.literal(I18n.get("smartoptimizer.optimize.btn.done")),
                btn -> { if (this.minecraft != null) this.minecraft.setScreen(parent); }
            ).bounds(cx + 5, btnY, 100, 20).build());
        } else {
            this.addRenderableWidget(Button.builder(
                Component.literal(I18n.get("smartoptimizer.optimize.btn.done")),
                btn -> { if (this.minecraft != null) this.minecraft.setScreen(parent); }
            ).bounds(cx - 50, btnY, 100, 20).build());
        }
    }

    // ── build preview (no side-effects — only reads hardware/mods) ───────────

    private void buildPreview() {
        Map<String, Boolean> mods = Map.of();
        try {
            HardwareInfo hw = HardwareDetector.detect();
            mods = ModDetector.detect();
            previewPreset = PresetSelector.select(hw, mods);
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[OptPreview] {}", e.getMessage());
        }

        // Read current options
        int curRend = -1, curSim = -1, curEnt = -1;
        ParticleStatus curPart = null;
        try {
            Minecraft mc = Minecraft.getInstance();
            curRend = mc.options.renderDistance().get();
            curSim  = mc.options.simulationDistance().get();
            curPart = mc.options.particles().get();
            curEnt  = (int) Math.round(mc.options.entityDistanceScaling().get() * 100);
        } catch (Exception ignored) {}

        int[] tgt       = presetTargetInts(previewPreset);
        ParticleStatus tgtPart = presetTargetParticles(previewPreset);

        // ── Section 1: backup ─────────────────────────────────────────────
        previewEntries.add(new Entry("[+] " + I18n.get("smartoptimizer.optimize.preview.backup"), 0xFF55FF55));

        // ── Section 2: only show graphic settings that differ from target ──
        boolean rendOk = curRend == tgt[0];
        boolean simOk  = curSim  == tgt[1];
        boolean partOk = curPart == tgtPart;
        boolean entOk  = curEnt  == tgt[2];
        if (!rendOk || !simOk || !partOk || !entOk) {
            previewEntries.add(new Entry("[+] " + I18n.get("smartoptimizer.optimize.preview.preset",
                    previewPreset.name()), 0xFF55FF55));
            if (!rendOk) previewEntries.add(new Entry(
                    "    " + I18n.get("smartoptimizer.optimize.preview.render", tgt[0]), 0xFF888888));
            if (!simOk)  previewEntries.add(new Entry(
                    "    " + I18n.get("smartoptimizer.optimize.preview.sim", tgt[1]), 0xFF888888));
            if (!partOk) previewEntries.add(new Entry(
                    "    " + I18n.get("smartoptimizer.optimize.preview.particles",
                            I18n.get("smartoptimizer.optimize.preview.particles." + tgtPart.name().toLowerCase())),
                    0xFF888888));
            if (!entOk)  previewEntries.add(new Entry(
                    "    " + I18n.get("smartoptimizer.optimize.preview.entity", tgt[2]), 0xFF888888));
        }

        // ── Section 3: tunable mods ───────────────────────────────────────
        List<String> tunableMods = tunableMods(mods);
        if (!tunableMods.isEmpty()) {
            previewEntries.add(new Entry("[+] " + I18n.get("smartoptimizer.optimize.preview.mods",
                    String.join(", ", tunableMods)), 0xFF55FF55));
        }

        // ── Section 4: JVM — only if fix not already in user_jvm_args.txt ─
        try {
            boolean hasJvmIssue = JvmIssueState.hasCrash() || !JvmIssueState.getRecommendations().isEmpty();
            if (hasJvmIssue) {
                String fixArgs = JvmIssueState.hasCrash()
                        ? JvmIssueState.getDetectedCrash().fixArgs()
                        : JvmIssueState.getRecommendations().stream()
                                .map(r -> r.args()).reduce((a, b) -> a + " " + b).orElse("");
                if (!fixArgs.isEmpty() && !JvmArgWriter.isAlreadyApplied(fixArgs)) {
                    previewEntries.add(new Entry(
                            "[!] " + I18n.get("smartoptimizer.optimize.preview.jvm"), 0xFFFFAA00));
                }
            }
        } catch (Exception ignored) {}
    }

    private static int[] presetTargetInts(Preset p) {
        return switch (p) {
            case POTATO   -> new int[]{ 6,  5,  50  };
            case BALANCED -> new int[]{ 10, 8,  75  };
            case HIGH_END -> new int[]{ 16, 12, 100 };
        };
    }

    private static ParticleStatus presetTargetParticles(Preset p) {
        return switch (p) {
            case POTATO   -> ParticleStatus.MINIMAL;
            case BALANCED -> ParticleStatus.DECREASED;
            case HIGH_END -> ParticleStatus.ALL;
        };
    }

    private static List<String> tunableMods(Map<String, Boolean> mods) {
        List<String> list = new ArrayList<>();
        if (Boolean.TRUE.equals(mods.get("ferritecore")))                         list.add("FerriteCore");
        if (Boolean.TRUE.equals(mods.get("entityculling")))                       list.add("EntityCulling");
        if (Boolean.TRUE.equals(mods.get("embeddium")) ||
            Boolean.TRUE.equals(mods.get("rubidium")))                            list.add("Rubidium/Embeddium");
        if (Boolean.TRUE.equals(mods.get("modernfix")))                           list.add("ModernFix");
        return list;
    }

    // ── render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int cx = this.width  / 2;
        int cy = this.height / 2;
        int px = cx - PW / 2;
        int py = cy - PH / 2;

        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(px, py, px + PW, py + PH, 0xF0101820);
        drawBorder(g, px, py, PW, PH, 0xFF44AA66);

        if (state == State.PREVIEW) renderPreview(g, cx, px, py);
        else                        renderResult (g, cx, px, py);

        super.render(g, mx, my, pt);
    }

    private void renderPreview(GuiGraphics g, int cx, int px, int py) {
        g.drawCenteredString(this.font,
                I18n.get("smartoptimizer.optimize.preview.title"), cx, py + 9, 0xFF55FF88);

        int lineY = py + 26;
        for (Entry e : previewEntries) {
            if (lineY > py + PH - 38) break;
            g.drawString(this.font, e.text(), px + 14, lineY, e.color(), false);
            lineY += 11;
        }
    }

    private void renderResult(GuiGraphics g, int cx, int px, int py) {
        g.drawCenteredString(this.font,
                I18n.get("smartoptimizer.optimize.done"), cx, py + 9, 0xFF55FF88);
        g.drawCenteredString(this.font,
                I18n.get("smartoptimizer.optimize.preset", result.preset().name()),
                cx, py + 22, 0xFF888888);

        int lineY = py + 38;
        for (String line : result.log()) {
            if (lineY > py + PH - 38) break;
            int col = line.startsWith("[+]") ? 0xFF55FF55
                    : line.startsWith("[!]") ? 0xFFFFAA00
                    : 0xFF888888;
            g.drawString(this.font, line, px + 14, lineY, col, false);
            lineY += 12;
        }

        if (result.jvmWritten()) {
            g.drawCenteredString(this.font,
                    I18n.get("smartoptimizer.optimize.restart_hint"),
                    cx, py + PH - 44, 0xFFFFCC44);
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int col) {
        g.fill(x,         y,         x + w,     y + 1,     col);
        g.fill(x,         y + h - 1, x + w,     y + h,     col);
        g.fill(x,         y,         x + 1,     y + h,     col);
        g.fill(x + w - 1, y,         x + w,     y + h,     col);
    }
}
