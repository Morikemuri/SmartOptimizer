package com.smartoptimizer;

import com.smartoptimizer.client.AnalyticsCollector;
import com.smartoptimizer.client.ClientSetup;
import com.smartoptimizer.client.OverlayRenderer;
import com.smartoptimizer.client.TitleScreenHandler;
import com.smartoptimizer.platform.forge.ForgeModInitializer;
import com.smartoptimizer.startup.StartupProfiler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SmartOptimizerMod.MOD_ID)
public class SmartOptimizerMod {

    public static final String MOD_ID = "smartoptimizer";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    // Captured as early as possible to measure total startup time
    @SuppressWarnings("unused")
    private static final long BOOT_MS = StartupProfiler.PROCESS_START_MS;

    public SmartOptimizerMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::onRegisterKeyMappings);
            MinecraftForge.EVENT_BUS.register(OverlayRenderer.class);
            MinecraftForge.EVENT_BUS.register(AnalyticsCollector.class);
            MinecraftForge.EVENT_BUS.register(TitleScreenHandler.class);
        });
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("SmartOptimizer initialized");
        ForgeModInitializer.onSetup(event);
    }
}
