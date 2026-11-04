package com.devilfruits.core.data;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public final class LegacyData {
   @Nullable
   private static ResourceLocation singleFruitOwner;

   public static void setSingleFruitOwner(ResourceLocation fruit) {
      if (singleFruitOwner != null && !singleFruitOwner.equals(fruit)) {
         throw new IllegalStateException("Legacy single-fruit saves are already claimed by " + singleFruitOwner);
      } else {
         singleFruitOwner = fruit;
      }
   }

   @Nullable
   public static ResourceLocation singleFruitOwner() {
      return singleFruitOwner;
   }

   private LegacyData() {
   }
}
