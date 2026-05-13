package com.smartoptimizer.startup;

import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CriticalModDetector {

    private static final List<String> KNOWN_CRITICAL = Arrays.asList(
        "rubidium", "oculus", "ferritecore", "entityculling",
        "starlight", "lithium", "phosphor", "iris", "embeddium"
    );

    public static List<String> detect() {
        List<String> found = new ArrayList<>();
        for (String modId : KNOWN_CRITICAL) {
            if (ModList.get().isLoaded(modId)) found.add(modId);
        }
        return found;
    }
}
