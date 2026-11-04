package com.devilfruits.client.render;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public final class SkinOverrides {
   private static final List<SkinOverrides.Provider> PROVIDERS = new ArrayList<>();

   public static void register(SkinOverrides.Provider provider) {
      PROVIDERS.add(provider);
   }

   @Nullable
   public static ResourceLocation skin(AbstractClientPlayer player) {
      for (int i = 0; i < PROVIDERS.size(); i++) {
         ResourceLocation r = PROVIDERS.get(i).skin(player);
         if (r != null) {
            return r;
         }
      }

      return null;
   }

   private SkinOverrides() {
   }

   @FunctionalInterface
   public interface Provider {
      @Nullable
      ResourceLocation skin(AbstractClientPlayer var1);
   }
}
