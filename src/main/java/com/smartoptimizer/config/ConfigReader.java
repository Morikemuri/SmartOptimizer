package com.smartoptimizer.config;

import com.smartoptimizer.SmartOptimizerMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigReader {

    public static Map<String, String> readKeyValue(Path filePath) {
        Map<String, String> result = new HashMap<>();
        if (!Files.exists(filePath)) return result;
        try {
            List<String> lines = Files.readAllLines(filePath);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue;
                int sep = trimmed.indexOf(':');
                if (sep == -1) sep = trimmed.indexOf('=');
                if (sep != -1) {
                    result.put(trimmed.substring(0, sep).trim(), trimmed.substring(sep + 1).trim());
                }
            }
        } catch (IOException e) {
            SmartOptimizerMod.LOGGER.warn("Failed to read config: {}", filePath);
        }
        return result;
    }
}
