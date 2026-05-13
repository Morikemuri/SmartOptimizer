package com.smartoptimizer.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class SmartOptimizerMixinPlugin implements IMixinConfigPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Check if this Mixin was disabled by a previous session's CrashPreventor
        try {
            String simpleName = mixinClassName.contains(".")
                    ? mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1)
                    : mixinClassName;
            if (com.smartoptimizer.compatibility.DisabledMixinsRegistry.isDisabled(simpleName)) {
                return false;
            }
        } catch (Exception ignored) {}

        // @Pseudo handles missing target class gracefully; require=0 handles non-matching injection points
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
        // Load disabled-Mixin list written by a previous session's CrashPreventor
        try {
            com.smartoptimizer.compatibility.DisabledMixinsRegistry.loadFromDefaultPath();
        } catch (Exception ignored) {}
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass,
                                   String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass,
                                    String mixinClassName, IMixinInfo mixinInfo) {}
}
