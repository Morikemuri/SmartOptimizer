package com.smartoptimizer.analytics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AnalyticsState {
    public static volatile int fps          = 0;
    public static volatile int entityCount  = 0;
    public static volatile int chunkCount   = 0;
    public static volatile int memUsedPct   = 0;
    public static final List<String> warnings = new CopyOnWriteArrayList<>();
}
