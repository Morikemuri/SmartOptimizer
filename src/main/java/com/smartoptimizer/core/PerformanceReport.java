package com.smartoptimizer.core;

import com.smartoptimizer.detection.CompatibilityChecker;
import com.smartoptimizer.presets.Preset;

import java.util.List;

public class PerformanceReport {
    private final Preset preset;
    private final int estimatedImprovementPct;
    private final List<String> bottlenecks;
    private final List<String> changes;
    private final List<CompatibilityChecker.CompatWarning> compatWarnings;
    private boolean safeModeApplied = false;

    public PerformanceReport(Preset preset, int estimatedImprovementPct,
                             List<String> bottlenecks, List<String> changes,
                             List<CompatibilityChecker.CompatWarning> compatWarnings) {
        this.preset                  = preset;
        this.estimatedImprovementPct = estimatedImprovementPct;
        this.bottlenecks             = bottlenecks;
        this.changes                 = changes;
        this.compatWarnings          = compatWarnings;
    }

    public Preset getPreset()                                           { return preset; }
    public int getEstimatedImprovementPct()                             { return estimatedImprovementPct; }
    public List<String> getBottlenecks()                                { return bottlenecks; }
    public List<String> getChanges()                                    { return changes; }
    public List<CompatibilityChecker.CompatWarning> getCompatWarnings() { return compatWarnings; }
    public boolean isSafeModeApplied()                                  { return safeModeApplied; }
    public void setSafeModeApplied(boolean v)                           { this.safeModeApplied = v; }
}
