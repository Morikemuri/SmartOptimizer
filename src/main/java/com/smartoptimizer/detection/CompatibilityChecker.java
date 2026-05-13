package com.smartoptimizer.detection;

import com.smartoptimizer.SmartOptimizerMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects known bad mod combinations and broken mod versions.
 * Never throws - all checks are guarded individually.
 */
public class CompatibilityChecker {

    public record CompatWarning(String message, boolean fatal) {}

    public static List<CompatWarning> check(Map<String, Boolean> mods) {
        List<CompatWarning> warnings = new ArrayList<>();
        try { checkOptiFineConflict(mods, warnings);   } catch (Exception e) { log(e); }
        try { checkRubidiumVersion(mods, warnings);    } catch (Exception e) { log(e); }
        try { checkEmbeddiumVersion(mods, warnings);   } catch (Exception e) { log(e); }
        try { checkIrisOnForge(mods, warnings);        } catch (Exception e) { log(e); }
        try { checkOculusIris(mods, warnings);         } catch (Exception e) { log(e); }

        for (CompatWarning w : warnings) {
            SmartOptimizerMod.LOGGER.warn("[SmartOptimizer] Compat{}: {}",
                    w.fatal() ? " FATAL" : "", w.message());
        }
        return warnings;
    }

    // --- individual checks ---

    private static void checkOptiFineConflict(Map<String, Boolean> mods, List<CompatWarning> out) {
        boolean hasOptiFine = Boolean.TRUE.equals(mods.get("optifine"));
        boolean hasRubidium = Boolean.TRUE.equals(mods.get("rubidium"))
                           || Boolean.TRUE.equals(mods.get("embeddium"));
        if (hasOptiFine && hasRubidium) {
            out.add(new CompatWarning("OptiFine + Rubidium/Embeddium: fatal conflict. Remove OptiFine.", true));
        }
    }

    private static void checkRubidiumVersion(Map<String, Boolean> mods, List<CompatWarning> out) {
        if (!Boolean.TRUE.equals(mods.get("rubidium"))) return;
        String ver = ModDetector.getModVersion("rubidium");
        if (ver == null) return;
        if (ver.startsWith("0.7.1") || ver.startsWith("0.6.")) {
            out.add(new CompatWarning(
                "Rubidium " + ver + ": lava crash patched. Update to Embeddium 0.3.9+.", false));
        }
    }

    private static void checkEmbeddiumVersion(Map<String, Boolean> mods, List<CompatWarning> out) {
        if (!Boolean.TRUE.equals(mods.get("embeddium"))) return;
        String ver = ModDetector.getModVersion("embeddium");
        if (ver == null) return;
        if (ver.startsWith("0.1.") || ver.startsWith("0.2.")) {
            out.add(new CompatWarning("Embeddium " + ver + ": AMD crash. Update to 0.3.9+.", false));
        }
    }

    private static void checkIrisOnForge(Map<String, Boolean> mods, List<CompatWarning> out) {
        if (Boolean.TRUE.equals(mods.get("iris")) && !Boolean.TRUE.equals(mods.get("sodium"))) {
            out.add(new CompatWarning("Iris is Fabric-only. Use Oculus for Forge shaders.", false));
        }
    }

    private static void checkOculusIris(Map<String, Boolean> mods, List<CompatWarning> out) {
        if (Boolean.TRUE.equals(mods.get("oculus")) && Boolean.TRUE.equals(mods.get("iris"))) {
            out.add(new CompatWarning("Oculus + Iris both loaded - pick one.", false));
        }
    }

    private static void log(Exception e) {
        SmartOptimizerMod.LOGGER.warn("[SmartOptimizer] CompatibilityChecker error: {}", e.getMessage());
    }
}
