package com.smartoptimizer.jvm;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.smartoptimizer.SmartOptimizerMod;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JvmRemoteFetcher {

    private static final String REMOTE_URL       = "https://morikemuri.github.io/smartoptimizer-db/jvm-db.json";
    private static final String REPORT_URL       = "https://smartoptimizer-worker.morikemuri.workers.dev/report";
    private static final long   FETCH_INTERVAL   = 24L * 60 * 60 * 1000; // 24 hours
    private static final int    CONNECT_TIMEOUT  = 3_000;
    private static final int    READ_TIMEOUT     = 5_000;

    /**
     * Returns a fresh (or cached) remote issue list, or null if unavailable.
     * Runs synchronously - call from a background thread or accept the brief block on startup.
     */
    public static JsonArray fetchOrCached(Path configDir) {
        try {
            Path cacheDir   = configDir.resolve("cache");
            Files.createDirectories(cacheDir);
            Path cacheFile  = cacheDir.resolve("jvm-db.json");
            Path tsFile     = cacheDir.resolve("jvm-db.timestamp");

            // Use cached copy if still fresh
            if (Files.exists(cacheFile) && Files.exists(tsFile)) {
                long age = System.currentTimeMillis() - Long.parseLong(Files.readString(tsFile).trim());
                if (age < FETCH_INTERVAL) return parseIssues(Files.readString(cacheFile));
            }

            // Try remote fetch
            String json = fetch(REMOTE_URL);
            if (json != null) {
                Files.writeString(cacheFile, json);
                Files.writeString(tsFile, String.valueOf(System.currentTimeMillis()));
                SmartOptimizerMod.LOGGER.info("[JvmRemote] Updated JVM bug database from remote");
                return parseIssues(json);
            }
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.debug("[JvmRemote] Unavailable (offline?): {}", e.getMessage());
        }
        return null;
    }

    private static String fetch(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("User-Agent", "SmartOptimizer/1.0");
            if (conn.getResponseCode() != 200) return null;
            try (var is = conn.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) { return null; }
    }

    private static JsonArray parseIssues(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("issues");
        } catch (Exception e) { return null; }
    }

    /**
     * Sends a JVM crash report to the SmartOptimizer Worker.
     * Non-blocking - runs in a daemon thread, silently fails if offline.
     */
    public static void sendReport(String crashType, String javaVersion, java.util.List<String> jvmArgs, String description) {
        Thread t = new Thread(() -> {
            try {
                String body = String.format(
                    "{\"crash_type\":\"%s\",\"java_version\":\"%s\",\"jvm_args\":%s,\"description\":\"%s\"}",
                    crashType.replace("\"", ""),
                    javaVersion.replace("\"", ""),
                    com.google.gson.JsonParser.parseString(new com.google.gson.Gson().toJson(jvmArgs)),
                    description.replace("\"", "").replace("\n", " ")
                );
                HttpURLConnection conn = (HttpURLConnection) new URL(REPORT_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "SmartOptimizer/1.0");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                SmartOptimizerMod.LOGGER.debug("[JvmRemote] Report sent, response: {}", code);
            } catch (Exception e) {
                SmartOptimizerMod.LOGGER.debug("[JvmRemote] Report failed (offline?): {}", e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
