package com.smartoptimizer.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {

    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    public static void ensureDir(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ignored) {}
    }
}
