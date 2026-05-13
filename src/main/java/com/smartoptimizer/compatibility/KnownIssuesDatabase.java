package com.smartoptimizer.compatibility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.detection.ModDetector;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KnownIssuesDatabase {

    /**
     * severity levels: info < warn < severe < fatal
     * actions: warnOnly | forceSafeMode | setOption:key=val | disableMixin:Name | disableIntegration:mod
     */
    public record KnownIssue(
        String id,
        String severity,
        String description,
        List<String> actions,
        String recommendation
    ) {}

    public static List<KnownIssue> getIssuesFor(Map<String, Boolean> mods) {
        List<KnownIssue> triggered = new ArrayList<>();
        try {
            JsonArray all = loadDatabase();
            if (all == null) return triggered;

            for (JsonElement element : all) {
                try {
                    JsonObject entry = element.getAsJsonObject();
                    String type = entry.get("type").getAsString();
                    boolean matches = false;

                    if ("single_mod".equals(type)) {
                        String modId = entry.get("modId").getAsString();
                        if (!Boolean.TRUE.equals(mods.get(modId))) continue;

                        if (entry.has("affectedVersionPrefixes")) {
                            String ver = ModDetector.getModVersion(modId);
                            if (ver == null) continue;
                            for (JsonElement pfx : entry.getAsJsonArray("affectedVersionPrefixes")) {
                                if (ver.startsWith(pfx.getAsString())) { matches = true; break; }
                            }
                        } else {
                            matches = true;
                        }

                        if (matches && entry.has("requiresAbsent")) {
                            String absent = entry.get("requiresAbsent").getAsString();
                            if (Boolean.TRUE.equals(mods.get(absent))) matches = false;
                        }

                    } else if ("mod_combo".equals(type)) {
                        JsonArray modIds = entry.getAsJsonArray("modIds");
                        matches = true;
                        for (JsonElement m : modIds) {
                            if (!Boolean.TRUE.equals(mods.get(m.getAsString()))) { matches = false; break; }
                        }
                    }

                    if (matches) {
                        List<String> actions = new ArrayList<>();
                        if (entry.has("actions")) {
                            for (JsonElement a : entry.getAsJsonArray("actions")) {
                                actions.add(a.getAsString());
                            }
                        }
                        triggered.add(new KnownIssue(
                            entry.get("id").getAsString(),
                            entry.get("severity").getAsString(),
                            entry.get("description").getAsString(),
                            actions,
                            entry.get("recommendation").getAsString()
                        ));
                    }
                } catch (Exception e) {
                    SmartOptimizerMod.LOGGER.warn("[KnownIssuesDB] Skipping malformed entry: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[KnownIssuesDB] Evaluation failed: {}", e.getMessage());
        }
        return triggered;
    }

    /** Returns issue by ID (used to map fingerprint-detected IDs to actions). */
    public static KnownIssue getById(String id) {
        try {
            JsonArray all = loadDatabase();
            if (all == null) return null;
            for (JsonElement element : all) {
                JsonObject entry = element.getAsJsonObject();
                if (id.equals(entry.get("id").getAsString())) {
                    List<String> actions = new ArrayList<>();
                    if (entry.has("actions")) {
                        for (JsonElement a : entry.getAsJsonArray("actions")) actions.add(a.getAsString());
                    }
                    return new KnownIssue(
                        id,
                        entry.get("severity").getAsString(),
                        entry.get("description").getAsString(),
                        actions,
                        entry.get("recommendation").getAsString()
                    );
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static JsonArray loadDatabase() {
        try (InputStream is = KnownIssuesDatabase.class.getResourceAsStream("/known_issues.json")) {
            if (is == null) {
                SmartOptimizerMod.LOGGER.warn("[KnownIssuesDB] known_issues.json not found in jar");
                return null;
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            return root.getAsJsonArray("issues");
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[KnownIssuesDB] Failed to load: {}", e.getMessage());
            return null;
        }
    }
}
