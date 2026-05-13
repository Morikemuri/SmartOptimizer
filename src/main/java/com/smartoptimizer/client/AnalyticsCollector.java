package com.smartoptimizer.client;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.core.OverlayState;
import com.smartoptimizer.jvm.JvmIssueState;
import com.smartoptimizer.startup.StartupProfiler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AnalyticsCollector {

    private static int  tick            = 0;
    private static int  prevFps         = -1;
    private static int  prevEntityCount = -1;
    private static int  prevChunkCount  = -1;
    private static int  prevMemPct      = -1;
    private static int  worldTicks      = 0;

    // Title-screen gate / wizard - show once per session
    private static boolean titleScreenChecked = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END) return;

            while (ClientSetup.TOGGLE_PANEL.consumeClick()) {
                OverlayState.pinned = !OverlayState.pinned;
            }

            Minecraft mc = Minecraft.getInstance();

            // ── Fallback: trigger profiler for custom modpack title screens ──────
            if (!StartupProfiler.isTriggered() && mc.screen != null && mc.level == null) {
                StartupProfiler.onTitleScreenReady();
            }

            // ── Title screen: gate screen or first-launch wizard ──────────────
            if (!titleScreenChecked && mc.screen instanceof TitleScreen) {
                titleScreenChecked = true;
                if (JvmIssueState.shouldShowGateScreen()) {
                    JvmIssueState.markGateShown();
                    mc.setScreen(new JvmGateScreen(mc.screen));
                } else if (JvmIssueState.isWizardPending()) {
                    mc.setScreen(new FirstLaunchWizardScreen(mc.screen));
                }
            }

            // ── In-world JVM alert (after 3 s) ───────────────────────────────
            if (mc.level != null) {
                worldTicks++;
                if (worldTicks == 60 && mc.screen == null && JvmIssueState.shouldShowAlertScreen()) {
                    JvmIssueState.markAlertShown();
                    mc.setScreen(new JvmAlertScreen());
                }
            } else {
                worldTicks = 0;
            }

            // ── Stats every 20 ticks (1 s) ────────────────────────────────────
            if (++tick % 20 != 0) return;

            if (mc.level == null) return;

            int fps         = safeGetFps(mc);
            int entityCount = safeCountEntities(mc);
            int chunkCount  = safeGetChunkCount(mc);
            int memPct      = safeGetMemPct();

            // Performance alerts
            if (prevFps > 30 && fps < 20 && fps > 0)
                OverlayState.triggerAlert(I18n.get("smartoptimizer.alert.fps", fps));
            if (prevEntityCount >= 0 && entityCount > 200 && prevEntityCount <= 200)
                OverlayState.triggerAlert(I18n.get("smartoptimizer.alert.entities", entityCount));
            if (prevChunkCount >= 0 && chunkCount > 150 && prevChunkCount <= 150)
                OverlayState.triggerAlert(I18n.get("smartoptimizer.alert.chunks", chunkCount));
            if (prevMemPct >= 0 && memPct > 90 && prevMemPct <= 90)
                OverlayState.triggerAlert(I18n.get("smartoptimizer.alert.memory", memPct));

            prevFps         = fps;
            prevEntityCount = entityCount;
            prevChunkCount  = chunkCount;
            prevMemPct      = memPct;

            // ── Dynamic settings auto-tune ────────────────────────────────────
            DynamicSettingsManager.tick(mc);

        } catch (Exception e) {
            // Never let analytics crash the game - just swallow and continue
        }
    }

    private static int safeGetFps(Minecraft mc) {
        try { return mc.getFps(); } catch (Exception e) { return prevFps; }
    }

    private static int safeCountEntities(Minecraft mc) {
        try {
            int count = 0;
            for (Entity ignored : mc.level.entitiesForRendering()) count++;
            return count;
        } catch (Exception e) { return prevEntityCount; }
    }

    private static int safeGetChunkCount(Minecraft mc) {
        try { return mc.level.getChunkSource().getLoadedChunksCount(); }
        catch (Exception e) { return prevChunkCount; }
    }

    private static int safeGetMemPct() {
        try {
            long max  = Runtime.getRuntime().maxMemory();
            long used = max - Runtime.getRuntime().freeMemory();
            return (int) (used * 100 / max);
        } catch (Exception e) { return prevMemPct; }
    }
}
