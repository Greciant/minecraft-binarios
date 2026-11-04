package com.devilfruits.core.logia;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.registry.DFAttachments;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class LogiaDash {
   public static final ResourceLocation FX = DevilFruits.id("logia/dash");
   public static final ResourceLocation COOLDOWN = DevilFruits.id("logia_dash");
   public static final int ARRIVAL_GRACE = 10;
   public static final LogiaMovementMode MODE = new LogiaMovementMode() {
      private final ResourceLocation id = DevilFruits.id("dash");

      @Override
      public ResourceLocation id() {
         return this.id;
      }

      @Override
      public boolean tick(ServerPlayer player, LogiaMovement.Session session) {
         player.fallDistance = 0.0F;
         int ticks = (Integer)session.data.get("ticks");
         if (session.data.get("frame") instanceof PhysicsStructure frame && frame.isValid()) {
            session.data.put("to", frame.toWorld((Vec3)session.data.get("localTo")));
         }

         Vec3 to = (Vec3)session.data.get("to");
         return session.ticks < ticks ? true : player.position().distanceToSqr(to) > 0.36 && session.ticks < ticks + 10;
      }

      @Override
      public Vec3 reformTarget(ServerPlayer player, LogiaMovement.Session session) {
         return (Vec3)session.data.get("to");
      }

      @Override
      public boolean staysElemental(ServerPlayer player, LogiaMovement.Session session) {
         return (Boolean)session.data.get("elemental");
      }

      @Override
      public boolean forcesBody() {
         return true;
      }

      @Override
      public int maxTicks() {
         return 60;
      }
   };

   public static LogiaDash.Result dash(ServerPlayer player) {
      return dash(player, player.getEyePosition(), AimHelper.look(player), (Double)CoreConfig.LOGIA_DASH_RANGE.get());
   }

   public static LogiaDash.Result dash(ServerPlayer player, Vec3 eye, Vec3 look, double range) {
      if (!LogiaManager.isLogia(player)) {
         return LogiaDash.Result.NOT_LOGIA;
      } else {
         PlayerFruitData data = DFAttachments.data(player);
         LogiaState state = data.logia().state();
         long now = player.level().getGameTime();
         if (data.isOnCooldown(COOLDOWN, now)) {
            return LogiaDash.Result.COOLDOWN;
         } else if (!LogiaManager.bodyReady(player) && !player.isSpectator()) {
            LogiaManager.fizzle(player);
            return LogiaDash.Result.NOT_READY;
         } else if (state != LogiaState.REFORMING && LogiaMovement.session(player) == null && !player.isSpectator()) {
            float cost = ((Double)CoreConfig.LOGIA_DASH_ENERGY.get()).floatValue();
            boolean free = AbilityManager.freeEnergy(player);
            if (!free && data.energy() < cost) {
               return LogiaDash.Result.NO_ENERGY;
            } else {
               Vec3 target = target(player, eye, look, range);
               Optional<Vec3> spot = LogiaReform.find(player, player.position(), target, 2, false);
               if (spot.isEmpty()) {
                  return LogiaDash.Result.NO_ROOM;
               } else {
                  Vec3 from = player.position();
                  Vec3 to = spot.get();
                  boolean elemental = state == LogiaState.ELEMENTAL_BODY;
                  if (!LogiaMovement.start(player, MODE)) {
                     return LogiaDash.Result.NOT_READY;
                  } else {
                     LogiaMovement.Session session = LogiaMovement.session(player);
                     int ticks = flightTicks(from.distanceTo(to));
                     if (player.onGround() && from.distanceToSqr(to) > 1.0E-4) {
                        StructureForces.reaction(player, to.subtract(from).normalize().scale((Double)CoreConfig.LOGIA_DASH_SPEED.get()), 1.0);
                     }

                     session.data.put("to", to);
                     PhysicsWorld.bridge().raycast(player.level(), to.add(0.0, 0.5, 0.0), to.add(0.0, -1.0, 0.0), player).ifPresent(deck -> {
                        session.data.put("frame", deck.structure());
                        session.data.put("localTo", deck.structure().toLocal(to));
                     });
                     session.data.put("ticks", ticks);
                     session.data.put("elemental", elemental);
                     ServerFx.send(player, FX, from, to, 1.0F, player.getRandom().nextLong(), ticks);
                     if (!free) {
                        data.setEnergy(data.energy() - cost);
                     }

                     data.startCooldown(COOLDOWN, now, (Integer)CoreConfig.LOGIA_DASH_COOLDOWN.get());
                     return LogiaDash.Result.OK;
                  }
               }
            }
         } else {
            return LogiaDash.Result.NOT_READY;
         }
      }
   }

   public static int flightTicks(double distance) {
      return Mth.clamp(Mth.ceil(distance / (Double)CoreConfig.LOGIA_DASH_SPEED.get()), 3, 40);
   }

   public static double progress(double t) {
      double c = Mth.clamp(t, 0.0, 1.0);
      return 1.0 - (1.0 - c) * (1.0 - c);
   }

   static Vec3 target(ServerPlayer player, Vec3 eye, Vec3 look, double range) {
      Vec3 end = eye.add(look.scale(range));
      BlockHitResult hit = PhysicsWorld.clip(player.level(), new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
      if (hit.getType() != Type.MISS) {
         if (hit.getDirection() == Direction.UP && PhysicsWorld.worldNormal(player.level(), hit).y > 0.7) {
            return hit.getLocation();
         }

         end = hit.getLocation().subtract(look.scale(0.6));
      }

      return end.subtract(0.0, (double)player.getEyeHeight(), 0.0);
   }

   private LogiaDash() {
   }

   public static enum Result {
      OK,
      NOT_LOGIA,
      NOT_READY,
      COOLDOWN,
      NO_ENERGY,
      NO_ROOM;
   }
}
