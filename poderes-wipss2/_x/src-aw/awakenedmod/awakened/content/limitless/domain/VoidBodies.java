package com.awakenedmod.awakened.content.limitless.domain;

import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.PhaseState;
import com.awakenedmod.awakened.core.TeleportUtil;
import com.awakenedmod.awakened.core.TempFlight;
import com.awakenedmod.awakened.core.TickFreeze;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class VoidBodies {
   public static final String PHASE_SOURCE = "limitless_void";
   private static final ResourceLocation ALLY_GRAVITY = AwId.of("limitless_void_float");

   private VoidBodies() {
   }

   private static void elsewhere(Entity body) {
      body.clearFire();
      body.setSharedFlagOnFire(false);
      body.setTicksFrozen(0);
   }

   public static void enterPlayer(ServerPlayer player) {
      elsewhere(player);
      floatPlayer(player);
   }

   public static void floatPlayer(ServerPlayer player) {
      PhaseState.setServerFull(player.getUUID(), "limitless_void", true);
      if (!player.isCreative() && !player.isSpectator()) {
         if (!player.getAbilities().flying || !player.getAbilities().mayfly) {
            TempFlight.grant(player);
         }
      } else if (!player.getAbilities().flying) {
         player.getAbilities().flying = true;
         player.onUpdateAbilities();
      }

      player.resetFallDistance();
   }

   public static void pin(ServerPlayer player, Vec3 spot) {
      player.setDeltaMovement(Vec3.ZERO);
      if (player.position().distanceToSqr(spot) > 0.0625) {
         player.teleportTo(spot.x, spot.y, spot.z);
      }

      player.hurtMarked = true;
      player.resetFallDistance();
   }

   public static void releasePlayer(ServerPlayer player, Vec3 entry) {
      PhaseState.setServerFull(player.getUUID(), "limitless_void", false);
      TempFlight.revoke(player, true);
      boolean wedged = !TeleportUtil.fits(player, player.position());
      if (wedged || !realSight(player, entry)) {
         Vec3 back = TeleportUtil.fits(player, entry) ? entry : TeleportUtil.nearestSafeSpot(player, entry, 4.0, 6);
         if (back != null) {
            player.teleportTo(back.x, back.y, back.z);
            player.resetFallDistance();
         } else if (wedged) {
            TeleportUtil.unwedge(player);
         }
      }

      VoidBody.set(player, VoidBody.NONE);
   }

   private static boolean realSight(ServerPlayer player, Vec3 entry) {
      Vec3 mid = new Vec3(0.0, (double)player.getBbHeight() * 0.5, 0.0);
      return player.serverLevel().clip(new ClipContext(player.position().add(mid), entry.add(mid), Block.COLLIDER, Fluid.NONE, player)).getType() == Type.MISS;
   }

   public static void dropPlayer(UUID id, @Nullable ServerPlayer player) {
      PhaseState.setServerFull(id, "limitless_void", false);
      if (player != null) {
         TempFlight.revoke(player, player.isAlive());
         VoidBody.set(player, VoidBody.NONE);
      }
   }

   public static void catchMob(LivingEntity mob, Vec3 centre, VoidBody body) {
      elsewhere(mob);
      TickFreeze.freeze(mob);
      Vec3 to = centre.subtract(mob.position());
      float yaw = (float)Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0F;
      mob.setYRot(yaw);
      mob.yRotO = yaw;
      mob.setYHeadRot(yaw);
      mob.setYBodyRot(yaw);
      mob.setXRot(-55.0F);
      mob.xRotO = -55.0F;
      mob.setDeltaMovement(Vec3.ZERO);
      pushSync(mob);
      VoidBody.set(mob, body);
   }

   public static void releaseMob(LivingEntity mob, Vec3 entry) {
      TickFreeze.release(mob);
      if (mob.isAlive()) {
         VoidBody.set(mob, VoidBody.NONE);
         mob.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false));
         mob.resetFallDistance();
         outOfWalls(mob, entry);
      }
   }

   public static void enterAlly(LivingEntity ally, VoidBody body) {
      elsewhere(ally);
      Attrs.set(ally, Attributes.GRAVITY, ALLY_GRAVITY, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
      ally.setDeltaMovement(ally.getDeltaMovement().multiply(1.0, 0.0, 1.0));
      VoidBody.set(ally, body);
   }

   public static void releaseAlly(LivingEntity ally, Vec3 entry) {
      Attrs.set(ally, Attributes.GRAVITY, ALLY_GRAVITY, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
      if (ally.isAlive()) {
         VoidBody.set(ally, VoidBody.NONE);
         ally.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false));
         ally.resetFallDistance();
         outOfWalls(ally, entry);
      }
   }

   private static void outOfWalls(LivingEntity body, Vec3 entry) {
      if (!TeleportUtil.fits(body, body.position())) {
         Vec3 back = TeleportUtil.fits(body, entry) ? entry : TeleportUtil.nearestSafeSpot(body, entry, 4.0, 6);
         if (back != null) {
            TeleportUtil.moveBody(body, back);
            body.setDeltaMovement(Vec3.ZERO);
            pushSync(body);
         }
      }
   }

   private static void pushSync(Entity e) {
      e.hasImpulse = true;
   }
}
