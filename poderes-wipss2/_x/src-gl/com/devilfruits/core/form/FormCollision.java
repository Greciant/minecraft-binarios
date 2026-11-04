package com.devilfruits.core.form;

import com.devilfruits.registry.DFAttachments;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class FormCollision {
   public static boolean passesThroughLeaves(@Nullable Entity entity) {
      if (entity instanceof Player player && player.hasData(DFAttachments.PLAYER_DATA)) {
         FruitForm form = DFAttachments.data(player).form();
         return form != null && form.passesThroughLeaves();
      }

      return false;
   }

   public static boolean fireImmune(@Nullable Entity entity) {
      if (entity instanceof Player player && player.hasData(DFAttachments.PLAYER_DATA)) {
         FruitForm form = DFAttachments.data(player).form();
         return form != null && form.fireImmune();
      }

      return false;
   }

   private FormCollision() {
   }
}
