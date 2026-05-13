package com.smartoptimizer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientSetup {

    public static final KeyMapping TOGGLE_PANEL = new KeyMapping(
            "key.smartoptimizer.toggle_panel",
            InputConstants.UNKNOWN.getValue(),
            "key.categories.smartoptimizer"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_PANEL);
    }
}
