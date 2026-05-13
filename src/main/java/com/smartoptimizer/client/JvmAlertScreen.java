package com.smartoptimizer.client;

import com.smartoptimizer.jvm.JvmArgWriter;
import com.smartoptimizer.jvm.JvmArgsValidator;
import com.smartoptimizer.jvm.JvmBugEntry;
import com.smartoptimizer.jvm.JvmIssueState;
import com.smartoptimizer.jvm.JvmProfile;
import com.smartoptimizer.jvm.JvmRecommendationEngine;
import com.smartoptimizer.jvm.JvmSentinelManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class JvmAlertScreen extends Screen {

    private static final int PW = 420;
    private static final int PH = 280;

    // Safe JVM Mode preset args (guaranteed to launch on any hardware)
    private static final String SAFE_MODE_ARGS = JvmProfile.SAFE.args;

    private final boolean isCrash;
    private final String  description;
    private final String  fixArgs;

    // Validation issues (shown as extra warnings)
    private final List<JvmArgsValidator.ValidationIssue> validIssues;

    private boolean applied   = false;
    private boolean copied    = false;
    private long    feedbackAt = 0;
    private String  feedbackMsg = "";

    public JvmAlertScreen() {
        super(Component.empty());
        JvmBugEntry crash = JvmIssueState.getDetectedCrash();
        List<JvmRecommendationEngine.Recommendation> recs = JvmIssueState.getRecommendations();
        validIssues = JvmIssueState.getValidationIssues();

        if (crash != null) {
            isCrash     = true;
            description = crash.description();
            fixArgs     = crash.fixArgs();
        } else if (!recs.isEmpty()) {
            isCrash     = false;
            description = recs.get(0).description();
            fixArgs     = recs.stream()
                    .map(JvmRecommendationEngine.Recommendation::args)
                    .reduce((a, b) -> a + " " + b).orElse("");
        } else if (!validIssues.isEmpty()) {
            isCrash     = validIssues.stream().anyMatch(JvmArgsValidator.ValidationIssue::critical);
            description = validIssues.get(0).message();
            fixArgs     = JvmProfile.BALANCED.args;
        } else {
            isCrash     = false;
            description = "";
            fixArgs     = "";
        }
    }

    @Override
    protected void init() {
        int cx   = this.width  / 2;
        int py   = this.height / 2 - PH / 2;
        int row1 = py + PH - 82;
        int row2 = py + PH - 58;

        // Row 1: Apply Fix | Safe JVM Mode
        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.jvm.alert.apply")),
            btn -> applyFix(fixArgs, false)
        ).bounds(cx - 210, row1, 130, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.jvm.alert.safe_mode")),
            btn -> applyFix(SAFE_MODE_ARGS, true)
        ).bounds(cx - 65, row1, 130, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.jvm.alert.copy")),
            btn -> {
                if (this.minecraft != null && !fixArgs.isEmpty()) {
                    this.minecraft.keyboardHandler.setClipboard(fixArgs);
                    showFeedback(I18n.get("smartoptimizer.jvm.alert.copied"));
                }
            }
        ).bounds(cx + 80, row1, 130, 20).build());

        // Row 2: Guide | Dismiss
        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.jvm.alert.guide")),
            btn -> { if (this.minecraft != null) this.minecraft.setScreen(new LauncherGuideScreen(this, fixArgs)); }
        ).bounds(cx - 105, row2, 100, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.jvm.alert.dismiss")),
            btn -> this.onClose()
        ).bounds(cx + 5, row2, 100, 20).build());
    }

    private void applyFix(String args, boolean safeMode) {
        if (args.isEmpty()) return;
        boolean ok = JvmArgWriter.applyArgs(args);
        if (ok) {
            if (safeMode) JvmSentinelManager.delete();
            // Rename crash file so it won't trigger the alert again on next launch
            java.nio.file.Path crashFile = JvmIssueState.getCrashFilePath();
            if (crashFile != null && java.nio.file.Files.exists(crashFile)) {
                try {
                    java.nio.file.Files.move(
                        crashFile,
                        crashFile.resolveSibling(crashFile.getFileName() + ".fixed"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (Exception ignored) {}
            }
        }
        showFeedback(ok
                ? I18n.get("smartoptimizer.jvm.alert.applied")
                : "Failed - check logs");
        applied = true;
    }

    private void showFeedback(String msg) {
        feedbackMsg = msg;
        feedbackAt  = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int cx = this.width  / 2;
        int cy = this.height / 2;
        int px = cx - PW / 2;
        int py = cy - PH / 2;

        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(px, py, px + PW, py + PH, 0xE0101020);
        int borderColor = isCrash ? 0xFFFF4444 : 0xFFFFAA00;
        drawBorder(g, px, py, PW, PH, borderColor);

        // Title
        String titleKey = isCrash ? "smartoptimizer.jvm.alert.title.crash" : "smartoptimizer.jvm.alert.title.info";
        g.drawCenteredString(this.font, I18n.get(titleKey), cx, py + 8, borderColor);

        // Description (word-wrapped)
        int descMaxW = PW - 24;
        int descY    = py + 22;
        if (!description.isEmpty()) {
            for (FormattedCharSequence line : this.font.split(Component.literal(description), descMaxW)) {
                g.drawString(this.font, line, cx - descMaxW / 2, descY, 0xFFCCCCCC, false);
                descY += 10;
            }
        }

        // Validation warnings (if any, briefly)
        if (!validIssues.isEmpty() && !isCrash) {
            for (JvmArgsValidator.ValidationIssue issue : validIssues) {
                int col = issue.critical() ? 0xFFFF6644 : 0xFFFFCC44;
                g.drawString(this.font, "⚠ " + issue.message(), px + 12, descY, col, false);
                descY += 10;
            }
        }

        // Fix label + args box
        int boxTop = descY + 4;
        g.drawString(this.font, I18n.get("smartoptimizer.jvm.alert.fix_label"), px + 12, boxTop, 0xFF888888, false);
        boxTop += 12;

        int boxW = PW - 24;
        g.fill(px + 12, boxTop, px + 12 + boxW, boxTop + 16, 0xCC000000);
        drawBorder(g, px + 12, boxTop, boxW, 16, 0xFF334455);
        String display = fixArgs;
        while (this.font.width(display) > boxW - 8 && display.length() > 8)
            display = display.substring(0, display.length() - 4) + "...";
        g.drawString(this.font, display, px + 16, boxTop + 4, 0xFF88FFAA, false);

        // Feedback message (applied / copied)
        if (!feedbackMsg.isEmpty() && System.currentTimeMillis() - feedbackAt < 3000) {
            g.drawCenteredString(this.font, feedbackMsg, cx, py + PH - 108, 0xFF55FF55);
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
