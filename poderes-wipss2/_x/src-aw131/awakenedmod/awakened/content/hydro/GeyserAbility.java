package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class GeyserAbility extends Ability {
   private static final float COST = 18.0F;
   private static final int UNLOCK = 3;
   private static final float WATER = 2.0F;
   private static final double AIM_RANGE = 12.0;
   private static final float ERUPT_DAMAGE = 5.5F;
   private static final double LAUNCH_TARGET = 1.35;
   private static final double LAUNCH_SELF = 1.25;
   private static final double RIDE_VY = 0.8;
   private static final int PUSH_TICKS = 5;
   private static final int ERUPT_TICKS = 16;
   private static final int RUMBLE_TICKS = 8;
   private static final int SELF_GLIDE_TICKS = 40;
   private static final double MAX_LEAN = 4.0;
   private static final float ROAR_VOLUME = 0.8F;
   private static final float ROAR_PITCH = 0.7F;
   private final Map<UUID, List<GeyserAbility.Erupt>> erupts = this.playerState();
   private final LandingLock landing = new LandingLock(this.playerState());

   public GeyserAbility() {
      super("geyser", 18.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(5.5).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      LivingEntity target = player.isShiftKeyDown() ? null : AimUtil.aimedLivingEntity(player, 12.0, e -> Harm.allowed(player, e));
      if (target == null && !AimUtil.wantsSelfLaunch(player)) {
         HydroFeedback.aim(player);
         return false;
      } else if (this.landing.locked(player, (Entity)(target != null ? target : player))) {
         return this.fail(player, target != null ? "awakened.hydrokinetic.geyser_airborne" : "awakened.hydrokinetic.geyser_self_airborne", new Object[0]);
      } else {
         Vec3 base = target != null ? target.position() : player.position();
         Hydro.Cast cast = Hydro.take(player, 2.0F);
         Hydro.show(player, cast, base.add(0.0, 1.0, 0.0));
         double power = Math.min(2.5, Math.max(1.0, (double)MasteryScaling.scaled(1.0F, player)));
         if (target != null) {
            this.landing.lock(player, target, 8);
            this.add(player, new GeyserAbility.Erupt(target, base, power, cast, 8, HydroJetFx.geyser(level, base, target, 8, 17, power, cast.wet())));
            Fx.sound(level, base, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 1.0F, 0.5F);
            Fx.shake(level, base, 8.0, 0.04F, 8, 2);
         } else {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * 0.3, 1.25 * power, v.z * 0.3);
            player.hurtMarked = true;
            player.resetFallDistance();
            this.landing.lock(player, player, 0);
            this.add(player, new GeyserAbility.Erupt(null, base, power, cast, 0, HydroJetFx.geyser(level, base, player, 0, 17, power, cast.wet())));
            eruptSounds(level, base, 0.9F);
         }

         return true;
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      List<GeyserAbility.Erupt> list = this.erupts.get(player.getUUID());
      if (list != null) {
         for (GeyserAbility.Erupt e : list) {
            if (e.fuse > 0) {
               e.shell.discard();
            }
         }
      }

      super.clearPlayer(player);
   }

   private void add(ServerPlayer player, GeyserAbility.Erupt erupt) {
      this.erupts.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(erupt);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      List<GeyserAbility.Erupt> list = this.erupts.get(player.getUUID());
      GeyserAbility.Erupt e = list != null && !list.isEmpty() ? list.get(list.size() - 1) : null;
      return e != null && e.target != null ? "hydro_geyser" : "hydro_geyser_self";
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.landing.tick(player);
      List<GeyserAbility.Erupt> list = this.erupts.get(player.getUUID());
      if (list != null) {
         Iterator<GeyserAbility.Erupt> it = list.iterator();

         while (it.hasNext()) {
            if (!this.tick(player, it.next())) {
               it.remove();
            }
         }

         if (list.isEmpty()) {
            this.erupts.remove(player.getUUID());
         }
      }
   }

   private boolean tick(ServerPlayer player, GeyserAbility.Erupt e) {
      if (e.collapse >= 0) {
         return collapseTick(player, e);
      } else {
         LivingEntity rider = (LivingEntity)(e.target != null ? e.target : player);
         if (rider.isAlive() && !rider.isRemoved() && rider.level() == player.serverLevel()) {
            if (e.fuse > 0) {
               return this.rumble(player, e);
            } else {
               if (e.age < 5) {
                  Vec3 v = rider.getDeltaMovement();
                  rider.setDeltaMovement(v.x * 0.6, Math.max(v.y, 0.8 * e.power), v.z * 0.6);
                  rider.hurtMarked = true;
                  if (e.target == null) {
                     rider.resetFallDistance();
                  }
               }

               ServerLevel level = player.serverLevel();
               HydroJetFx.eruptTick(level, e.base, Math.max(e.base.y + 1.0, rider.getY()), e.age, e.power, e.cast.wet(), e.target == null);
               LoopSound.keepAt(level, e.soundKey, e.base, SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.8F, 0.7F);
               if (++e.age > 16) {
                  startCollapse(player, e, rider);
               }

               return true;
            }
         } else if (e.fuse > 0) {
            e.shell.discard();
            this.landing.release(player, rider);
            this.resource(player).refund(player, this.effectiveCost(player));
            return false;
         } else {
            startCollapse(player, e, rider);
            return true;
         }
      }
   }

   private static void startCollapse(ServerPlayer player, GeyserAbility.Erupt e, LivingEntity rider) {
      e.shell.discard();
      e.height = Math.max(1.0, rider.isAlive() ? rider.getY() - e.base.y : 2.0 + 2.5 * e.power);
      HydroJetFx.collapse(player.serverLevel(), e.base, rider, e.height, e.power, e.cast.wet());
      HydroJetFx.burst(player.serverLevel(), top(player, e), e.power, e.target == null);
      e.collapse = 0;
      if (e.target == null && rider == player && player.isAlive() && !player.onGround()) {
         player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
      }
   }

   private static boolean collapseTick(ServerPlayer player, GeyserAbility.Erupt e) {
      if (++e.collapse >= 8) {
         return false;
      } else {
         float f = 1.0F - (float)e.collapse / 8.0F;
         LoopSound.keepAt(player.serverLevel(), e.soundKey, e.base, SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.8F * f, 0.7F);
         return true;
      }
   }

   private static Vec3 top(ServerPlayer player, GeyserAbility.Erupt e) {
      LivingEntity rider = (LivingEntity)(e.target != null ? e.target : player);
      if (rider.isAlive() && rider.level() == player.serverLevel()) {
         Vec3 rel = rider.position().subtract(e.base);
         double lean = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
         double cut = lean > 4.0 ? 4.0 / lean : 1.0;
         return e.base.add(rel.x * cut, Mth.clamp(rel.y, 1.0, 30.0), rel.z * cut);
      } else {
         return e.base.add(0.0, e.height, 0.0);
      }
   }

   private boolean rumble(ServerPlayer player, GeyserAbility.Erupt e) {
      ServerLevel level = player.serverLevel();
      LivingEntity target = e.target;
      Vec3 foot = e.base.add(0.0, 0.1, 0.0);
      HydroJetFx.rumbleTick(level, e.base, 8 - e.fuse, 8);
      if (--e.fuse > 0) {
         return true;
      } else if (!Harm.allowed(player, target)) {
         e.shell.discard();
         this.landing.release(player, target);
         this.resource(player).refund(player, this.effectiveCost(player));
         Fx.burst(level, ParticleTypes.SPLASH, foot, 10, 0.5, 0.08);
         return false;
      } else {
         Hydro.hit(player, target, Hydro.damage(player, this, 5.5F, e.cast));
         MasterySystem.award(player, MasteryReason.HIT, target);
         eruptSounds(level, e.base, 0.7F);
         if (!target.isAlive()) {
            startCollapse(player, e, target);
            return true;
         } else {
            Vec3 v = target.getDeltaMovement();
            target.setDeltaMovement(v.x * 0.2, 1.35 * e.power, v.z * 0.2);
            target.hurtMarked = true;
            return true;
         }
      }
   }

   private static void eruptSounds(ServerLevel level, Vec3 base, float splashPitch) {
      Fx.sound(level, base, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0F, splashPitch);
      Fx.sound(level, base, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.6F, 0.8F);
      Fx.sound(level, base, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.5F, 0.7F);
      Fx.shake(level, base, 10.0, 0.1F, 2, 6);
   }

   private static final class Erupt {
      private final LivingEntity target;
      private final Vec3 base;
      private final double power;
      private final Hydro.Cast cast;
      private int fuse;
      private int age;
      private int collapse = -1;
      private double height;
      private final VfxEntity shell;
      private final String soundKey;

      private Erupt(LivingEntity target, Vec3 base, double power, Hydro.Cast cast, int fuse, VfxEntity shell) {
         this.shell = shell;
         this.target = target;
         this.base = base;
         this.power = power;
         this.cast = cast;
         this.fuse = fuse;
         this.soundKey = "hydro_geyser_" + shell.getId();
      }
   }
}
