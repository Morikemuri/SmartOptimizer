package com.smartoptimizer.client;

import com.smartoptimizer.startup.StartupProfiler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class TitleScreenHandler {

    private static boolean toastTriggered  = false;
    private static long    toastShowUntil  = 0;

    private static final int  TOAST_W   = 220;
    private static final int  TOAST_PAD = 15;
    private static final long TOAST_MS  = 5000;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen) {
            StartupProfiler.onTitleScreenReady();
            return;
        }

        if (!(event.getScreen() instanceof OptionsScreen screen)) return;

        var mc = Minecraft.getInstance();
        int bw = 150, bh = 20;

        // Find the bottommost widget (Done button) at runtime — mods may add
        // extra rows that shift hardcoded offsets into existing buttons.
        int doneY = screen.height - 27;
        for (var child : screen.children()) {
            if (child instanceof AbstractWidget w && w.getY() > 0 && w.getY() < screen.height - 2) {
                if (w.getY() > doneY) doneY = w.getY();
            }
        }
        int by = doneY - 24;
        int bx = screen.width  / 2 - bw - 5;

        event.addListener(Button.builder(
                Component.literal(I18n.get("smartoptimizer.title.optimize")),
                btn -> mc.setScreen(new OptimizationResultScreen(screen))
        ).bounds(bx, by, bw, bh).build());

        event.addListener(Button.builder(
                Component.literal(I18n.get("smartoptimizer.title.doctor")),
                btn -> mc.setScreen(new ModpackDoctorScreen(screen))
        ).bounds(bx + bw + 10, by, bw, bh).build());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        // Arm toast once when profiler has a result
        if (!toastTriggered && StartupProfiler.shouldShowResult()) {
            toastTriggered = true;
            toastShowUntil = System.currentTimeMillis() + TOAST_MS;
            StartupProfiler.markResultShown();
        }

        if (toastShowUntil > 0 && System.currentTimeMillis() < toastShowUntil) {
            renderToast(event.getGuiGraphics(),
                    event.getScreen().width, event.getScreen().height,
                    Minecraft.getInstance().font);
        }
    }

    private static void renderToast(GuiGraphics g, int sw, int sh,
                                    net.minecraft.client.gui.Font font) {
        StartupProfiler.ProfileResult result = StartupProfiler.getLastResult();
        if (result == null) return;

        List<StartupProfiler.ModTiming> slow = result.slowMods();
        int modLines = Math.min(slow.size(), 3);
        int th = 6 + (2 + modLines) * 11 + 5;

        int tx = sw - TOAST_W - TOAST_PAD;
        int ty = sh - th          - TOAST_PAD;

        g.fill(tx,              ty,          tx + TOAST_W,     ty + th,     0xCC101820);
        g.fill(tx,              ty,          tx + TOAST_W,     ty + 1,      0xFF4466AA);
        g.fill(tx,              ty + th - 1, tx + TOAST_W,     ty + th,     0xFF4466AA);
        g.fill(tx,              ty,          tx + 1,           ty + th,     0xFF4466AA);
        g.fill(tx + TOAST_W-1,  ty,          tx + TOAST_W,     ty + th,     0xFF4466AA);

        int ly = ty + 6;
        long ms = result.totalMs();
        String timeStr = ms >= 60_000
                ? I18n.get("smartoptimizer.startup.time.min", ms / 60_000, (ms % 60_000) / 1000)
                : I18n.get("smartoptimizer.startup.time.sec", ms / 1000);

        g.drawString(font, timeStr, tx + 6, ly, 0xFF55FF55, false);
        ly += 11;
        g.drawString(font, I18n.get("smartoptimizer.startup.mods", result.modCount()),
                tx + 6, ly, 0xFF888888, false);
        ly += 11;

        for (int i = 0; i < modLines; i++) {
            StartupProfiler.ModTiming m = slow.get(i);
            String label = m.displayName() + " ~" + String.format("%.1fs", m.estimatedMs() / 1000.0);
            int col = m.estimatedMs() > 5000 ? 0xFFFF6644
                    : m.estimatedMs() > 2000 ? 0xFFFFAA00 : 0xFF88AAFF;
            g.drawString(font, label, tx + 6, ly, col, false);
            ly += 11;
        }
    }
}
