package com.devilfruits.fruits.bomu_bomu.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.fruits.bomu_bomu.BomuItems;
import com.devilfruits.fruits.bomu_bomu.item.BombPistolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class BombPistolClient {
   private static final float LOW_X = 0.38F;
   private static final float LOW_Y = -0.34F;
   private static final float LOW_Z = -0.62F;
   private static final float LOW_YAW = 7.0F;
   private static final float LOW_PITCH = -8.0F;
   private static final float READY_X = 0.27F;
   private static final float READY_Y = -0.29F;
   private static final float READY_Z = -0.55F;
   private static final float READY_YAW = 3.0F;
   private static final float READY_PITCH = 1.0F;
   private static float readiness;
   private static long lastFrame;
   private static long shotAt;
   private static float shotKick;
   static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
      public ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
         return !entity.isUsingItem() && BombPistolItem.rounds(stack) > 0 ? ArmPose.CROSSBOW_HOLD : null;
      }

      public boolean applyForgeHandTransform(
         PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess
      ) {
         if (!(stack.getItem() instanceof BombPistolItem)) {
            return false;
         } else {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            boolean reloading = player.isUsingItem() && player.getUseItem() == stack;
            float ready = BombPistolClient.readiness(BombPistolItem.rounds(stack) > 0 && !reloading);
            float x = Mth.lerp(ready, 0.38F, 0.27F);
            float y = Mth.lerp(ready, -0.34F, -0.29F) - equipProcess * 0.6F;
            float z = Mth.lerp(ready, -0.62F, -0.55F);
            float yaw = Mth.lerp(ready, 7.0F, 3.0F);
            float pitch = Mth.lerp(ready, -8.0F, 1.0F);
            float roll = 0.0F;
            if (reloading) {
               int total = BombPistolItem.loadTicks();
               float used = Math.min((float)total, (float)player.getTicksUsingItem() + partialTick);
               float t = used / Math.max(1.0F, (float)total);
               float out = BombPistolClient.window(t, 0.04F, 0.95F, 0.06F);
               float spin = BombPistolClient.window(t, 0.18F, 0.48F, 0.03F);
               float flick = spin * (float)Math.exp((double)(-(used % 3.0F / 0.9F)));
               float blow = BombPistolClient.window(t, 0.51F, 0.85F, 0.07F);
               float snap = (float)Math.exp((double)(-Math.max(0.0F, (t - 0.95F) * (float)total) / 1.2F)) * (t > 0.95F ? 1.0F : 0.0F);
               x += -0.08F * out - 0.15F * blow;
               y += 0.03F * out - 0.012F * flick + 0.11F * blow - 0.02F * snap;
               z += 0.08F * blow;
               roll += 58.0F * out + 6.0F * flick + 20.0F * blow;
               pitch += 16.0F * out + 24.0F * blow;
               yaw += -10.0F * out - 16.0F * blow;
            }

            float since = (float)(Util.getMillis() - BombPistolClient.shotAt) / 50.0F;
            if (since >= 0.0F && since < 12.0F) {
               float k = (float)Math.exp((double)(-since / 2.2F)) * BombPistolClient.shotKick;
               pitch += 22.0F * k;
               z += 0.09F * k;
               y += 0.03F * k;
            }

            pose.translate((float)side * x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees((float)side * yaw));
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.ZP.rotationDegrees((float)side * roll));
            return true;
         }
      }
   };

   static void recoil(float strength) {
      shotAt = Util.getMillis();
      shotKick = strength;
   }

   static void registerProperties() {
      ItemProperties.register(
         (Item)BomuItems.BOMB_PISTOL.get(), DevilFruits.id("bomb_cocked"), (stack, level, entity, seed) -> BombPistolItem.rounds(stack) > 0 ? 1.0F : 0.0F
      );
      ItemProperties.register((Item)BomuItems.BOMB_PISTOL.get(), DevilFruits.id("bomb_load"), (stack, level, entity, seed) -> loadState(stack, entity));
   }

   static float loadState(ItemStack stack, @Nullable LivingEntity entity) {
      if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) {
         int total = BombPistolItem.loadTicks();
         float t = (float)entity.getTicksUsingItem() / (float)Math.max(1, total);
         if (t < 0.08F) {
            return 0.0F;
         } else if (t < 0.18F) {
            return 0.2F;
         } else if (t < 0.48F) {
            int frame = entity.getTicksUsingItem() % 3;
            return frame == 0 ? 0.4F : (frame == 1 ? 0.5F : 0.6F);
         } else {
            return t < 0.95F ? 0.75F : 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   private static float window(float t, float a, float b, float ramp) {
      return smooth((t - a) / ramp + 1.0F) * (1.0F - smooth((t - b) / ramp));
   }

   private static float smooth(float x) {
      float c = Mth.clamp(x, 0.0F, 1.0F);
      return c * c * (3.0F - 2.0F * c);
   }

   private static float readiness(boolean target) {
      long now = System.nanoTime();
      float dt = lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - lastFrame) / 1.0E9F);
      lastFrame = now;
      readiness = readiness + ((target ? 1.0F : 0.0F) - readiness) * (1.0F - (float)Math.exp((double)(-dt * 9.0F)));
      return readiness;
   }

   private BombPistolClient() {
   }
}
