package com.devilfruits.fruits.kage_kage.client;

import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.item.MoriaBladeItem;
import com.devilfruits.fruits.kage_kage.item.MoriaScissorsItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

final class DualGuardPose implements AnimationController.PoseLayer {
   private static final ResourceLocation GUARD = KageKage.id("dual_guard");

   @Override
   public void poses(Player player, float partialTick, AnimationController.PoseSink out) {
      if (player.getMainHandItem().getItem() instanceof MoriaScissorsItem
         && MoriaScissorsItem.isSplit(player.getMainHandItem())
         && player.getOffhandItem().getItem() instanceof MoriaBladeItem) {
         out.add(GUARD, (float)player.tickCount + partialTick, 1.0F);
      }
   }
}
