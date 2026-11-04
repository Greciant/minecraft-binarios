package com.devilfruits.fruits.kilo_kilo.client;

import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.fruits.kilo_kilo.weight.WeightCurves;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class KiloPose implements AnimationController.PoseLayer {
   private static final ResourceLocation LIGHT = KiloKilo.id("light_pose");
   private static final ResourceLocation HEAVY = KiloKilo.id("heavy_pose");

   @Override
   public void poses(Player player, float partialTick, AnimationController.PoseSink out) {
      double s = KiloClientState.scale(player, partialTick);
      float age = (float)player.tickCount + partialTick;
      if (s < -0.01) {
         out.add(LIGHT, age, (float)(-s));
      } else if (s > 0.01) {
         out.add(HEAVY, age, (float)s);
      }
   }

   @Override
   public void lean(Player player, float partialTick, float[] pitchRoll) {
      double light = WeightCurves.lightness(KiloClientState.kg(player, partialTick));
      if (!(light < 0.01)) {
         Vec3 v = new Vec3(player.getX() - player.xo, 0.0, player.getZ() - player.zo);
         float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * (float) (Math.PI / 180.0);
         double forward = -v.x * (double)Mth.sin(yaw) + v.z * (double)Mth.cos(yaw);
         double side = v.x * (double)Mth.cos(yaw) + v.z * (double)Mth.sin(yaw);
         pitchRoll[0] += (float)(Mth.clamp(forward * 1.4, -0.4, 0.4) * light);
         pitchRoll[1] += (float)(Mth.clamp(side * 1.4, -0.35, 0.35) * light);
      }
   }
}
