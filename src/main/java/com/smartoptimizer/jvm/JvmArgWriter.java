package com.smartoptimizer.jvm;

import com.smartoptimizer.SmartOptimizerMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JvmArgWriter {

    /**
     * Appends the given JVM args (space-separated) to user_jvm_args.txt.
     * Skips tokens already present. Returns true on success.
     */
    public static boolean applyArgs(String args) {
        try {
            Path file = FMLPaths.GAMEDIR.get().resolve("user_jvm_args.txt");
            String existing = Files.exists(file) ? Files.readString(file) : "";
            String existingLow = existing.toLowerCase();

            List<String> toAdd = new ArrayList<>();
            for (String token : args.split("\\s+")) {
                token = token.trim();
                if (token.isEmpty()) continue;
                String key = extractKey(token);
                if (!existingLow.contains(key.toLowerCase())) toAdd.add(token);
            }

            if (toAdd.isEmpty()) return true;

            StringBuilder sb = new StringBuilder();
            if (existing.isEmpty()) sb.append("# Forge extra JVM arguments\n");
            if (!existing.endsWith("\n") && !existing.isEmpty()) sb.append("\n");
            sb.append("# Added by SmartOptimizer\n");
            for (String arg : toAdd) sb.append(arg).append("\n");

            Files.writeString(file, existing + sb);
            SmartOptimizerMod.LOGGER.info("[JVM] Wrote to user_jvm_args.txt: {}", toAdd);
            return true;
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[JVM] Failed to write user_jvm_args.txt: {}", e.getMessage());
            return false;
        }
    }

    /** True if every token in args is already present in user_jvm_args.txt. */
    public static boolean isAlreadyApplied(String args) {
        try {
            Path file = FMLPaths.GAMEDIR.get().resolve("user_jvm_args.txt");
            if (!Files.exists(file)) return false;
            String existing = Files.readString(file).toLowerCase();
            for (String token : args.split("\\s+")) {
                token = token.trim();
                if (token.isEmpty()) continue;
                if (!existing.contains(extractKey(token).toLowerCase())) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractKey(String token) {
        if (token.startsWith("-Xmx") || token.startsWith("-Xms")) return token.substring(0, 4);
        return token.replaceAll("^-XX:[+\\-]?", "").replaceAll("=.*$", "");
    }
}
