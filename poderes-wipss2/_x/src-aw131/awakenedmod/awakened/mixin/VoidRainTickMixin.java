package com.awakenedmod.awakened.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(
   targets = {"net.minecraft.client.renderer.WeatherEffectRenderer"}
)
public abstract class VoidRainTickMixin {
}
