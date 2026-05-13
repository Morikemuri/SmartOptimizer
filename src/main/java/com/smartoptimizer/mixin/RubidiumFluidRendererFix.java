package com.smartoptimizer.mixin;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Patches Rubidium 0.7.1 / 0.6.x FluidRenderer NPE: sprite can be null
 * when flowing lava is rendered (vanilla Minecraft bug exposed by Rubidium's
 * render path skipping the null-guard that vanilla's FluidRenderer has).
 * Applied only when Rubidium's FluidRenderer class is on the classpath
 * (see SmartOptimizerMixinPlugin.shouldApplyMixin).
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer",
       remap = false)
public abstract class RubidiumFluidRendererFix {

    // getU(double) - SRG name m_118367_ in Rubidium bytecode
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;m_118367_(D)D",
                     remap = false),
            require = 0,
            remap = false
    )
    private double fixNullSpriteGetU(TextureAtlasSprite sprite, double u) {
        return sprite != null ? sprite.getU(u) : 0.5;
    }

    // getV(double) - SRG name m_118366_ in Rubidium bytecode
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;m_118366_(D)D",
                     remap = false),
            require = 0,
            remap = false
    )
    private double fixNullSpriteGetV(TextureAtlasSprite sprite, double v) {
        return sprite != null ? sprite.getV(v) : 0.5;
    }
}
