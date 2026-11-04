package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.SpiderVisual;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SpiderVisualState {
   private SpiderVisualState() {
   }

   public static SpiderVisual get(Player player) {
      return (SpiderVisual)player.getData(AwakenedAttachments.SPIDER_VISUAL);
   }

   private static void apply(ServerPlayer player, SpiderVisual next) {
      if (!get(player).equals(next)) {
         player.setData(AwakenedAttachments.SPIDER_VISUAL, next);
      }
   }

   public static void setGauntlets(ServerPlayer player, boolean gauntlets) {
      apply(player, get(player).withGauntlets(gauntlets));
   }

   public static void setGliding(ServerPlayer player, boolean gliding) {
      apply(player, get(player).withGliding(gliding));
   }

   public static void setDiving(ServerPlayer player, boolean diving) {
      apply(player, get(player).withDiving(diving));
   }

   public static void setSwing(ServerPlayer player, boolean hanging, boolean leftArm, Vec3 anchor) {
      apply(player, get(player).withTether(1, hanging, leftArm, anchor.x, anchor.y, anchor.z));
   }

   public static void setHanging(ServerPlayer player, boolean hanging) {
      apply(player, get(player).withHanging(hanging));
   }

   public static void setZip(ServerPlayer player, boolean leftArm, Vec3 anchor) {
      apply(player, get(player).withTether(2, false, leftArm, anchor.x, anchor.y, anchor.z));
   }

   public static void clearTether(ServerPlayer player) {
      apply(player, get(player).withTether(0, false, false, 0.0, 0.0, 0.0));
   }

   public static void clearAll(ServerPlayer player) {
      apply(player, SpiderVisual.NONE);
   }
}
