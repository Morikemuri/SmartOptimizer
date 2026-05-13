package com.smartoptimizer.client;

import com.smartoptimizer.config.ConfigManager;
import com.smartoptimizer.core.FirstLaunchController;
import com.smartoptimizer.detection.ModDetector;
import com.smartoptimizer.jvm.JvmArgWriter;
import com.smartoptimizer.jvm.JvmIssueState;
import com.smartoptimizer.jvm.JvmProfile;
import com.smartoptimizer.presets.Preset;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class FirstLaunchWizardScreen extends Screen {

    private static final int PW = 460;
    private static final int PH = 230;

    private final Screen parent;

    private static final String[][] MODES = {
        { "smartoptimizer.wizard.mode.fps",      "smartoptimizer.wizard.mode.fps.desc",      "POTATO"    },
        { "smartoptimizer.wizard.mode.balanced",  "smartoptimizer.wizard.mode.balanced.desc", "BALANCED"  },
        { "smartoptimizer.wizard.mode.graphics",  "smartoptimizer.wizard.mode.graphics.desc", "HIGH_END"  },
        { "smartoptimizer.wizard.mode.laptop",    "smartoptimizer.wizard.mode.laptop.desc",   "POTATO"    },
    };

    private int selected = 1; // Balanced by default

    public FirstLaunchWizardScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width  / 2;
        int py = this.height / 2 - PH / 2;

        // Mode selection buttons (2×2 grid)
        int bw = 200, bh = 26;
        int gx = cx - bw - 5;
        int gy = py + 50;

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            int bx = (i % 2 == 0) ? gx : cx + 5;
            int by = gy + (i / 2) * (bh + 8);
            this.addRenderableWidget(Button.builder(
                Component.literal(I18n.get(MODES[i][0])),
                btn -> selected = idx
            ).bounds(bx, by, bw, bh).build());
        }

        // Apply + Skip
        int btnY = py + PH - 30;
        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.wizard.apply")),
            btn -> applyAndClose()
        ).bounds(cx - 105, btnY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.wizard.skip")),
            btn -> close()
        ).bounds(cx + 5, btnY, 100, 20).build());
    }

    private void applyAndClose() {
        try {
            Preset preset = switch (MODES[selected][2]) {
                case "HIGH_END" -> Preset.HIGH_END;
                case "POTATO"   -> Preset.POTATO;
                default         -> Preset.BALANCED;
            };
            Map<String, Boolean> mods = ModDetector.detect();
            ConfigManager.apply(preset, mods);

            // Apply JVM profile (balanced or performance based on mode)
            if (selected == 2) { // Maximum Graphics = Performance JVM
                JvmArgWriter.applyArgs(JvmProfile.PERFORMANCE.args);
            } else if (selected == 0 || selected == 3) { // Max FPS / Laptop = Balanced JVM
                JvmArgWriter.applyArgs(JvmProfile.BALANCED.args);
            }

            new FirstLaunchController().markApplied();
            JvmIssueState.setWizardPending(false);
        } catch (Exception ignored) {}
        close();
    }

    private void close() {
        JvmIssueState.setWizardPending(false);
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int cx = this.width  / 2;
        int cy = this.height / 2;
        int px = cx - PW / 2;
        int py = cy - PH / 2;

        g.fill(0, 0, this.width, this.height, 0xBB000000);
        g.fill(px, py, px + PW, py + PH, 0xF0101820);
        drawBorder(g, px, py, PW, PH, 0xFF4466AA);

        g.drawCenteredString(this.font, I18n.get("smartoptimizer.wizard.title"), cx, py + 10, 0xFF88AAFF);
        g.drawCenteredString(this.font, I18n.get("smartoptimizer.wizard.subtitle"), cx, py + 24, 0xFF888888);

        // Description of selected mode
        String descKey = MODES[selected][1];
        g.drawCenteredString(this.font, I18n.get(descKey), cx, py + PH - 55, 0xFFCCCCCC);

        // Highlight selected mode
        int bw = 200, bh = 26;
        int gx = cx - bw - 5;
        int gy = py + 50;
        for (int i = 0; i < 4; i++) {
            if (i == selected) {
                int bx = (i % 2 == 0) ? gx : cx + 5;
                int by = gy + (i / 2) * (bh + 8);
                drawBorder(g, bx - 1, by - 1, bw + 2, bh + 2, 0xFF55AAFF);
            }
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int col) {
        g.fill(x,         y,         x + w,     y + 1,     col);
        g.fill(x,         y + h - 1, x + w,     y + h,     col);
        g.fill(x,         y,         x + 1,     y + h,     col);
        g.fill(x + w - 1, y,         x + w,     y + h,     col);
    }
}
