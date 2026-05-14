# SmartOptimizer

> Client-side performance mod for Minecraft Forge 1.20.1.  
> Automatically detects your hardware and installed mods, selects the optimal preset, and applies the best possible settings - no manual configuration needed.

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen)
![Forge](https://img.shields.io/badge/Forge-47.3.0-orange)
![Java](https://img.shields.io/badge/Java-17-blue)
![Version](https://img.shields.io/badge/Version-1.0.0%20Prototype-yellow)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red)

---

## What is SmartOptimizer?

SmartOptimizer is a client-side Forge mod that takes the guesswork out of Minecraft performance tuning. Instead of manually digging through settings and mod configs, the mod detects your system's RAM, CPU cores, and installed performance mods at launch, then automatically selects and applies the most suitable configuration preset.

It also monitors your game in real time - detecting FPS drops, window focus changes, and JVM issues - and adjusts settings on the fly to keep the experience as smooth as possible.

---

## Features

### Hardware Detection and Auto-Preset
At startup, SmartOptimizer reads your available RAM, CPU core count, and GPU info. Based on these, it selects one of three presets:

| Preset | RAM / CPU | Render | Simulation | Particles | Entity % |
|--------|-----------|--------|-----------|-----------|----------|
| `POTATO` | Low-end | 6 | 5 | Minimal | 50% |
| `BALANCED` | Mid-range | 10 | 8 | Decreased | 75% |
| `HIGH_END` | High-end | 16 | 12 | All | 100% |

### HUD Overlay
- Toggle with a configurable keybind
- Draggable position (bottom-right by default)
- Shows current live FPS, render distance, simulation distance, particles
- Displays drift warnings (orange) when settings deviate from the applied preset
- "Apply: PRESET" button for one-click optimization

### Dynamic Settings Manager
Continuously monitors and adjusts settings every game tick:
- **Focus loss** - after 350 ms debounce, reduces render to 2, simulation to 2, particles to Minimal, FPS cap to 30
- **Focus gain** - instantly restores previous values
- **FPS-based auto-tuning** - reduces settings on sustained low FPS (<30), restores on high FPS (>50)
- **User override detection** - if you manually change a setting, SmartOptimizer stops touching it

### Optimize My Modpack Screen
Accessible from the Options screen. Shows a two-state view:
1. **Preview** - lists only the settings that differ from the recommended preset, including JVM args if needed
2. **Result** - confirms what was applied

### Modpack Doctor
Scans your loaded mods for known issues and conflicts. Checks both the ModList and physical JAR existence on disk (catches mods that were deleted without a restart). Has a "Fix All" button for one-click resolution.

### JVM Monitoring and Crash Detection
- Scans `hs_err_pid*.log` crash files and matches them against a built-in bug database
- Proactively recommends JVM arguments based on your hardware
- Validates your current JVM args for known-bad combinations
- Shows an in-world alert (3 seconds after world join, once per session) if a JVM issue is detected
- Shows a gate screen on the title screen if a crash loop or sentinel file is detected
- Writes fixes directly to `user_jvm_args.txt` (read by Forge on next launch)

### Startup Profiler
- Measures total boot time and per-mod load times
- Shows a 5-second toast on the title screen identifying the heaviest mods
- Helps pinpoint what is slowing down your modpack startup

### Launch Loop Detector
Counts launches within a 30-minute window. If repeated crashes are detected, triggers safe mode with a guaranteed-stable JVM preset to let you get back into the game.

### Mod Config Tuning
SmartOptimizer can automatically tune configuration files for supported performance mods:
- Rubidium / Embeddium
- FerriteCore
- Entity Culling
- ModernFix
- And more

Backups are created before any config file is modified.

### First-Launch Wizard
On the very first boot, a preference selector walks you through basic options before the game loads.

---

## Installation

1. Make sure you have **Minecraft Forge 1.20.1** (version 47.x) installed.
2. Make sure you are running **Java 17**.
3. Download `SmartOptimizer-1.0.0.jar` from https://www.curseforge.com/minecraft/mc-mods/smartoptimizer
4. Place the JAR in your `mods` folder (`.minecraft/mods/`).
5. Launch the game. SmartOptimizer runs automatically on startup.

---

## How It Works

```
Game Launch
  |
  +-- FMLCommonSetupEvent
        |
        +-- HardwareDetector     --> RAM, CPU cores, GPU name
        +-- ModDetector          --> which performance mods are installed
        +-- PresetSelector       --> POTATO / BALANCED / HIGH_END
        +-- ConfigManager        --> writes mod config files (with backup)
        +-- PerformanceAnalyzer  --> generates PerformanceReport
        +-- OverlayState         --> feeds the HUD overlay
        +-- StartupProfiler      --> measures mod load times
        +-- LaunchLoopDetector   --> detects repeated crash loops
        +-- JVM pipeline:
              JvmCrashDetector
              JvmBugDatabase
              JvmRecommendationEngine
              JvmArgsValidator

Every Client Tick (~20/s)
  |
  +-- Keybind check (toggle overlay)
  +-- First-launch wizard / JVM gate (title screen only)
  +-- In-world JVM alert (3s after world join, once per session)
  +-- Every 20 ticks (1s):
        DynamicSettingsManager
          +-- Read live MC options
          +-- Detect user overrides
          +-- Focus loss/gain handling (350ms debounce)
          +-- FPS-based auto-tuning (every 5s)

Every Frame
  |
  +-- OverlayRenderer (only when overlay is pinned)
```

---

## Package Structure

```
com.smartoptimizer
  SmartOptimizerMod.java          -- Entry point, event bus registration

  analytics/
    AnalyticsCollector.java       -- Client tick listener, FPS/entity/memory monitoring

  client/
    ClientSetup.java              -- Keybind registration
    DynamicSettingsManager.java   -- Real-time settings adjustment
    OverlayRenderer.java          -- HUD rendering
    OverlayState.java             -- Shared overlay state (FPS, drift, alerts)
    TitleScreenHandler.java       -- Injects buttons into OptionsScreen, startup toast

  compatibility/
    CompatChecker.java            -- Mod conflict warnings
    CrashPreventor.java           -- Applies safe settings on detected crash risk

  config/
    ConfigBackupSystem.java       -- Timestamped config backups
    ConfigManager.java            -- Writes tuned configs for performance mods

  core/
    OptimizerManager.java         -- Startup orchestrator (runs on mod loading thread)
    OptimizationResultScreen.java -- "Optimize My Modpack" two-state screen
    ModpackDoctorScreen.java      -- Mod health scan screen

  detection/
    HardwareDetector.java         -- RAM, CPU, GPU detection
    HardwareInfo.java             -- Hardware info record
    ModDetector.java              -- Mod presence check with JAR validation

  jvm/
    JvmAlertScreen.java           -- In-world JVM alert screen
    JvmArgsValidator.java         -- Checks for bad JVM arg combos
    JvmBugDatabase.java           -- Known JVM crash signatures
    JvmCrashDetector.java         -- Scans hs_err_pid*.log files
    JvmGateScreen.java            -- Title screen gate on crash loop
    JvmIssueState.java            -- Shared JVM issue state
    JvmRecommendationEngine.java  -- Hardware-based proactive suggestions
    JvmSentinelManager.java       -- Sentinel file for safe-mode detection

  presets/
    Preset.java                   -- POTATO / BALANCED / HIGH_END enum
    PresetSelector.java           -- Selects preset from HardwareInfo

  rules/
    RuleEngine.java               -- Evaluates rules to pick preset
    LowRAMRule.java
    WeakCPURule.java
    HighEntityRule.java

  startup/
    LaunchLoopDetector.java       -- Counts launches, triggers safe mode
    StartupOptimizerManager.java  -- Coordinates startup optimizations
    StartupProfiler.java          -- Measures boot and per-mod load times

  platform/forge/
    ForgeModInitializer.java      -- Bridges FMLCommonSetupEvent to OptimizerManager
```

---

## Build

Requirements: Java 17, Gradle 8.8

```powershell
$env:JAVA_HOME = "C:/Program Files/Microsoft/jdk-17.0.19.10-hotspot"
./gradlew.bat build --no-daemon
```

Output: `build/libs/SmartOptimizer-1.0.0.jar`

---

## Authors

- **RuleCore**
- **Morikemuri**

---

## License

All Rights Reserved. Do not modify or redistribute without permission.
