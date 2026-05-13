package com.smartoptimizer.client;

import com.smartoptimizer.config.ConfigBackupSystem;
import com.smartoptimizer.config.ConfigPatchEngine;
import com.smartoptimizer.detection.HardwareDetector;
import com.smartoptimizer.detection.ModDetector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModpackDoctorScreen extends Screen {

    private static final int PW = 480;
    private static final int PH = 290;

    private final Screen parent;

    private static final Set<String> DEPRECATED_MODS = Set.of(
            "smoothboot", "lazydfu", "performant", "optifine");
    private static final Set<String> PERF_MODS = Set.of(
            "ferritecore", "embeddium", "rubidium", "entityculling",
            "modernfix", "immediatelyfast", "starlight", "memoryleakfix");
    private static final Set<String> RISKY_MODS = Set.of("optifine");

    private record Issue(String text, int color) {}

    private enum State { BROWSE, CONFIRM, DONE }

    private final List<Issue>  issues    = new ArrayList<>();
    private final List<String> ok        = new ArrayList<>();
    private final int          healthPct;
    private final int          totalMods;
    private int                scrollY   = 0;
    private State              state     = State.BROWSE;
    private int                appliedTweaks = 0;

    public ModpackDoctorScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;

        Set<String> modIds = ModList.get().getMods().stream()
                .map(m -> m.getModId().toLowerCase())
                .collect(Collectors.toSet());
        totalMods = modIds.size();

        for (String mod : RISKY_MODS) {
            if (modIds.contains(mod))
                issues.add(new Issue("[X] " + mod + ": " + I18n.get("smartoptimizer.doctor.hint.incompatible"), 0xFFFF4444));
        }
        for (String mod : DEPRECATED_MODS) {
            if (modIds.contains(mod))
                issues.add(new Issue("[!] " + mod + ": " + I18n.get("smartoptimizer.doctor.hint.deprecated"), 0xFFFFAA00));
        }
        if (totalMods > 30) {
            boolean hasGpu = modIds.contains("embeddium") || modIds.contains("rubidium");
            if (!hasGpu) issues.add(new Issue("[!] " + I18n.get("smartoptimizer.doctor.hint.no_gpu"), 0xFFFFAA00));
            if (!modIds.contains("ferritecore")) issues.add(new Issue("[!] " + I18n.get("smartoptimizer.doctor.hint.no_ferritecore"), 0xFFFFCC44));
        }
        for (String mod : PERF_MODS) {
            if (modIds.contains(mod)) ok.add("[+] " + mod);
        }

        long critical = issues.stream().filter(i -> i.color() == 0xFFFF4444).count();
        long warnings = issues.stream().filter(i -> i.color() != 0xFFFF4444).count();
        healthPct = Math.max(0, 100 - (int)(critical * 25 + warnings * 8));
    }

    @Override
    protected void init() {
        int cx   = this.width  / 2;
        int py   = this.height / 2 - PH / 2;
        int btnY = py + PH - 28;

        if (state == State.BROWSE) {
            this.addRenderableWidget(Button.builder(
                Component.literal(I18n.get("smartoptimizer.doctor.fix_all")),
                btn -> { state = State.CONFIRM; rebuildWidgets(); }
            ).bounds(cx - 165, btnY, 155, 20).build());

            this.addRenderableWidget(Button.builder(
                Component.literal(I18n.get("smartoptimizer.doctor.close")),
                btn -> { if (this.minecraft != null) this.minecraft.setScreen(parent); }
            ).bounds(cx - 5, btnY, 100, 20).build());

            int backups = ConfigBackupSystem.backupCount();
            if (backups > 0) {
                this.addRenderableWidget(Button.builder(
                    Component.literal(I18n.get("smartoptimizer.doctor.backups", backups)),
                    btn -> { if (this.minecraft != null) this.minecraft.setScreen(new BackupBrowseScreen(this)); }
                ).bounds(cx + 100, btnY, 100, 20).build());
            }

        } else if (state == State.CONFIRM) {
            this.addRenderableWidget(Button.builder(
                Component.literal(I18n.get("smartoptimizer.doctor.confirm.apply")),
                btn -> { applyFixes(); state = State.DONE; rebuildWidgets(); }
            ).bounds(cx - 115, btnY, 165, 20).build());

            this.addRenderableWidget(Button.builder(
                Component.literal(I18n.get("smartoptimizer.doctor.confirm.cancel")),
                btn -> { state = State.BROWSE; rebuildWidgets(); }
            ).bounds(cx + 55, btnY, 80, 20).build());

        } else { // DONE
            this.addRenderableWidget(Button.builder(
                Component.literal(I18n.get("smartoptimizer.doctor.close")),
                btn -> { if (this.minecraft != null) this.minecraft.setScreen(parent); }
            ).bounds(cx - 50, btnY, 100, 20).build());
        }
    }

    private void applyFixes() {
        try {
            ConfigBackupSystem.backup();
            var hw      = HardwareDetector.detect();
            var mods    = ModDetector.detect();
            var patches = ConfigPatchEngine.patch(hw, mods);
            appliedTweaks = patches.stream().mapToInt(p -> p.applied().size()).sum();
        } catch (Exception e) {
            appliedTweaks = 0;
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int cx = this.width  / 2;
        int cy = this.height / 2;
        int px = cx - PW / 2;
        int py = cy - PH / 2;

        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(px, py, px + PW, py + PH, 0xE0101820);
        drawBorder(g, px, py, PW, PH, 0xFF4466AA);

        g.drawCenteredString(this.font, I18n.get("smartoptimizer.doctor.title"), cx, py + 9, 0xFF88AAFF);

        int healthColor = healthPct >= 80 ? 0xFF55FF55 : healthPct >= 50 ? 0xFFFFAA00 : 0xFFFF4444;
        g.drawCenteredString(this.font, I18n.get("smartoptimizer.doctor.mods", totalMods), cx, py + 22, 0xFF888888);
        g.drawCenteredString(this.font, I18n.get("smartoptimizer.doctor.health", healthPct), cx, py + 34, healthColor);

        if (state == State.BROWSE) {
            renderBrowse(g, px, py, cx);
        } else if (state == State.CONFIRM) {
            renderConfirm(g, px, py);
        } else {
            renderDone(g, px, py);
        }

        super.render(g, mx, my, pt);
    }

    private void renderBrowse(GuiGraphics g, int px, int py, int cx) {
        int lx      = px + 12;
        int legendY = py + 47;

        // Color legend
        String redLabel = "[X] " + I18n.get("smartoptimizer.doctor.legend.red");
        g.drawString(this.font, "[X]", lx, legendY, 0xFFFF4444, false);
        g.drawString(this.font, " " + I18n.get("smartoptimizer.doctor.legend.red"),
                lx + this.font.width("[X]"), legendY, 0xFF888888, false);
        int offOrg = lx + this.font.width(redLabel);
        String orgLabel = "[!] " + I18n.get("smartoptimizer.doctor.legend.orange");
        g.drawString(this.font, "[!]", offOrg + 6, legendY, 0xFFFFAA00, false);
        g.drawString(this.font, " " + I18n.get("smartoptimizer.doctor.legend.orange"),
                offOrg + 6 + this.font.width("[!]"), legendY, 0xFF888888, false);
        int offGrn = offOrg + 6 + this.font.width(orgLabel);
        g.drawString(this.font, "[+]", offGrn + 6, legendY, 0xFF55AA55, false);
        g.drawString(this.font, " " + I18n.get("smartoptimizer.doctor.legend.green"),
                offGrn + 6 + this.font.width("[+]"), legendY, 0xFF888888, false);

        // Separator
        g.fill(px + 8, legendY + 11, px + PW - 8, legendY + 12, 0xFF223355);

        // Scrollable issue/ok list
        int listTop = legendY + 15;
        int lineY   = listTop + scrollY;
        int maxY    = py + PH - 36;

        for (Issue issue : issues) {
            if (lineY >= listTop && lineY < maxY)
                g.drawString(this.font, issue.text(), px + 12, lineY, issue.color(), false);
            lineY += 11;
        }
        if (!issues.isEmpty() && !ok.isEmpty()) {
            if (lineY >= listTop && lineY < maxY)
                g.drawString(this.font, "------------------", px + 12, lineY, 0xFF333355, false);
            lineY += 11;
        }
        for (String s : ok) {
            if (lineY >= listTop && lineY < maxY)
                g.drawString(this.font, s, px + 12, lineY, 0xFF55AA55, false);
            lineY += 11;
        }
        if (issues.isEmpty() && ok.isEmpty()) {
            g.drawCenteredString(this.font, I18n.get("smartoptimizer.doctor.ok"), cx, listTop + 20, 0xFF55FF55);
        }
    }

    private void renderConfirm(GuiGraphics g, int px, int py) {
        int y = py + 47;

        g.drawString(this.font, I18n.get("smartoptimizer.doctor.confirm.info"),
                px + 12, y, 0xFF88CCFF, false);
        y += 13;
        g.drawString(this.font, "[!] " + I18n.get("smartoptimizer.doctor.confirm.restart"),
                px + 12, y, 0xFFFFCC44, false);
        y += 16;

        if (!issues.isEmpty()) {
            g.fill(px + 8, y, px + PW - 8, y + 1, 0xFF223355);
            y += 8;
            g.drawString(this.font, I18n.get("smartoptimizer.doctor.confirm.manual_title"),
                    px + 12, y, 0xFFAAAAAA, false);
            y += 13;
            for (Issue issue : issues) {
                if (y < py + PH - 36)
                    g.drawString(this.font, issue.text(), px + 12, y, issue.color(), false);
                y += 11;
            }
        } else {
            y += 6;
            g.drawString(this.font, I18n.get("smartoptimizer.doctor.ok"),
                    px + 12, y, 0xFF55FF55, false);
        }
    }

    private void renderDone(GuiGraphics g, int px, int py) {
        int y = py + 47;

        g.drawString(this.font, I18n.get("smartoptimizer.doctor.done.applied", appliedTweaks),
                px + 12, y, 0xFF55FF55, false);
        y += 13;
        g.drawString(this.font, "[!] " + I18n.get("smartoptimizer.doctor.done.restart"),
                px + 12, y, 0xFFFFCC44, false);
        y += 16;

        if (!issues.isEmpty()) {
            g.fill(px + 8, y, px + PW - 8, y + 1, 0xFF223355);
            y += 8;
            g.drawString(this.font, I18n.get("smartoptimizer.doctor.done.manual_title"),
                    px + 12, y, 0xFFAAAAAA, false);
            y += 13;
            for (Issue issue : issues) {
                if (y < py + PH - 36)
                    g.drawString(this.font, issue.text(), px + 12, y, issue.color(), false);
                y += 11;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (state == State.BROWSE)
            scrollY = Math.min(0, scrollY + (int)(delta * 8));
        return true;
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
