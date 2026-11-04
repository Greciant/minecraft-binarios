package com.awakenedmod.awakened.core.npc;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public interface ClipBearer {
   @Nullable
   ResourceLocation activeClip();

   int clipSequence();

   float clipAge(float var1);
}
