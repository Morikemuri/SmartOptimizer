package com.smartoptimizer.config;

import com.smartoptimizer.SmartOptimizerMod;
import com.smartoptimizer.presets.BalancedPreset;
import com.smartoptimizer.presets.HighEndPreset;
import com.smartoptimizer.presets.PotatoPreset;
import com.smartoptimizer.presets.Preset;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    public static void apply(Preset preset, Map<String, Boolean> mods) {
        try {
            List<ConfigEntry> entries = getEntries(preset);
            Path gameDir = FMLPaths.GAMEDIR.get();

            for (ConfigEntry entry : entries) {
                try {
                    Path configPath = resolveConfigPath(gameDir, entry);
                    ConfigWriter.writeKeyValue(configPath, entry.getKey(), entry.getValue());
                    SmartOptimizerMod.LOGGER.debug("Set {}={} in {}", entry.getKey(), entry.getValue(), configPath);
                } catch (Exception e) {
                    SmartOptimizerMod.LOGGER.warn("Failed to apply config entry {}: {}", entry.getKey(), e.getMessage());
                }
            }

            applyModSpecificConfigs(mods, preset, gameDir);
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.error("ConfigManager.apply failed: {}", e.getMessage(), e);
        }
    }

    private static List<ConfigEntry> getEntries(Preset preset) {
        return switch (preset) {
            case POTATO   -> PotatoPreset.getEntries();
            case BALANCED -> BalancedPreset.getEntries();
            case HIGH_END -> HighEndPreset.getEntries();
        };
    }

    private static Path resolveConfigPath(Path gameDir, ConfigEntry entry) {
        if ("minecraft".equals(entry.getModId())) {
            return gameDir.resolve(entry.getFileName());
        }
        return gameDir.resolve("config").resolve(entry.getFileName());
    }

    private static void applyModSpecificConfigs(Map<String, Boolean> mods, Preset preset, Path gameDir) {
        // EntityCulling
        if (Boolean.TRUE.equals(mods.get("entityculling"))) {
            safeWrite(gameDir.resolve("config").resolve("entityculling.toml"),
                    "cullingDistance", switch (preset) {
                        case POTATO   -> "32";
                        case BALANCED -> "64";
                        case HIGH_END -> "128";
                    });
        }

        // FerriteCore - config exists only in older versions; safe to skip if absent
        if (Boolean.TRUE.equals(mods.get("ferritecore"))) {
            Path fc = gameDir.resolve("config").resolve("ferritecore.toml");
            if (fc.toFile().exists()) {
                SmartOptimizerMod.LOGGER.debug("FerriteCore config present, preset {} applied", preset.name());
            }
        }

        // Embeddium / Rubidium - both use the same properties file format
        boolean hasRubidStyle = Boolean.TRUE.equals(mods.get("rubidium"))
                             || Boolean.TRUE.equals(mods.get("embeddium"));
        if (hasRubidStyle) {
            String cfgName = Boolean.TRUE.equals(mods.get("embeddium"))
                    ? "embeddium-options.properties"
                    : "rubidium-options.properties";
            Path path = gameDir.resolve("config").resolve(cfgName);
            if (path.toFile().exists()) {
                // Only tune quality-related settings; never force unsafe flags
                String quality = switch (preset) {
                    case POTATO   -> "false";
                    case BALANCED -> "false";
                    case HIGH_END -> "true";
                };
                safeWrite(path, "mipmap_levels", switch (preset) {
                    case POTATO -> "0"; case BALANCED -> "2"; case HIGH_END -> "4";
                });
            }
        }
    }

    private static void safeWrite(Path path, String key, String value) {
        try {
            ConfigWriter.writeKeyValue(path, key, value);
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("safeWrite failed for {}/{}: {}", path.getFileName(), key, e.getMessage());
        }
    }
}
