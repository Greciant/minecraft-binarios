package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.caster.PlayerCaster;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class FlameBeamAbility extends Ability {
   public static final String ANIMATION = "flame_beam";
   public static final double RANGE = 20.0;
   private static final int UNLOCK = 3;
   private static final int SYNC_INTERVAL = 5;
   public static final int PULSE_INTERVAL = 5;
   public static final int RAMP_TICKS = 60;
   private static final double HIT_RADIUS_NEAR = 0.6;
   private static final double HIT_RADIUS_PER_BLOCK = 0.02;
   private final Map<UUID, FlameBeamEngine.Channel> channels = this.activeState();
   private final List<Ability> guards;

   public FlameBeamAbility(List<Ability> guards) {
      super("flame_beam", 0.0F, 3);
      this.guards = guards;
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String costLabel() {
      return (int)this.billed(9.0F) + "/s";
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.channels.containsKey(player.getUUID())) {
         return this.failSilently(player);
      } else {
         for (Ability guard : this.guards) {
            if (guard.ownsBody(player)) {
               return this.fail(player, "awakened.fail.busy", new Object[0]);
            }
         }

         if (!this.charge(player, 6.0F)) {
            return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            this.channels.put(player.getUUID(), new FlameBeamEngine.Channel());
            PowerService.playAnimation(player, "flame_beam", true);
            FlameBeamEngine.ignite(PlayerCaster.of(player));
            PowerService.sync(player);
            return true;
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.end(player, false);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      FlameBeamEngine.Channel channel = this.channels.get(player.getUUID());
      if (channel != null) {
         ServerLevel level = player.serverLevel();
         PowerData data = PowerService.data(player);
         if (!data.infiniteStamina()) {
            float drain = this.billed(0.45F);
            if (data.stamina() < drain) {
               PowerService.feedback(player, 0);
               this.end(player, true);
               return;
            }

            StaminaSystem.drainSustained(data, level.getGameTime(), drain);
            if (player.tickCount % 5 == 0) {
               PowerService.sync(player);
            }
         }

         LivingEntity fullBurn = FlameBeamEngine.tick(PlayerCaster.of(player), channel);
         if (fullBurn != null && !channel.fullBurnReported() && Foes.counts(player, fullBurn)) {
            channel.markFullBurnReported();
            QuestSystem.feat(player, "pyro_full_burn");
         }
      }
   }

   private void end(ServerPlayer player, boolean dry) {
      FlameBeamEngine.Channel channel = this.channels.remove(player.getUUID());
      if (channel != null) {
         PowerService.stopAnimation(player);
         FlameBeamEngine.shutdown(PlayerCaster.of(player), channel, dry);
         PowerService.sync(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.channels.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }

   private static double hitRadius(double distance) {
      return 0.6 + 0.02 * Mth.clamp(distance, 0.0, 20.0);
   }

   public static Vec3 beamEnd(Level level, Player caster, Vec3 eye) {
      return beamEnd(level, caster, eye, caster.getViewVector(1.0F));
   }

   public static Vec3 beamEnd(Level level, Entity caster, Vec3 eye, Vec3 dir) {
      Vec3 reach = eye.add(dir.scale(20.0));
      BlockHitResult blocked = level.clip(new ClipContext(eye, reach, Block.COLLIDER, Fluid.NONE, caster));
      return blocked.getType() == Type.BLOCK ? blocked.getLocation() : reach;
   }

   public static FlameBeamAbility.LineHit firstOnLine(Level level, Entity caster, Vec3 eye, Vec3 end, Predicate<LivingEntity> eligible) {
      Vec3 span = end.subtract(eye);
      double length = span.length();
      if (length < 1.0E-4) {
         return null;
      } else {
         Vec3 dir = span.scale(1.0 / length);
         AABB box = new AABB(eye, end).inflate(hitRadius(20.0));
         LivingEntity first = null;
         double bestAlong = Double.MAX_VALUE;

         for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, c -> c != caster && c.isAlive() && eligible.test(c))) {
            AABB hull = e.getBoundingBox();
            double along = Mth.clamp(hull.getCenter().subtract(eye).dot(dir), 0.0, length);
            if (!(along >= bestAlong)) {
               Vec3 on = eye.add(dir.scale(along));
               double gapX = Math.max(Math.max(hull.minX - on.x, on.x - hull.maxX), 0.0);
               double gapY = Math.max(Math.max(hull.minY - on.y, on.y - hull.maxY), 0.0);
               double gapZ = Math.max(Math.max(hull.minZ - on.z, on.z - hull.maxZ), 0.0);
               double gap = Math.sqrt(gapX * gapX + gapY * gapY + gapZ * gapZ);
               if (gap <= hitRadius(along)) {
                  bestAlong = along;
                  first = e;
               }
            }
         }

         return first == null ? null : new FlameBeamAbility.LineHit(first, bestAlong);
      }
   }

   public static record LineHit(LivingEntity entity, double dist) {
   }
}
