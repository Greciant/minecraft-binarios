package com.devilfruits.fruits.uo_uo_seiryu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.AABB;

public final class HybridSize {
   public static final ResourceLocation MODIFIER = UoUoSeiryu.id("adaptive_size");
   private static final double SHRINK_PER_TICK = 0.1;
   private static final double GROW_PER_TICK = 0.04;

   public static void tick(ServerPlayer player) {
      AttributeInstance scale = player.getAttribute(Attributes.SCALE);
      if (scale != null) {
         AttributeModifier current = scale.getModifier(MODIFIER);
         boolean active = UoUoSeiryu.isIn(player, "hybrid") && UoUoSeiryu.fruit().config().flag("hybridAdaptiveSize") && !player.isSpectator();
         if (!active) {
            if (current != null) {
               scale.removeModifier(MODIFIER);
            }
         } else if (player.tickCount % 2 == 0) {
            double shrink = current == null ? 0.0 : -current.amount();
            double full = scale.getValue() + shrink;
            double maxShrink = Math.max(0.0, full - 1.0);
            double target = 0.0;
            if (!fits(player, full)) {
               target = maxShrink;

               for (double s = 0.1; s < maxShrink; s += 0.1) {
                  if (fits(player, full - s)) {
                     target = s;
                     break;
                  }
               }
            }

            if (!(Math.abs(target - shrink) < 1.0E-4)) {
               double next = target > shrink ? Math.min(target, shrink + 0.1) : Math.max(target, shrink - 0.04);
               if (next < 1.0E-4) {
                  scale.removeModifier(MODIFIER);
               } else {
                  scale.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER, -next, Operation.ADD_VALUE));
               }
            }
         }
      }
   }

   public static boolean fits(ServerPlayer player, double scale) {
      EntityDimensions standing = player.getDimensions(Pose.STANDING);
      double unit = scale / (double)player.getScale();
      double w = (double)standing.width() * unit * 0.5;
      double h = (double)standing.height() * unit;
      AABB box = new AABB(player.getX() - w, player.getY(), player.getZ() - w, player.getX() + w, player.getY() + h, player.getZ() + w);
      return player.level().noCollision(player, box.deflate(1.0E-7));
   }

   private HybridSize() {
   }
}
