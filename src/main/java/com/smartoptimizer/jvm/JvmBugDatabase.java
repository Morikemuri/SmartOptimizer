package com.smartoptimizer.jvm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartoptimizer.SmartOptimizerMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JvmBugDatabase {

    /**
     * Matches the current environment against the known JVM issues database.
     * Checks remote/cached database first, falls back to embedded JAR resource.
     */
    public static Optional<JvmBugEntry> match(JvmCrashDetector.ParsedJvmCrash crash,
                                               Map<String, Boolean> mods) {
        try {
            JsonArray issues = loadMerged();
            if (issues == null) return Optional.empty();

            String cpuVendor   = detectCpuVendor();
            int    javaVersion = Runtime.version().feature();
            String currentArgs = String.join(" ",
                    ManagementFactory.getRuntimeMXBean().getInputArguments());

            for (JsonElement el : issues) {
                try {
                    JsonObject e = el.getAsJsonObject();

                    if (e.has("cpuVendorContains") && !e.get("cpuVendorContains").isJsonNull()) {
                        if (!cpuVendor.contains(e.get("cpuVendorContains").getAsString())) continue;
                    }
                    if (e.has("javaVersionMin") && !e.get("javaVersionMin").isJsonNull()) {
                        if (javaVersion < e.get("javaVersionMin").getAsInt()) continue;
                    }
                    if (e.has("javaVersionMax") && !e.get("javaVersionMax").isJsonNull()) {
                        if (javaVersion > e.get("javaVersionMax").getAsInt()) continue;
                    }
                    if (e.has("modsAny") && e.getAsJsonArray("modsAny").size() > 0) {
                        boolean any = false;
                        for (JsonElement m : e.getAsJsonArray("modsAny")) {
                            if (Boolean.TRUE.equals(mods.get(m.getAsString()))) { any = true; break; }
                        }
                        if (!any) continue;
                    }

                    boolean proactive    = e.has("proactiveCheck") && e.get("proactiveCheck").getAsBoolean();
                    boolean crashMatches = false;
                    if (e.has("crashSignatures") && crash != null) {
                        JsonArray sigs = e.getAsJsonArray("crashSignatures");
                        if (sigs.size() > 0) {
                            String lower = crash.content().toLowerCase();
                            crashMatches = true;
                            for (JsonElement sig : sigs) {
                                if (!lower.contains(sig.getAsString().toLowerCase())) { crashMatches = false; break; }
                            }
                        }
                    }
                    if (!crashMatches && !proactive) continue;

                    String fixArgs   = e.get("fixArgs").getAsString();
                    String firstToken = fixArgs.split("\\s+")[0]
                            .replace("-XX:", "").replace("+", "").replace("-", "");
                    if (currentArgs.contains(firstToken)) continue;

                    List<String> sigs = new ArrayList<>();
                    if (e.has("crashSignatures")) {
                        for (JsonElement s : e.getAsJsonArray("crashSignatures")) sigs.add(s.getAsString());
                    }
                    return Optional.of(new JvmBugEntry(
                            e.get("id").getAsString(),
                            e.get("description").getAsString(),
                            fixArgs,
                            e.get("severity").getAsString(),
                            proactive,
                            sigs));

                } catch (Exception ex) {
                    SmartOptimizerMod.LOGGER.warn("[JvmBugDatabase] Skipping bad entry: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[JvmBugDatabase] Match failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /** Loads remote (cached) issues merged with embedded ones. Deduplicates by id. */
    private static JsonArray loadMerged() {
        JsonArray embedded = loadEmbedded();
        JsonArray remote   = null;
        try {
            Path configDir = FMLPaths.GAMEDIR.get().resolve("config/smartoptimizer");
            remote = JvmRemoteFetcher.fetchOrCached(configDir);
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.debug("[JvmBugDatabase] Remote fetch skipped: {}", e.getMessage());
        }

        if (remote == null) return embedded;
        if (embedded == null) return remote;

        // Remote overrides embedded (same id wins remote)
        JsonArray merged = new JsonArray();
        for (JsonElement el : remote) merged.add(el);
        for (JsonElement el : embedded) {
            String id = el.getAsJsonObject().get("id").getAsString();
            boolean overridden = false;
            for (JsonElement r : remote) {
                if (id.equals(r.getAsJsonObject().get("id").getAsString())) { overridden = true; break; }
            }
            if (!overridden) merged.add(el);
        }
        return merged;
    }

    static String detectCpuVendor() {
        try {
            String id = System.getenv("PROCESSOR_IDENTIFIER");
            if (id != null) {
                if (id.contains("AMD"))   return "AMD";
                if (id.contains("Intel")) return "Intel";
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static JsonArray loadEmbedded() {
        try (InputStream is = JvmBugDatabase.class.getResourceAsStream("/known_jvm_issues.json")) {
            if (is == null) { SmartOptimizerMod.LOGGER.warn("[JvmBugDatabase] Embedded DB not found"); return null; }
            return JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("issues");
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[JvmBugDatabase] Load embedded failed: {}", e.getMessage());
            return null;
        }
    }
}
