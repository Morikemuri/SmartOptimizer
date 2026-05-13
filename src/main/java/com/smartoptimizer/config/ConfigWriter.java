package com.smartoptimizer.config;

import com.smartoptimizer.SmartOptimizerMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConfigWriter {

    public static void writeKeyValue(Path filePath, String key, String value) {
        if (!Files.exists(filePath)) return;
        try {
            List<String> lines = Files.readAllLines(filePath);
            List<String> result = new ArrayList<>();
            boolean found = false;
            Pattern pattern = Pattern.compile("^\\s*" + Pattern.quote(key) + "\\s*[=:]");
            for (String line : lines) {
                if (pattern.matcher(line).find()) {
                    char sep = line.contains("=") ? '=' : ':';
                    result.add(key + sep + value);
                    found = true;
                } else {
                    result.add(line);
                }
            }
            if (!found) {
                result.add(key + "=" + value);
            }
            Files.write(filePath, result);
        } catch (IOException e) {
            SmartOptimizerMod.LOGGER.warn("Failed to write config: {} key={}", filePath, key);
        }
    }
}
