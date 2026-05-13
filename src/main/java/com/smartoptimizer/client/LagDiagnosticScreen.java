package com.smartoptimizer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class LagDiagnosticScreen extends Screen {

    private static final int PW = 440;
    private static final int PH = 220;

    private final Screen parent;
    private LagDiagnosticEngine.Report report;

    public LagDiagnosticScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        report = LagDiagnosticEngine.analyze();

        int cx   = this.width  / 2;
        int py   = this.height / 2 - PH / 2;
        int btnY = py + PH - 28;

        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.lag.rescan")),
            btn -> {
                report = LagDiagnosticEngine.analyze();
                clearWidgets();
                init();
            }
        ).bounds(cx - 105, btnY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.lag.close")),
            btn -> { if (this.minecraft != null) this.minecraft.setScreen(parent); }
        ).bounds(cx + 5, btnY, 100, 20).build());
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

        g.drawCenteredString(this.font, I18n.get("smartoptimizer.lag.title"), cx, py + 9, 0xFF88AAFF);

        if (report == null) {
            g.drawCenteredString(this.font, "Scanning...", cx, cy, 0xFF888888);
            super.render(g, mx, my, pt);
            return;
        }

        int lineY = py + 26;

        // Primary cause
        LagDiagnosticEngine.Cause primary = report.primary();
        int primaryColor = severityColor(primary.severity());
        drawBorder(g, px + 10, lineY - 2, PW - 20, 34, primaryColor);
        g.fill(px + 11, lineY - 1, px + PW - 11, lineY + 31, 0x44000000);

        List<FormattedCharSequence> descLines = this.font.split(
                Component.literal("⚠ " + primary.description()), PW - 40);
        for (var line : descLines) {
            g.drawString(this.font, line, px + 16, lineY + 2, primaryColor, false);
            lineY += 10;
        }
        g.drawString(this.font, "→ " + primary.fix(), px + 16, lineY + 4, 0xFF55FFAA, false);
        lineY += 22;

        // Secondary causes
        if (!report.secondary().isEmpty()) {
            g.drawString(this.font, I18n.get("smartoptimizer.lag.also"), px + 12, lineY, 0xFF666666, false);
            lineY += 11;
            for (LagDiagnosticEngine.Cause c : report.secondary()) {
                if (lineY > py + PH - 50) break;
                int col = severityColor(c.severity());
                g.drawString(this.font, "• " + c.description(), px + 14, lineY, col, false);
                lineY += 10;
                g.drawString(this.font, "  Fix: " + c.fix(), px + 14, lineY, 0xFF55FFAA, false);
                lineY += 12;
            }
        }

        super.render(g, mx, my, pt);
    }

    private int severityColor(int severity) {
        return switch (severity) {
            case 3  -> 0xFFFF4444;
            case 2  -> 0xFFFFAA00;
            case 1  -> 0xFFFFCC44;
            default -> 0xFF55FF55;
        };
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
