package com.awakenedmod.awakened.content.chronokinetic;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TeamTimeLock;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TimeStopAbility extends Ability {
   private static final float COST = 50.0F;
   private static final int UNLOCK = 10;
   private static final double BASE_RADIUS = 18.0;
   private static final float UPFRONT_SHARE = 0.5F;
   private static final int DRAIN_TICKS = 80;
   private static final int MIN_HOLD_TICKS = 10;
   private static final int SYNC_INTERVAL = 5;
   private final Map<UUID, TimeStopAbility.Field> fields = this.playerState();
   private static volatile IntPredicate clientFieldRunning = id -> false;

   public TimeStopAbility() {
      super("time_stop", 50.0F, 10);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().radius(18.0).build();
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return this.fields.containsKey(player.getUUID()) ? 0.0F : upfront();
   }

   @Override
   public String costLabel() {
      return String.valueOf((int)this.billed(upfront()));
   }

   @Override
   public float staminaCost(Player viewer) {
      return ownFieldRunning(viewer) ? 0.0F : upfront();
   }

   @Override
   public String costLabel(Player viewer) {
      return ownFieldRunning(viewer) ? "0" : this.costLabel();
   }

   public static void watchClientFields(IntPredicate running) {
      clientFieldRunning = running;
   }

   private static boolean ownFieldRunning(Player viewer) {
      return viewer != null && clientFieldRunning.test(viewer.getId());
   }

   @Override
   public boolean isActive(ServerPlayer player) {
      return this.fields.containsKey(player.getUUID());
   }

   @Override
   public boolean ignoresSealWhileActive() {
      return true;
   }

   @Override
   public boolean occupiesHands(ServerPlayer player) {
      return false;
   }

   private static float upfront() {
      return StaminaSystem.baseMax() * 0.5F;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      TimeStopAbility.Field field = this.fields.get(player.getUUID());
      return field != null && field.elapsed == 0 && !field.resolving() ? this.id() : null;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      TimeStopAbility.Field running = this.fields.get(player.getUUID());
      if (running != null) {
         if (!running.resolving() && running.elapsed >= 10) {
            resolve(player, running);
            return true;
         } else {
            return this.failSilently(player);
         }
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 center = player.position();
         double radius = MasteryScaling.scaled(18.0, player);
         float drain = this.effectiveCost(player) / 80.0F;
         TimeStopAbility.Field field = new TimeStopAbility.Field(level, center, radius, drain);
         List<LivingEntity> around = AimUtil.livingAround(player, center, radius);
         List<LivingEntity> caught = Harm.controllable(player, around);

         for (LivingEntity target : caught) {
            field.freeze(target);
         }

         for (Projectile p : projectilesAround(level, center, radius)) {
            field.freezeProjectile(p);
         }

         if (field.frozen.isEmpty() && field.projectiles.isEmpty()) {
            return this.fail(player, "awakened.chronokinetic.timestop_empty", new Object[0]);
         } else {
            lockAllies(player, field, around);
            this.fields.put(player.getUUID(), field);
            MasterySystem.award(player, MasteryReason.HIT, caught);
            ChronoFx.timeStop(level, player, center, radius, 160);
            player.displayClientMessage(
               Component.translatable("awakened.chronokinetic.time_stopped", new Object[]{caught.size()}).withStyle(ChatFormatting.GOLD), true
            );
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      TimeStopAbility.Field field = this.fields.get(player.getUUID());
      if (field != null) {
         ServerLevel level = player.serverLevel();
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 8, 1, false, false));
         player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 8, 1, false, false));
         PowerData data = PowerService.data(player);
         if (!field.resolving() && !data.infiniteStamina()) {
            if (StaminaSystem.drainSustained(data, level.getGameTime(), field.drain) <= 0.0F) {
               field.outOfStamina = true;
            }

            if (field.outOfStamina || player.tickCount % 5 == 0) {
               PowerService.sync(player);
            }
         } else {
            StaminaSystem.suppressRegen(data, level.getGameTime());
         }

         List<LivingEntity> around = AimUtil.livingAround(player, field.center, field.radius);
         if (!field.resolving()) {
            for (LivingEntity target : Harm.controllable(player, around)) {
               if (!field.ids.contains(target.getId())) {
                  field.freeze(target);
               }
            }

            for (Projectile p : projectilesAround(level, field.center, field.radius)) {
               if (!field.projIds.contains(p.getId())) {
                  field.freezeProjectile(p);
               }
            }
         }

         field.frozen.removeIf(f -> {
            if (f.target.isAlive()) {
               return false;
            } else {
               if (!f.ally && Foes.defeatedBy(f.target, player)) {
                  field.kills++;
               }

               ChronoSupport.release(f.target);
               field.ids.remove(f.target.getId());
               return true;
            }
         });
         lockAllies(player, field, around);

         for (TimeStopAbility.Frozen frozen : field.frozen) {
            ChronoSupport.hold(frozen.target, frozen.anchor);
         }

         field.projectiles.removeIf(px -> {
            if (px.isAlive()) {
               return false;
            } else {
               ChronoSupport.releaseProjectile(px);
               return true;
            }
         });
         int elapsed = field.elapsed++;
         int sinceResolve = field.sinceResolve();
         ChronoFx.timeStopTick(player, elapsed, sinceResolve);
         if (field.resolving()) {
            if (sinceResolve >= 15) {
               this.end(player, field, field.level);
            }
         } else if (field.outOfStamina || field.elapsed >= 145) {
            resolve(player, field);
         }
      }
   }

   private static void resolve(ServerPlayer player, TimeStopAbility.Field field) {
      field.resolvedAt = field.level.getGameTime();
      ChronoFx.timeStopResolve(field.level, player, field.center, field.radius);
   }

   private static void lockAllies(ServerPlayer player, TimeStopAbility.Field field, List<LivingEntity> around) {
      boolean engaged = TeamTimeLock.engaged(player, Iterables.transform(field.frozen, TimeStopAbility.Frozen::target));
      field.frozen.removeIf(f -> {
         if (f.ally && (!engaged || !TeamTimeLock.allied(player, f.target))) {
            ChronoSupport.release(f.target);
            field.ids.remove(f.target.getId());
            return true;
         } else {
            return false;
         }
      });
      if (engaged && !field.resolving()) {
         for (LivingEntity ally : around) {
            if (!field.ids.contains(ally.getId()) && TeamTimeLock.allied(player, ally)) {
               field.freeze(ally, true);
               TeamTimeLock.tellHeld(player, ally, "awakened.team.time_held_stop");
            }
         }
      }
   }

   private void end(ServerPlayer player, TimeStopAbility.Field field, ServerLevel level) {
      if (field.kills >= 3) {
         QuestSystem.feat(player, "chrono_stopped_execution");
      }

      release(field);
      this.fields.remove(player.getUUID());
      ChronoFx.timeStopEnd(level, player, field.center, field.radius);
   }

   private static void release(TimeStopAbility.Field field) {
      for (TimeStopAbility.Frozen frozen : field.frozen) {
         ChronoSupport.release(frozen.target);
      }

      for (Projectile p : field.projectiles) {
         ChronoSupport.releaseProjectile(p);
      }
   }

   @Override
   public void onDimensionChange(ServerPlayer player, ServerLevel from) {
      TimeStopAbility.Field field = this.fields.get(player.getUUID());
      if (field != null) {
         this.end(player, field, field.level);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      TimeStopAbility.Field field = this.fields.remove(player.getUUID());
      if (field != null) {
         release(field);
         ChronoFx.timeStopEnd(field.level, player, field.center, field.radius);
      }

      super.clearPlayer(player);
   }

   private static List<Projectile> projectilesAround(ServerLevel level, Vec3 center, double radius) {
      return level.getEntitiesOfClass(
         Projectile.class,
         new AABB(center, center).inflate(radius),
         p -> p.isAlive() && p.getDeltaMovement().lengthSqr() > 1.0E-4 && p.distanceToSqr(center) <= radius * radius
      );
   }

   private static final class Field {
      private int elapsed;
      private long resolvedAt = -1L;
      private final float drain;
      private boolean outOfStamina;
      private int kills;
      private final ServerLevel level;
      private final Vec3 center;
      private final double radius;
      private final List<TimeStopAbility.Frozen> frozen = new ArrayList<>();
      private final Set<Integer> ids = new HashSet<>();
      private final List<Projectile> projectiles = new ArrayList<>();
      private final Set<Integer> projIds = new HashSet<>();

      private Field(ServerLevel level, Vec3 center, double radius, float drain) {
         this.level = level;
         this.center = center;
         this.radius = radius;
         this.drain = drain;
      }

      private boolean resolving() {
         return this.resolvedAt >= 0L;
      }

      private int sinceResolve() {
         return this.resolving() ? (int)(this.level.getGameTime() - this.resolvedAt) : -1;
      }

      private void freeze(LivingEntity target) {
         this.freeze(target, false);
      }

      private void freeze(LivingEntity target, boolean ally) {
         ChronoSupport.freeze(target);
         this.frozen.add(new TimeStopAbility.Frozen(target, target.position(), ally));
         this.ids.add(target.getId());
      }

      private void freezeProjectile(Projectile p) {
         ChronoSupport.freezeProjectile(p);
         this.projectiles.add(p);
         this.projIds.add(p.getId());
      }
   }

   private static record Frozen(LivingEntity target, Vec3 anchor, boolean ally) {
   }
}
