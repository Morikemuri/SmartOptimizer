package com.smartoptimizer.startup;

import net.minecraftforge.fml.ModList;

public class ModpackAnalyzer {

    public static int getModCount() {
        return ModList.get().size();
    }

    public static ModpackType classify(int modCount) {
        if (modCount <= 5)   return ModpackType.VANILLA;
        if (modCount <= 30)  return ModpackType.LIGHT;
        if (modCount <= 100) return ModpackType.MEDIUM;
        return ModpackType.HEAVY;
    }
}
