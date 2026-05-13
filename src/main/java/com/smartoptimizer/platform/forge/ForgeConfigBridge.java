package com.smartoptimizer.platform.forge;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgeConfigBridge {

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }
}
