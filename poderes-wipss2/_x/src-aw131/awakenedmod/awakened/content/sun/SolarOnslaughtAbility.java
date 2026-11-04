package com.awakenedmod.awakened.content.sun;

import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class SolarOnslaughtAbility extends Ability {
   private static final float PIN_COST = 12.0F;
   private static final float PUNCH_COST = 3.0F;
   private static final int UNLOCK = 7;
   private static final double ACQUIRE_RANGE = 6.0;
   private static final double LUNGE_SPEED = 1.05;
   private static final double LUNGE_PER_BLOCK = 0.22;
   private static final double WALL_REACH = 2.6;
   private static final double BREAK_DISTANCE = 6.5;
   private static final int GRACE_TICKS = 12;
   private static final double STOP_DISTANCE = 1.6;
   private static final int LUNGE_MAX_TICKS = 10;
   private static final int PUNCH_INTERVAL = 5;
   private static final int AWARD_EVERY = 6;
   private static final float BASE_PUNCH_DMG = 1.5F;
   private static final int MAX_TICKS = 200;
   private final Map<UUID, SolarOnslaughtAbility.Pin> pins = this.activeState();

   public SolarOnslaughtAbility() {
      super("solar_onslaught", 12.0F, 7);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.pins.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         LivingEntity target = AimUtil.aimedLivingEntity(player, 6.0, e -> Harm.allowed(player, e));
         if (target == null) {
            return this.fail(player, "awakened.sun.pin_no_target", new Object[0]);
         } else if (!Harm.controls(player, target)) {
            return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
         } else {
            ServerLevel level = player.serverLevel();
            Vec3 wall = findWallAnchor(player, target);
            SolarOnslaughtAbility.Pin pin = new SolarOnslaughtAbility.Pin(target, wall != null ? wall : target.position(), wall != null);
            if (!(target instanceof Player)) {
               TickFreeze.freeze(target);
            }

            this.pins.put(player.getUUID(), pin);
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            double gap = Math.sqrt(dx * dx + dz * dz);
            if (gap > 1.6) {
               double speed = Math.min(1.05, gap * 0.22);
               Vec3 dir = AimUtil.horizontalDir(player.position(), target.position());
               player.setDeltaMovement(dir.x * speed, 0.1, dir.z * speed);
               player.hurtMarked = true;
               player.resetFallDistance();
            } else {
               pin.lunging = false;
               pin.anchor = player.position();
            }

            this.hold(player, pin);
            PowerService.playAnimation(player, "solar_onslaught_pin");
            Fx.burst(
               level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0), 18, 0.4, 0.05
            );
            Vfx.sphere(level, pinCenter(pin), 0.8, 16761390, 8, 1.0F);
            Fx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.7F);
            Fx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("awakened.sun.pinned").withStyle(ChatFormatting.GOLD), true);
            MasterySystem.award(player, MasteryReason.HIT, target);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      SolarOnslaughtAbility.Pin pin = this.pins.get(player.getUUID());
      if (pin != null) {
         LivingEntity target = pin.target;
         if (target.isAlive() && !target.isRemoved()) {
            if (pin.ticks >= 12 && (double)player.distanceTo(target) > 6.5) {
               this.finish(player, pin, false);
            } else {
               if (pin.lunging) {
                  double dx = player.getX() - target.getX();
                  double dz = player.getZ() - target.getZ();
                  if (dx * dx + dz * dz <= 2.5600000000000005 || pin.ticks >= 10) {
                     pin.lunging = false;
                     pin.anchor = player.position();
                  }
               }

               if (pin.anchor != null) {
                  player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 250, false, false));
                  Vec3 v = player.getDeltaMovement();
                  player.setDeltaMovement(0.0, Math.min(0.0, v.y), 0.0);
                  player.hurtMarked = true;
                  player.resetFallDistance();
               }

               this.hold(player, pin);
               if (pin.ticks > 0 && pin.ticks % 5 == 0) {
                  PowerData data = PowerService.data(player);
                  if (!data.infiniteStamina()) {
                     float punchCost = this.billed(3.0F);
                     if (data.stamina() < punchCost) {
                        this.finish(player, pin, false);
                        return;
                     }

                     StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), punchCost);
                     PowerService.sync(player);
                  }

                  this.punch(player, pin);
               }

               if (++pin.ticks >= 200) {
                  this.finish(player, pin, false);
               }
            }
         } else {
            this.finish(player, pin, true);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      SolarOnslaughtAbility.Pin pin = this.pins.get(player.getUUID());
      if (pin != null) {
         this.finish(player, pin, false);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      SolarOnslaughtAbility.Pin pin = this.pins.remove(player.getUUID());
      if (pin != null) {
         if (!(pin.target instanceof Player)) {
            TickFreeze.release(pin.target);
         }

         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }

   private void hold(ServerPlayer player, SolarOnslaughtAbility.Pin pin) {
      LivingEntity target = pin.target;
      target.setPos(pin.pinPos.x, pin.pinPos.y, pin.pinPos.z);
      target.setDeltaMovement(Vec3.ZERO);
      target.hurtMarked = true;
      target.resetFallDistance();
      target.setSprinting(false);
      if (target instanceof Player) {
         Harm.effect(player, target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 9, false, false));
         Harm.effect(player, target, new MobEffectInstance(MobEffects.WEAKNESS, 8, 4, false, false));
         Harm.effect(player, target, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 8, 4, false, false));
      }
   }

   private void punch(ServerPlayer player, SolarOnslaughtAbility.Pin pin) {
      ServerLevel level = player.serverLevel();
      LivingEntity target = pin.target;
      PowerData data = PowerService.data(player);
      float m = Solar.scale(data, 0.6F, 1.8F);
      if (Solar.hasAxe(player)) {
         m *= 1.5F;
      }

      float dmg = MasteryScaling.scaledDamage(1.5F, player, this.balanceKey(), m - 1.0F);
      Dmg.hurt(target, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), dmg);
      PowerService.playAnimation(player, pin.nextHook ? "solar_onslaught_hook" : "solar_onslaught_jab");
      pin.nextHook = !pin.nextHook;
      Vec3 hit = pinCenter(pin);
      Vfx.sphere(level, hit, 0.4 + 0.14 * (double)m, 16770688, 5, 1.4F);
      Fx.burst(level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), hit, 7, 0.3, 0.12);
      if (pin.wallPinned) {
         Fx.burst(level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), hit, 6, 0.45, 0.02);
      }

      Fx.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 0.7F);
      Fx.sound(level, target.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.45F, 1.4F);
      if (++pin.blows % 6 == 0) {
         MasterySystem.award(player, MasteryReason.HIT, target);
      }
   }

   private void finish(ServerPlayer player, SolarOnslaughtAbility.Pin pin, boolean targetDied) {
      this.pins.remove(player.getUUID());
      creditGrind(player, pin);
      if (!(pin.target instanceof Player)) {
         TickFreeze.release(pin.target);
      }

      PowerService.stopAnimation(player);
      ServerLevel level = player.serverLevel();
      Vec3 at = pin.target.isAlive() ? pinCenter(pin) : pin.target.position();
      double radius = 1.4 + 0.5 * (double)Solar.scale(player, 0.6F, 1.8F);
      SunFx.detonation(level, at, radius);
      LivingEntity target = pin.target;
      if (!targetDied && target.isAlive()) {
         float m = Solar.scale(PowerService.data(player), 0.6F, 1.8F) * (Solar.hasAxe(player) ? 1.5F : 1.0F);
         Dmg.hurt(target, level, level.damageSources().playerAttack(player), MasteryScaling.scaledDamage(1.5F, player, this.balanceKey(), m - 1.0F));
         Vec3 away = AimUtil.horizontalDir(player.position(), target.position());
         Harm.setMotion(player, target, away.x * 0.9, 0.45, away.z * 0.9);
      }
   }

   private static void creditGrind(ServerPlayer player, SolarOnslaughtAbility.Pin pin) {
      if (Foes.defeatedBy(pin.target, player)) {
         QuestSystem.feat(player, "sun_pinned_down");
      }
   }

   private static Vec3 pinCenter(SolarOnslaughtAbility.Pin pin) {
      return pin.target.position().add(0.0, (double)pin.target.getBbHeight() * 0.55, 0.0);
   }

   private static Vec3 findWallAnchor(ServerPlayer player, LivingEntity target) {
      ServerLevel level = player.serverLevel();
      Vec3 from = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
      Vec3 look = player.getLookAngle();
      Vec3 dir = new Vec3(look.x, 0.0, look.z);
      if (dir.lengthSqr() < 1.0E-4) {
         return null;
      } else {
         dir = dir.normalize();
         Vec3 to = from.add(dir.scale(2.6));
         ClipContext ctx = new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, player);
         BlockHitResult hit = level.clip(ctx);
         if (hit.getType() != Type.BLOCK) {
            return null;
         } else {
            Vec3 face = hit.getLocation().subtract(dir.scale((double)target.getBbWidth() * 0.5 + 0.05));
            return new Vec3(face.x, target.getY(), face.z);
         }
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   private static final class Pin {
      private final LivingEntity target;
      private final Vec3 pinPos;
      private final boolean wallPinned;
      private int ticks;
      private int blows;
      private boolean lunging = true;
      private Vec3 anchor;
      private boolean nextHook;

      private Pin(LivingEntity target, Vec3 pinPos, boolean wallPinned) {
         this.target = target;
         this.pinPos = pinPos;
         this.wallPinned = wallPinned;
      }
   }
}
