package com.smartoptimizer.client;

import com.smartoptimizer.core.OverlayState;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class LagDiagnosticEngine {

    public record Cause(String description, String fix, int severity) {}
    public record Report(Cause primary, List<Cause> secondary) {}

    public static Report analyze() {
        Minecraft mc = Minecraft.getInstance();
        int fps = OverlayState.getLiveFps();
        int render = OverlayState.getLiveRender();

        long maxMem  = Runtime.getRuntime().maxMemory();
        long usedMem = maxMem - Runtime.getRuntime().freeMemory();
        int memPct   = (int)(usedMem * 100 / Math.max(1, maxMem));

        int entityCount = 0;
        int chunkCount  = 0;
        try {
            if (mc.level != null) {
                for (var ignored : mc.level.entitiesForRendering()) entityCount++;
                chunkCount = mc.level.getChunkSource().getLoadedChunksCount();
            }
        } catch (Exception ignored) {}

        List<Cause> causes = new ArrayList<>();

        // Memory pressure
        if (memPct > 90) {
            causes.add(new Cause("Memory pressure: " + memPct + "% used",
                    "Add -XX:+UseZGC to JVM args or install FerriteCore", 3));
        } else if (memPct > 75) {
            causes.add(new Cause("High memory usage: " + memPct + "%",
                    "Consider adding FerriteCore or MemoryLeakFix", 2));
        }

        // Entity overload
        if (entityCount > 300) {
            causes.add(new Cause("Extreme entity count: " + entityCount + " entities",
                    "Reduce simulation distance or add EntityCulling", 3));
        } else if (entityCount > 150) {
            causes.add(new Cause("High entity count: " + entityCount + " entities",
                    "Install EntityCulling for better performance", 2));
        }

        // Chunk overload
        if (chunkCount > 200) {
            causes.add(new Cause("Too many loaded chunks: " + chunkCount,
                    "Reduce render distance (currently " + render + " chunks)", 3));
        } else if (chunkCount > 120) {
            causes.add(new Cause("High chunk load: " + chunkCount + " chunks",
                    "Reducing render distance by 2 will help", 1));
        }

        // Low FPS without obvious cause
        if (fps > 0 && fps < 20 && causes.isEmpty()) {
            causes.add(new Cause("Low FPS (" + fps + ") - possible GPU or CPU bottleneck",
                    "Install Embeddium/Rubidium for GPU acceleration", 2));
        }

        // Render distance too high for hardware
        long maxMB = maxMem / (1024 * 1024);
        if (render > 16 && maxMB < 4096) {
            causes.add(new Cause("High render distance (" + render + " chunks) on low RAM",
                    "Reduce render distance to 12 or lower", 2));
        }

        if (causes.isEmpty()) {
            causes.add(new Cause("No obvious bottleneck detected",
                    "Game appears to be running well!", 0));
        }

        causes.sort((a, b) -> b.severity() - a.severity());
        Cause primary = causes.get(0);
        List<Cause> secondary = causes.subList(1, causes.size());
        return new Report(primary, secondary);
    }
}
