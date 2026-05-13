package com.smartoptimizer.client;

import com.smartoptimizer.jvm.JvmArgWriter;
import com.smartoptimizer.jvm.JvmIssueState;
import com.smartoptimizer.jvm.JvmProfile;
import com.smartoptimizer.jvm.JvmSentinelManager;
import com.smartoptimizer.jvm.LaunchLoopDetector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

public class JvmGateScreen extends Screen {

    private static final int PW = 440;
    private static final int PH = 180;

    private final Screen parent;
    private final String fixArgs;
    private final boolean isLoop;

    private String feedback = "";
    private long   feedbackAt = 0;

    public JvmGateScreen(Screen parent) {
        super(Component.empty());
        this.parent  = parent;
        this.isLoop  = LaunchLoopDetector.isLoopDetected();
        // Prefer sentinel args; fall back to safe profile
        String sentinel = JvmSentinelManager.readArgs();
        this.fixArgs = sentinel != null ? sentinel : JvmProfile.SAFE.args;
    }

    @Override
    protected void init() {
        int cx    = this.width  / 2;
        int py    = this.height / 2 - PH / 2;
        int btnY  = py + PH - 32;

        // Apply Fix Automatically
        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.jvm.gate.apply")),
            btn -> {
                boolean ok = JvmArgWriter.applyArgs(fixArgs);
                JvmSentinelManager.delete();
                LaunchLoopDetector.reset(FMLPaths.GAMEDIR.get().resolve("config/smartoptimizer"));
                feedback   = ok ? I18n.get("smartoptimizer.jvm.alert.applied") : "Failed - check logs";
                feedbackAt = System.currentTimeMillis();
            }
        ).bounds(cx - 160, btnY, 155, 20).build());

        // Continue Anyway
        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.jvm.gate.continue")),
            btn -> { if (this.minecraft != null) this.minecraft.setScreen(parent); }
        ).bounds(cx + 5, btnY, 155, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int cx = this.width  / 2;
        int cy = this.height / 2;
        int px = cx - PW / 2;
        int py = cy - PH / 2;

        // Full-screen dim
        g.fill(0, 0, this.width, this.height, 0xCC000000);
        g.fill(px, py, px + PW, py + PH, 0xF0100010);
        drawBorder(g, px, py, PW, PH, 0xFFFF4444);

        // Title
        String title = isLoop
                ? I18n.get("smartoptimizer.jvm.gate.title.loop")
                : I18n.get("smartoptimizer.jvm.gate.title.crash");
        g.drawCenteredString(this.font, title, cx, py + 9, 0xFFFF4444);

        // Subtitle / detail
        String subtitle = isLoop
                ? I18n.get("smartoptimizer.jvm.gate.subtitle.loop", LaunchLoopDetector.getCrashCount())
                : I18n.get("smartoptimizer.jvm.gate.subtitle.crash");
        g.drawCenteredString(this.font, subtitle, cx, py + 25, 0xFFCCCCCC);

        // Body text
        g.drawCenteredString(this.font, I18n.get("smartoptimizer.jvm.gate.body"), cx, py + 42, 0xFFAAAAAA);

        // Args box label
        g.drawString(this.font, I18n.get("smartoptimizer.jvm.gate.fix_label"), px + 14, py + 60, 0xFF888888, false);

        int bw = PW - 28;
        g.fill(px + 14, py + 74, px + 14 + bw, py + 90, 0xCC000000);
        drawBorder(g, px + 14, py + 74, bw, 16, 0xFF334455);
        String disp = fixArgs;
        while (this.font.width(disp) > bw - 8 && disp.length() > 8)
            disp = disp.substring(0, disp.length() - 4) + "...";
        g.drawString(this.font, disp, px + 18, py + 78, 0xFF88FFAA, false);

        // "Works with any launcher" hint
        g.drawCenteredString(this.font,
                I18n.get("smartoptimizer.jvm.gate.hint"), cx, py + 95, 0xFF555588);

        // Feedback
        if (!feedback.isEmpty() && System.currentTimeMillis() - feedbackAt < 3000) {
            g.drawCenteredString(this.font, feedback, cx, py + PH - 52, 0xFF55FF55);
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
