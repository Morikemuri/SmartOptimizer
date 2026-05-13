package com.smartoptimizer.jvm;

import java.util.List;

public record JvmBugEntry(
    String id,
    String description,
    String fixArgs,
    String severity,
    boolean proactive,
    List<String> crashSignatures
) {}
