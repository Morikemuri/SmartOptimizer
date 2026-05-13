package com.smartoptimizer.core;

import com.smartoptimizer.SmartOptimizerMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FirstLaunchController {

    private static final String MARKER_FILE = "smartoptimizer_applied.flag";
    private final Path markerPath;

    public FirstLaunchController() {
        this.markerPath = FMLPaths.CONFIGDIR.get().resolve(MARKER_FILE);
    }

    public boolean isFirstLaunch() {
        return !Files.exists(markerPath);
    }

    public void markApplied() {
        try {
            Files.writeString(markerPath, "applied=true");
        } catch (IOException e) {
            SmartOptimizerMod.LOGGER.warn("Could not write first-launch marker: {}", e.getMessage());
        }
    }
}
