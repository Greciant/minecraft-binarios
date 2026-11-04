package com.devilfruits.mobs.pirate.client;

import com.devilfruits.core.npc.AnimData;
import com.devilfruits.core.npc.FactionFighter;
import com.devilfruits.mobs.pirate.FireGunPayload;
import com.devilfruits.mobs.pirate.PirateClips;
import com.devilfruits.mobs.pirate.item.FlintlockItem;
import com.devilfruits.mobs.pirate.item.ReloadCues;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PirateGunClient {
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
   private static long lastShot;
   static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
      public ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
         return !entity.isUsingItem() && FlintlockItem.isLoaded(stack) ? ArmPose.CROSSBOW_HOLD : null;
      }

      public boolean applyForgeHandTransform(
         PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess
      ) {
         if (stack.getItem() instanceof FlintlockItem gun) {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            boolean reloading = player.isUsingItem() && player.getUseItem() == stack;
            float ready = PirateGunClient.readiness(FlintlockItem.isLoaded(stack) && !reloading);
            float x = Mth.lerp(ready, 0.38F, 0.27F);
            float y = Mth.lerp(ready, -0.34F, -0.29F) - equipProcess * 0.6F;
            float z = Mth.lerp(ready, -0.62F, -0.55F);
            float yaw = Mth.lerp(ready, 7.0F, 3.0F);
            float pitch = Mth.lerp(ready, -8.0F, 1.0F);
            float roll = 0.0F;
            if (reloading) {
               int action = ReloadCues.action(gun.isMusket());
               int total = stack.getUseDuration(player);
               float t = ReloadCues.drillTick(gun.isMusket(), (float)(total - player.getUseItemRemainingTicks()) + partialTick, total);
               float prime = PirateGunClient.window(t, (float)(PirateClips.HALF_COCK[action] - 4), (float)(PirateClips.PAN_SHUT[action] + 2), 6.0F);
               float upright = PirateGunClient.window(t, (float)(PirateClips.POUR[action] - 6), (float)(PirateClips.ROD_IN[action] + 4), 8.0F);
               float stroke = PirateGunClient.pulse(t, PirateClips.RAM_1[action]) + PirateGunClient.pulse(t, PirateClips.RAM_2[action]);
               float shake = PirateGunClient.window(t, (float)(PirateClips.POUR[action] - 2), (float)(PirateClips.POUR[action] + 4), 2.0F) * Mth.sin(t * 1.3F);
               x += -0.06F * prime - 0.16F * upright;
               y += -0.04F * prime - 0.02F * upright - 0.06F * stroke;
               z += 0.06F * prime - 0.02F * upright;
               pitch += 8.0F * prime + 68.0F * upright - 4.0F * stroke;
               yaw += -4.0F * upright;
               roll += 26.0F * prime + 3.0F * shake;
            }

            float since = (float)player.tickCount + partialTick - (float)PirateGunClient.lastShot;
            if (since >= 0.0F && since < 12.0F) {
               float k = (float)Math.exp((double)(-since / 2.5F));
               pitch += 14.0F * k;
               z += 0.07F * k;
               y += 0.02F * k;
            }

            pose.translate((float)side * x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees((float)side * yaw));
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.ZP.rotationDegrees((float)side * roll));
            return true;
         } else {
            return false;
         }
      }
   };

   static boolean loaded(ItemStack stack, @Nullable LivingEntity entity) {
      if (entity instanceof FactionFighter f) {
         AnimData a = f.anim;
         if (stack == f.getMainHandItem()) {
            return f.hasFlag(8);
         } else {
            float t = a.actionTime(f.tickCount, 0.0F);
            return a.actionId != 13 || !(t >= (float)PirateClips.HIT[13]) || !(t < 60.0F);
         }
      } else {
         return FlintlockItem.isLoaded(stack);
      }
   }

   static float reload(ItemStack stack, @Nullable LivingEntity entity) {
      if (entity instanceof FactionFighter f) {
         return stack != f.getMainHandItem() ? 0.0F : PirateClips.reloadState(f.anim.actionId, f.anim.actionTime(f.tickCount, 0.0F));
      } else if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack && stack.getItem() instanceof FlintlockItem gun) {
         int total = stack.getUseDuration(entity);
         float tick = ReloadCues.drillTick(gun.isMusket(), (float)(total - entity.getUseItemRemainingTicks()), total);
         return PirateClips.reloadState(ReloadCues.action(gun.isMusket()), tick);
      } else {
         return 0.0F;
      }
   }

   static void onInteraction(InteractionKeyMappingTriggered event) {
      if (event.isAttack() && event.getHand() == InteractionHand.MAIN_HAND) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof FlintlockItem && FlintlockItem.isLoaded(stack) && !player.isUsingItem()) {
               event.setCanceled(true);
               event.setSwingHand(false);
               long now = Util.getMillis();
               if (now - lastShot >= 400L && !player.getCooldowns().isOnCooldown(stack.getItem())) {
                  lastShot = now;
                  PacketDistributor.sendToServer(new FireGunPayload(), new CustomPacketPayload[0]);
               }
            }
         }
      }
   }

   private static float readiness(boolean target) {
      long now = System.nanoTime();
      float dt = lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - lastFrame) / 1.0E9F);
      lastFrame = now;
      readiness = readiness + ((target ? 1.0F : 0.0F) - readiness) * (1.0F - (float)Math.exp((double)(-dt * 9.0F)));
      return readiness;
   }

   private static float window(float t, float a, float b, float ramp) {
      return smooth((t - a) / ramp + 1.0F) * (1.0F - smooth((t - b) / ramp));
   }

   private static float smooth(float x) {
      float c = Mth.clamp(x, 0.0F, 1.0F);
      return c * c * (3.0F - 2.0F * c);
   }

   private static float pulse(float t, int at) {
      float d = (t - (float)at) / 3.0F;
      return (float)Math.exp((double)(-d * d));
   }

   private PirateGunClient() {
   }
}
