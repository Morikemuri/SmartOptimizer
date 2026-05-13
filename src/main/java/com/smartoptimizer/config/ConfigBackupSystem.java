package com.smartoptimizer.config;

import com.smartoptimizer.SmartOptimizerMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;

public class ConfigBackupSystem {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String[] EXTENSIONS = { ".toml", ".json", ".cfg", ".properties" };

    /**
     * Backs up all config files + options.txt into a timestamped folder.
     * Returns the backup directory path, or null on failure.
     */
    public static Path backup() {
        try {
            Path gameDir   = FMLPaths.GAMEDIR.get();
            Path configDir = gameDir.resolve("config");
            Path backupDir = gameDir.resolve("config/smartoptimizer/backups")
                    .resolve(LocalDateTime.now().format(TS));

            Files.createDirectories(backupDir);

            // options.txt
            Path options = gameDir.resolve("options.txt");
            if (Files.exists(options)) {
                Files.copy(options, backupDir.resolve("options.txt"), StandardCopyOption.REPLACE_EXISTING);
            }

            // All config files
            if (Files.exists(configDir)) {
                Files.walkFileTree(configDir, EnumSet.noneOf(FileVisitOption.class), 3,
                        new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                String name = file.getFileName().toString().toLowerCase();
                                boolean isConfigFile = false;
                                for (String ext : EXTENSIONS) {
                                    if (name.endsWith(ext)) { isConfigFile = true; break; }
                                }
                                if (!isConfigFile) return FileVisitResult.CONTINUE;
                                // Skip our own backups directory
                                if (file.toString().contains("smartoptimizer" + File.separator + "backups"))
                                    return FileVisitResult.CONTINUE;
                                try {
                                    Path rel  = configDir.relativize(file);
                                    Path dest = backupDir.resolve(rel);
                                    Files.createDirectories(dest.getParent());
                                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException ignored) {}
                                return FileVisitResult.CONTINUE;
                            }
                        });
            }

            SmartOptimizerMod.LOGGER.info("[Backup] Created: {}", backupDir);
            return backupDir;
        } catch (Exception e) {
            SmartOptimizerMod.LOGGER.warn("[Backup] Failed: {}", e.getMessage());
            return null;
        }
    }

    /** Returns how many backup folders exist. */
    public static int backupCount() {
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("config/smartoptimizer/backups");
            if (!Files.exists(dir)) return 0;
            try (var s = Files.list(dir)) { return (int) s.filter(Files::isDirectory).count(); }
        } catch (Exception e) { return 0; }
    }
}
