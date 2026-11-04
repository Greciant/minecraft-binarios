package com.awakenedmod.awakened.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({GameRenderer.class})
public interface GameRendererFovAccessor {
   @Invoker("getFov")
   double awakened$getFov(Camera var1, float var2, boolean var3);
}
