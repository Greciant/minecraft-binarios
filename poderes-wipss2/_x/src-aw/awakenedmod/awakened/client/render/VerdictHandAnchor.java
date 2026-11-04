package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.client.ClipClock;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class VerdictHandAnchor {
   private static final ResourceLocation WHIRL = AwId.of("verdict_whirl");
   private static final ResourceLocation THROW = AwId.of("verdict_throw");
   private static final ClipClock WHIRL_CLOCK = new ClipClock(WHIRL);
   private static final ClipClock THROW_CLOCK = new ClipClock(THROW);
   private static final float[] WHIRL_HAND = new float[]{
      0.453F,
      0.302F,
      0.32F,
      0.245F,
      0.298F,
      0.364F,
      0.045F,
      0.218F,
      0.379F,
      -0.003F,
      0.172F,
      0.379F,
      0.077F,
      0.216F,
      0.371F,
      0.171F,
      0.264F,
      0.34F,
      0.371F,
      0.267F,
      0.39F,
      0.567F,
      0.195F,
      0.412F,
      0.622F,
      0.161F,
      0.404F,
      0.551F,
      0.229F,
      0.373F
   };
   private static final float[] THROW_HAND = new float[]{
      0.268F,
      0.36F,
      0.183F,
      0.111F,
      0.377F,
      -0.029F,
      0.173F,
      0.371F,
      0.081F,
      0.445F,
      -0.019F,
      0.558F,
      0.41F,
      -0.647F,
      0.536F,
      0.333F,
      -0.836F,
      0.327F,
      0.32F,
      -0.856F,
      0.233F,
      0.312F,
      -0.867F,
      0.136F,
      0.312F,
      -0.868F,
      0.102F,
      0.312F,
      -0.869F,
      0.068F,
      0.312F,
      -0.87F,
      0.034F,
      0.312F,
      -0.87F,
      0.0F
   };
   private static final float[] REST = new float[]{0.3125F, -0.87F, 0.0F};
   private static final float PIVOT_RIGHT = 0.3125F;
   private static final float PIVOT_UP = -0.245F;
   private static final double FP_WIDTH = 0.37;
   private static final double FP_DROP = 0.37;

   private VerdictHandAnchor() {
   }

   public static float whirlPhase(Player player, float partialTick) {
      long age = WHIRL_CLOCK.age(player);
      if (age < 0L) {
         return -1.0F;
      } else {
         float t = ((float)age + partialTick) / 10.0F;
         return t - (float)Math.floor((double)t);
      }
   }

   public static float whirlAge(Player player, float partialTick) {
      long age = WHIRL_CLOCK.age(player);
      return age < 0L ? -1.0F : (float)age + partialTick;
   }

   public static Vec3 hand(Player player, float partialTick, Vec3 camera) {
      float whirl = whirlAge(player, partialTick);
      float r;
      float u;
      float f;
      if (whirl >= 0.0F) {
         int n = WHIRL_HAND.length / 3;
         float pos = (whirl % (float)n + (float)n) % (float)n;
         int i0 = (int)pos;
         int i1 = (i0 + 1) % n;
         float k = pos - (float)i0;
         r = Mth.lerp(k, WHIRL_HAND[i0 * 3], WHIRL_HAND[i1 * 3]);
         u = Mth.lerp(k, WHIRL_HAND[i0 * 3 + 1], WHIRL_HAND[i1 * 3 + 1]);
         f = Mth.lerp(k, WHIRL_HAND[i0 * 3 + 2], WHIRL_HAND[i1 * 3 + 2]);
      } else {
         long throwAge = THROW_CLOCK.age(player);
         if (throwAge >= 0L) {
            int n = THROW_HAND.length / 3;
            float pos = Math.min((float)(n - 1), (float)throwAge + partialTick);
            int i0 = (int)pos;
            int i1 = Math.min(n - 1, i0 + 1);
            float k = pos - (float)i0;
            r = Mth.lerp(k, THROW_HAND[i0 * 3], THROW_HAND[i1 * 3]);
            u = Mth.lerp(k, THROW_HAND[i0 * 3 + 1], THROW_HAND[i1 * 3 + 1]);
            f = Mth.lerp(k, THROW_HAND[i0 * 3 + 2], THROW_HAND[i1 * 3 + 2]);
         } else {
            r = REST[0];
            u = REST[1];
            f = REST[2];
         }
      }

      Minecraft mc = Minecraft.getInstance();
      return player == mc.player && mc.options.getCameraType().isFirstPerson()
         ? firstPerson(player, partialTick, camera, r, u, f)
         : thirdPerson(player, partialTick, r, u, f);
   }

   private static Vec3 thirdPerson(Player player, float partialTick, float r, float u, float f) {
      Vec3 eye = player.getPosition(partialTick).add(0.0, (double)player.getEyeHeight(), 0.0);
      float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
      Vec3 right = AimUtil.horizontalRight((double)bodyYaw);
      Vec3 fwd = AimUtil.direction((double)bodyYaw, 0.0);
      return eye.add(right.scale((double)r)).add(0.0, (double)u, 0.0).add(fwd.scale((double)f));
   }

   private static Vec3 firstPerson(Player player, float partialTick, Vec3 camera, float r, float u, float f) {
      float yaw = player.getViewYRot(partialTick);
      double pitch = Math.toRadians((double)AwakenedAnimations.firstPersonArmPitch(player.getViewXRot(partialTick)));
      double ar = (double)(r - 0.3125F);
      double au = (double)(u - -0.245F);
      double af = (double)f;
      double cp = Math.cos(pitch);
      double sp = Math.sin(pitch);
      double tf = af * cp + au * sp;
      double tu = au * cp - af * sp;
      Vec3 right = AimUtil.horizontalRight((double)yaw);
      Vec3 fwd = AimUtil.direction((double)yaw, 0.0);
      return camera.add(right.scale(0.37 + ar)).add(0.0, -0.37 + tu, 0.0).add(fwd.scale(tf));
   }
}
