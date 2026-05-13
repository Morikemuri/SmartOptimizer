package com.smartoptimizer.startup;

import java.util.List;

public class ModpackProfile {
    private final int modCount;
    private final ModpackType type;
    private final List<String> criticalMods;
    private final long analysisMs;

    public ModpackProfile(int modCount, ModpackType type, List<String> criticalMods, long analysisMs) {
        this.modCount    = modCount;
        this.type        = type;
        this.criticalMods = criticalMods;
        this.analysisMs  = analysisMs;
    }

    public int getModCount()              { return modCount; }
    public ModpackType getType()          { return type; }
    public List<String> getCriticalMods() { return criticalMods; }
    public long getAnalysisMs()           { return analysisMs; }
}
