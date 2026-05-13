package com.smartoptimizer.compatibility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Persistent registry of Mixin class names that should NOT be loaded.
 *
 * Write path (called from CrashPreventor during mod init):
 *   markDisabledNextLaunch(gameDir, name) - appends to the config file.
 *
 * Read path (called from SmartOptimizerMixinPlugin.onLoad, very early):
 *   loadFromDefaultPath() - reads from "config/smartoptimizer_disabled_mixins.txt"
 *   isDisabled(name) - checks in-memory set.
 *
 * The two-launch design is intentional: Mixins are applied before mod init,
 * so a disable decision made during this session takes effect next launch.
 */
public class DisabledMixinsRegistry {

    private static final String CONFIG_FILE = "config/smartoptimizer_disabled_mixins.txt";
    private static final Set<String> disabled = Collections.synchronizedSet(new HashSet<>());

    public static void loadFromDefaultPath() {
        try {
            Path file = Paths.get(CONFIG_FILE);
            if (!Files.exists(file)) return;
            Files.readAllLines(file).stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .forEach(disabled::add);
        } catch (Exception ignored) {}
    }

    public static boolean isDisabled(String mixinSimpleName) {
        for (String d : disabled) {
            if (mixinSimpleName.endsWith(d) || mixinSimpleName.equals(d)) return true;
        }
        return false;
    }

    public static void markDisabledNextLaunch(Path gameDir, String mixinSimpleName) {
        try {
            Path file = gameDir.resolve("config").resolve("smartoptimizer_disabled_mixins.txt");
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
            // Avoid duplicate entries
            String existing = Files.exists(file) ? Files.readString(file) : "";
            if (!existing.contains(mixinSimpleName)) {
                Files.writeString(file, mixinSimpleName + System.lineSeparator(),
                        StandardOpenOption.APPEND);
            }
        } catch (IOException ignored) {}
    }

    public static void clear(Path gameDir) {
        try {
            Path file = gameDir.resolve("config").resolve("smartoptimizer_disabled_mixins.txt");
            if (Files.exists(file)) Files.delete(file);
            disabled.clear();
        } catch (IOException ignored) {}
    }
}
