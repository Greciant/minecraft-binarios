package com.awakenedmod.awakened.client.render;

import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class VfxOwnerHide {
   private static volatile IntPredicate predicate = id -> false;

   private VfxOwnerHide() {
   }

   public static void set(IntPredicate rule) {
      predicate = rule;
   }

   public static boolean hidden(int ownerEntityId) {
      return predicate.test(ownerEntityId);
   }

   public static float ownerDim(int ownerEntityId, float dim) {
      if (!(dim >= 1.0F) && ownerEntityId >= 0) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer me = mc.player;
         return me != null && me.getId() == ownerEntityId && mc.options.getCameraType().isFirstPerson() ? Math.max(0.0F, dim) : 1.0F;
      } else {
         return 1.0F;
      }
   }
}
