package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.ArbiterEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ArbiterAnchors {
   private static final double FEET_TRANSLATE = 1.501;

   private ArbiterAnchors() {
   }

   public static Vec3 scepterHand(ArbiterEntity boss, float partialTick) {
      return hand(boss, partialTick, true);
   }

   public static Vec3 freeHand(ArbiterEntity boss, float partialTick) {
      return hand(boss, partialTick, false);
   }

   public static Vec3 hand(ArbiterEntity boss, float partialTick, boolean right) {
      return world(boss, partialTick, ArbiterModel.hand(rigFor(boss, partialTick), right));
   }

   public static Vec3 yoke(ArbiterEntity boss, float partialTick) {
      return world(boss, partialTick, ArbiterModel.shoulder(rigFor(boss, partialTick)));
   }

   private static ArbiterModel.Rig rigFor(ArbiterEntity boss, float partialTick) {
      ArbiterModel.Rig rig = new ArbiterModel.Rig();
      ArbiterModel.rig(rig, (float)boss.tickCount + partialTick, boss.getAttackAnim(partialTick), boss.getPhase(), boss.isKneeling(), boss.inRecess());
      return rig;
   }

   public static float pinnedYawOrBody(ArbiterEntity boss, float partialTick) {
      return !boss.isKneeling() && boss.deathTime <= 0 ? Mth.rotLerp(partialTick, boss.yBodyRotO, boss.yBodyRot) : boss.pinnedYaw();
   }

   private static Vec3 world(ArbiterEntity boss, float partialTick, Vector3f model) {
      double s = (double)(boss.getScale() * 1.15F);
      double lx = (double)(-model.x) / 16.0 * s;
      double ly = (1.501 - (double)model.y / 16.0) * s;
      double lz = (double)model.z / 16.0 * s;
      double rad = Math.toRadians((double)pinnedYawOrBody(boss, partialTick));
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);
      return boss.getPosition(partialTick).add(-lx * cos + lz * sin, ly, -lx * sin - lz * cos);
   }
}
