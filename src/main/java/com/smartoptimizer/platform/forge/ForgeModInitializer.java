package com.smartoptimizer.platform.forge;

import com.smartoptimizer.core.OptimizerManager;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ForgeModInitializer {

    public static void onSetup(FMLCommonSetupEvent event) {
        OptimizerManager.initialize();
    }
}
