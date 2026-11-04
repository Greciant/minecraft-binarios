package com.devilfruits.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;

public final class BodyModels {
   private static final List<BodyModels.Provider> PROVIDERS = new ArrayList<>();

   public static void register(BodyModels.Provider provider) {
      PROVIDERS.add(provider);
   }

   public static boolean render(AbstractClientPlayer player, PlayerModel<?> posed, PoseStack pose, MultiBufferSource buffers, int light, int overlay, int color) {
      for (int i = 0; i < PROVIDERS.size(); i++) {
         if (PROVIDERS.get(i).render(player, posed, pose, buffers, light, overlay, color)) {
            return true;
         }
      }

      return false;
   }

   private BodyModels() {
   }

   @FunctionalInterface
   public interface Provider {
      boolean render(AbstractClientPlayer var1, PlayerModel<?> var2, PoseStack var3, MultiBufferSource var4, int var5, int var6, int var7);
   }
}
