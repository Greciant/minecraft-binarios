package com.awakenedmod.awakened.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(
   targets = {"net.minecraft.client.renderer.chunk.ChunkSectionsToRender"},
   priority = 500
)
public abstract class VoidChunkLayerMixin {
}
