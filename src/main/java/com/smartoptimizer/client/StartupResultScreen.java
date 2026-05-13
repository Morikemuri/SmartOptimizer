package com.smartoptimizer.client;

import com.smartoptimizer.startup.StartupProfiler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.List;

public class StartupResultScreen extends Screen {

    private static final int PW = 400;
    private static final int PH = 230;

    private final Screen parent;
    private final StartupProfiler.ProfileResult result;

    public StartupResultScreen(Screen parent, StartupProfiler.ProfileResult result) {
        super(Component.empty());
        this.parent = parent;
        this.result = result;
    }

    @Override
    protected void init() {
        int cx  = this.width  / 2;
        int py  = this.height / 2 - PH / 2;
        int btnY = py + PH - 28;

        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.startup.close")),
            btn -> close()
        ).bounds(cx - 50, btnY, 100, 20).build());
    }

    private void close() {
        StartupProfiler.markResultShown();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int cx = this.width  / 2;
        int cy = this.height / 2;
        int px = cx - PW / 2;
        int py = cy - PH / 2;

        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(px, py, px + PW, py + PH, 0xE0101020);
        drawBorder(g, px, py, PW, PH, 0xFF4466AA);

        // Title
        g.drawCenteredString(this.font, I18n.get("smartoptimizer.startup.title"), cx, py + 9, 0xFF88AAFF);

        // Total time
        long totalMs = result.totalMs();
        String timeStr;
        if (totalMs >= 60_000) {
            timeStr = I18n.get("smartoptimizer.startup.time.min",
                    totalMs / 60_000, (totalMs % 60_000) / 1000);
        } else {
            timeStr = I18n.get("smartoptimizer.startup.time.sec", totalMs / 1000);
        }
        g.drawCenteredString(this.font, timeStr, cx, py + 25, 0xFF55FF55);
        g.drawCenteredString(this.font,
                I18n.get("smartoptimizer.startup.mods", result.modCount()),
                cx, py + 37, 0xFF888888);

        List<StartupProfiler.ModTiming> slow = result.slowMods();
        if (slow.isEmpty()) {
            g.drawCenteredString(this.font,
                    I18n.get("smartoptimizer.startup.no_heavy"), cx, py + 60, 0xFF55FF55);
        } else {
            g.drawCenteredString(this.font,
                    I18n.get("smartoptimizer.startup.heaviest"), cx, py + 55, 0xFFAAAAAA);

            int lineY = py + 68;
            int max = Math.min(slow.size(), 7);
            long maxMs = slow.get(0).estimatedMs();
            for (int i = 0; i < max; i++) {
                StartupProfiler.ModTiming m = slow.get(i);
                // bar fill proportional to max
                int barW = (int)((PW - 100) * m.estimatedMs() / Math.max(1, maxMs));
                g.fill(px + 90, lineY, px + 90 + barW, lineY + 9, 0x66335577);

                String label = m.displayName();
                String time  = String.format("~%.1fs", m.estimatedMs() / 1000.0);
                int warnColor = m.estimatedMs() > 5000 ? 0xFFFF6644 : m.estimatedMs() > 2000 ? 0xFFFFAA00 : 0xFF88AAFF;
                g.drawString(this.font, label, px + 12, lineY, warnColor, false);
                g.drawString(this.font, time, px + PW - 50, lineY, 0xFF888888, false);
                lineY += 13;
            }
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int col) {
        g.fill(x,         y,         x + w,     y + 1,     col);
        g.fill(x,         y + h - 1, x + w,     y + h,     col);
        g.fill(x,         y,         x + 1,     y + h,     col);
        g.fill(x + w - 1, y,         x + w,     y + h,     col);
    }
}
