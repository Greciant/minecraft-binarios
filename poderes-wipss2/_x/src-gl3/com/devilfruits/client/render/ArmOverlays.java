package com.devilfruits.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;

public final class ArmOverlays {
   private static final List<ArmOverlays.Overlay> OVERLAYS = new ArrayList<>();

   public static void register(ArmOverlays.Overlay overlay) {
      OVERLAYS.add(overlay);
   }

   public static void render(AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, boolean right, PoseStack pose, MultiBufferSource buffers, int light) {
      for (int i = 0; i < OVERLAYS.size(); i++) {
         OVERLAYS.get(i).render(player, arm, sleeve, right, pose, buffers, light);
      }
   }

   public static boolean hideSleeve(AbstractClientPlayer player, boolean right) {
      for (int i = 0; i < OVERLAYS.size(); i++) {
         if (OVERLAYS.get(i).hideSleeve(player, right)) {
            return true;
         }
      }

      return false;
   }

   private ArmOverlays() {
   }

   public interface Overlay {
      void render(AbstractClientPlayer var1, ModelPart var2, ModelPart var3, boolean var4, PoseStack var5, MultiBufferSource var6, int var7);

      default boolean hideSleeve(AbstractClientPlayer player, boolean right) {
         return false;
      }
   }
}
