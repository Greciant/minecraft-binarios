package com.awakenedmod.awakened.core;

import net.minecraft.resources.ResourceLocation;

public final class AwId {
   private AwId() {
   }

   public static ResourceLocation of(String path) {
      return ResourceLocation.fromNamespaceAndPath("awakened", path);
   }
}
