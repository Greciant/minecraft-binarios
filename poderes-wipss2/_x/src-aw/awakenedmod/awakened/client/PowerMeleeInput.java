package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.net.MeleeSwingPayload;
import com.awakenedmod.awakened.core.power.MeleeProfile;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PowerMeleeInput {
   private static final double CENTRED_HALF_DEG = 12.0;
   private static final double BLOCK_NEARER_BY = 0.5;
   private static long lastSwingTick = Long.MIN_VALUE;

   private PowerMeleeInput() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(PowerMeleeInput::onAttack);
   }

   private static void onAttack(InteractionKeyMappingTriggered event) {
      if (event.isAttack()) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer self = mc.player;
         if (self != null && mc.level != null && !self.isSpectator()) {
            if (!AbilityKeyInput.ownsClick(mc)) {
               MeleeProfile profile = profile(self);
               if (profile != null) {
                  if (!profile.requiresEmptyHand() || self.getMainHandItem().isEmpty()) {
                     if (mc.hitResult instanceof EntityHitResult entityHit && !(entityHit.getEntity() instanceof LivingEntity)) {
                        return;
                     }

                     if (mc.gameMode == null || !mc.gameMode.isDestroying()) {
                        MeleeProfile.ClientAim aim = profile.clientAim();
                        LivingEntity target = AimUtil.aimedLivingSoft(
                           self,
                           mc.level,
                           aim.reach(),
                           aim.coneHalfDegrees(),
                           candidate -> candidate != self && candidate.isAlive() && !candidate.isSpectator()
                        );
                        if (target != null) {
                           if (!aimingAtBlock(mc, self, target)) {
                              event.setCanceled(true);
                              event.setSwingHand(false);
                              long now = mc.level.getGameTime();
                              long since = now - lastSwingTick;
                              if (since < 0L || since >= (long)aim.cadenceTicks()) {
                                 lastSwingTick = now;
                                 self.swing(InteractionHand.MAIN_HAND);
                                 self.resetAttackStrengthTicker();
                                 send(MeleeSwingPayload.INSTANCE);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean aimingAtBlock(Minecraft mc, LocalPlayer self, LivingEntity target) {
      HitResult hit = mc.hitResult;
      if (hit != null && hit.getType() == Type.BLOCK) {
         Vec3 eye = self.getEyePosition();
         Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
         double targetDist = toTarget.length();
         if (eye.distanceTo(hit.getLocation()) + 0.5 >= targetDist) {
            return false;
         } else if (targetDist < 0.001) {
            return false;
         } else {
            double cos = toTarget.scale(1.0 / targetDist).dot(self.getLookAngle().normalize());
            return cos <= Math.cos(Math.toRadians(12.0));
         }
      } else {
         return false;
      }
   }

   private static MeleeProfile profile(LocalPlayer self) {
      ClientPowerState state = ClientPowerState.get();
      if (state.awakened() && !PowerSuppression.isSuppressed(self)) {
         Power power = PowerRegistry.get(state.powerId());
         return power == null ? null : power.meleeProfile(self);
      } else {
         return null;
      }
   }

   private static void send(MeleeSwingPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }
}
