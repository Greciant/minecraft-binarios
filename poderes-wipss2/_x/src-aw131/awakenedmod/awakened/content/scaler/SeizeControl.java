package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.ForcedCameraPayload;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SeizeControl {
   private static final int DASH_TIMEOUT_TICKS = 20;
   private static final double DASH_SPEED = 0.9;
   private static final double CONTACT_RANGE_MULT = 1.2;
   private static final int RUN_TICKS = 30;
   private static final int DAMAGE_INTERVAL = 5;
   private static final float HIT_BASE = 1.0F;
   private static final float HIT_FACTOR_SPECK = 0.6F;
   private static final float HIT_FACTOR_MITE = 0.8F;
   private static final int SHAKE_TOGGLES = 4;
   private static final int CAMERA_TICKS = 65;
   private static final double[][] PATH = new double[][]{{0.0, 0.05, 0.65}, {0.65, 0.45, 0.25}, {0.25, 1.05, -0.15}, {-0.55, 0.75, -0.45}, {0.0, 0.35, -0.95}};
   private static final float HOLD_DRAIN = 0.5F;
   private static final int ESCAPE_BRUTE = 8;
   private static final int ESCAPE_TITAN = 14;
   private static final int ESCAPE_COLOSSUS = 25;
   private static final float CLAMBER_HOLD_DRAIN = 0.4F;
   private static final int HOLD_LOOP_AT = 7;
   private static final int SQUIRM_INTERVAL = 12;
   private static final double ANCHOR_FORWARD = 0.8;
   private static final double ANCHOR_SIDE = 0.45;
   private static final double ANCHOR_BELOW_EYE_PER_SCALE = 0.4;
   private static final int STAGGER_TICKS = 40;
   private static final int STAGGER_AMP = 1;
   private static final double THROW_SPEED_BASE = 1.6;
   private static final double THROW_SPEED_PER_STAGE = 0.3;
   private static final float THROW_HIT_BASE = 8.0F;
   private static final float THROW_HIT_PER_STAGE = 2.0F;
   private static final float THROW_CRASH_BASE = 6.0F;
   private static final float THROW_CRASH_PER_STAGE = 1.5F;
   private static final double THROW_KNOCKBACK = 0.6;
   private static final double THROW_KNOCKUP = 0.25;
   private static final int THROW_TIMEOUT_TICKS = 60;
   private static final int THROW_MIN_FLIGHT = 3;
   private static final double GRAVITY = 0.08;
   private static final double DRAG = 0.98;
   private static final Map<UUID, SeizeControl.Clamber> CLAMBERS = new ConcurrentHashMap<>();
   private static final Map<UUID, SeizeControl.Seize> SEIZES = new ConcurrentHashMap<>();
   private static final Map<UUID, SeizeControl.Thrown> THROWN = new ConcurrentHashMap<>();

   private SeizeControl() {
   }

   public static boolean isClambering(ServerPlayer rider) {
      return CLAMBERS.containsKey(rider.getUUID());
   }

   public static boolean isSeizing(ServerPlayer holder) {
      return SEIZES.containsKey(holder.getUUID());
   }

   public static boolean isSeizedVictim(LivingEntity entity) {
      for (SeizeControl.Seize seize : SEIZES.values()) {
         if (seize.victim == entity) {
            return true;
         }
      }

      return false;
   }

   public static void beginClamber(ServerPlayer rider, LivingEntity victim) {
      HitchControl.detach(rider, false);
      CLAMBERS.put(rider.getUUID(), new SeizeControl.Clamber(victim));
      PacketDistributor.sendToPlayer(rider, new ForcedCameraPayload(65, victim.getId()), new CustomPacketPayload[0]);
      if (victim instanceof ServerPlayer sp && !Scaling.isSpeck(rider)) {
         sp.displayClientMessage(Component.translatable("awakened.scaler.clamber_warning"), true);
      }

      ServerLevel level = Dmg.serverLevel(rider);
      Fx.sound(level, rider.position(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.9F, 1.5F);
   }

   private static void tickClamber(ServerPlayer rider) {
      SeizeControl.Clamber clamber = CLAMBERS.get(rider.getUUID());
      if (clamber != null) {
         LivingEntity victim = clamber.victim;
         if (rider.isAlive()
            && victim.isAlive()
            && !victim.isRemoved()
            && victim.level() == rider.level()
            && !(Scaling.stage(PowerService.data(rider)).scale() > SizeStage.SMALL.scale() + 0.001F)) {
            rider.fallDistance = 0.0F;
            if (!clamber.running) {
               tickDash(rider, clamber, victim);
            } else {
               tickRun(rider, clamber, victim);
            }
         } else {
            endClamber(rider, false);
         }
      }
   }

   private static void tickDash(ServerPlayer rider, SeizeControl.Clamber clamber, LivingEntity victim) {
      if (++clamber.dashTicks > 20) {
         endClamber(rider, false);
      } else {
         double contact = 1.2 * (double)victim.getBbWidth() + (double)rider.getBbWidth() * 0.5;
         Vec3 toVictim = victim.position().add(0.0, (double)victim.getBbHeight() * 0.4, 0.0).subtract(rider.position());
         double dist = toVictim.length();
         if (dist <= contact) {
            clamber.running = true;
         } else {
            Vec3 step = toVictim.scale(Math.min(0.9, dist - contact * 0.5) / dist);
            pinTo(rider, rider.position().add(step));
         }
      }
   }

   private static void tickRun(ServerPlayer rider, SeizeControl.Clamber clamber, LivingEntity victim) {
      clamber.runTicks++;
      ServerLevel level = Dmg.serverLevel(rider);
      if (clamber.runTicks >= 30) {
         if (clamber.runTicks == 30) {
            MasterySystem.award(rider, MasteryReason.HIT, victim);
         }

         if (clamber.released) {
            dismount(rider, victim, level);
            return;
         }

         PowerData data = PowerService.data(rider);
         if (!data.infiniteStamina() && StaminaSystem.drainSustained(data, rider.level().getGameTime(), billed(0.4F)) <= 0.0F) {
            dismount(rider, victim, level);
            return;
         }

         if (clamber.runTicks % 20 == 0) {
            PacketDistributor.sendToPlayer(rider, new ForcedCameraPayload(40, victim.getId()), new CustomPacketPayload[0]);
         }
      }

      Vec3 point = pathPoint(victim, pingPong((float)clamber.runTicks / 30.0F));
      pinTo(rider, point);
      if (clamber.runTicks % 5 == 0) {
         SizeStage stage = Scaling.stage(PowerService.data(rider));
         float factor = stage == SizeStage.SPECK ? 0.6F : (stage == SizeStage.MITE ? 0.8F : 1.0F);
         DamageSource source = Dmg.by(rider, DamageTypes.PLAYER_ATTACK);
         Dmg.hurt(victim, level, source, MasteryScaling.scaledDamage(1.0F * factor, rider, "ClamberSeizeAbility"));
         if (Foes.defeatedBy(victim, rider)) {
            QuestSystem.feat(rider, "scaler_clamber_kill");
         }

         Fx.burst(level, ParticleTypes.CRIT, point, 5, 0.2, 0.1);
         Fx.burst(level, ParticleTypes.SWEEP_ATTACK, point, 1, 0.05, 0.0);
         Fx.sound(level, point, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.4F);
      }

      if (victim instanceof ServerPlayer sp && !Scaling.isSpeck(rider)) {
         boolean crouch = sp.isShiftKeyDown();
         if (crouch && !clamber.lastCrouch && ++clamber.crouchToggles >= 4) {
            endClamber(rider, false);
            Vec3 fling = rider.position().subtract(victim.position());
            fling = fling.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(fling.x, 0.0, fling.z).normalize();
            rider.setDeltaMovement(fling.scale(0.6).add(0.0, 0.4, 0.0));
            rider.hurtMarked = true;
            rider.displayClientMessage(Component.translatable("awakened.scaler.shaken_off"), true);
            return;
         }

         clamber.lastCrouch = crouch;
      }
   }

   private static void dismount(ServerPlayer rider, LivingEntity victim, ServerLevel level) {
      endClamber(rider, false);
      Vec3 back = bodyForward(victim.yBodyRot).scale(-0.45);
      rider.setDeltaMovement(back.x, 0.5, back.z);
      rider.hurtMarked = true;
      Fx.burst(level, ParticleTypes.CLOUD, rider.position(), 6, 0.25, 0.04);
      Fx.sound(level, rider.position(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.7F, 1.6F);
   }

   private static float pingPong(float t) {
      float cycle = t % 2.0F;
      return cycle <= 1.0F ? cycle : 2.0F - cycle;
   }

   public static void releaseClamberKey(ServerPlayer rider) {
      SeizeControl.Clamber clamber = CLAMBERS.get(rider.getUUID());
      if (clamber != null) {
         clamber.released = true;
      }
   }

   private static void endClamber(ServerPlayer rider, boolean silent) {
      if (CLAMBERS.remove(rider.getUUID()) != null && !silent) {
         rider.fallDistance = 0.0F;
         PacketDistributor.sendToPlayer(rider, new ForcedCameraPayload(0), new CustomPacketPayload[0]);
      }
   }

   private static Vec3 pathPoint(LivingEntity victim, float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      int segments = PATH.length - 1;
      float scaled = t * (float)segments;
      int i = Math.min(segments - 1, (int)scaled);
      float f = smooth(scaled - (float)i);
      double x = Mth.lerp((double)f, PATH[i][0], PATH[i + 1][0]) * (double)victim.getBbWidth();
      double y = Mth.lerp((double)f, PATH[i][1], PATH[i + 1][1]) * (double)victim.getBbHeight();
      double z = Mth.lerp((double)f, PATH[i][2], PATH[i + 1][2]) * (double)victim.getBbWidth();
      Vec3 forward = bodyForward(victim.yBodyRot);
      Vec3 right = bodyRight(victim.yBodyRot);
      return victim.position().add(right.scale(x)).add(0.0, y, 0.0).add(forward.scale(z));
   }

   private static float smooth(float f) {
      return f * f * (3.0F - 2.0F * f);
   }

   public static void beginSeize(ServerPlayer holder, LivingEntity victim) {
      boolean mob = victim instanceof Mob;
      if (mob) {
         suspend((Mob)victim, true);
      }

      int escape = escapeThreshold(Scaling.stage(PowerService.data(holder)));
      SeizeControl.Seize seize = new SeizeControl.Seize(victim, mob, escape);
      SEIZES.put(holder.getUUID(), seize);
      if (victim instanceof ServerPlayer victimPlayer) {
         seize.anchor = spawnGripAnchor(holder, victimPlayer);
      }

      PowerService.playAnimation(holder, "scaler_seize");
      RayEffects.syncRestraint(holder);
      if (victim instanceof ServerPlayer sp) {
         RayEffects.syncRestraint(sp);
         sp.displayClientMessage(Component.translatable("awakened.scaler.seized_hint"), true);
      }

      ServerLevel level = Dmg.serverLevel(holder);
      Fx.burst(level, ParticleTypes.CLOUD, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 8, (double)victim.getBbWidth() * 0.5, 0.05);
      Fx.sound(level, victim.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 0.7F);
   }

   private static int escapeThreshold(SizeStage stage) {
      return stage == SizeStage.COLOSSUS ? 25 : (stage == SizeStage.TITAN ? 14 : 8);
   }

   private static void tickSeize(ServerPlayer holder) {
      SeizeControl.Seize seize = SEIZES.get(holder.getUUID());
      if (seize != null) {
         LivingEntity victim = seize.victim;
         PowerData data = PowerService.data(holder);
         if (holder.isAlive()
            && victim.isAlive()
            && !victim.isRemoved()
            && victim.level() == holder.level()
            && !(Scaling.stage(data).scale() < SizeStage.BRUTE.scale())) {
            seize.holdTicks++;
            if (seize.holdTicks == 7) {
               PowerService.playAnimation(holder, "scaler_seize_hold");
            }

            if (!data.infiniteStamina() && StaminaSystem.drainSustained(data, holder.level().getGameTime(), billed(0.5F)) <= 0.0F) {
               drop(holder);
            } else {
               Vec3 hand = handAnchor(holder);
               if (seize.anchor != null) {
                  seize.anchor.setPos(hand.x, hand.y, hand.z);
                  seize.anchor.setDeltaMovement(Vec3.ZERO);
                  if (victim.getVehicle() != seize.anchor) {
                     forceRide(victim, seize.anchor);
                  }
               } else {
                  pinTo(victim, hand);
                  float yaw = holder.getYRot();
                  if (seize.holdTicks % 12 == 0) {
                     yaw += (victim.getRandom().nextFloat() - 0.5F) * 40.0F;
                     ServerLevel level = Dmg.serverLevel(holder);
                     Fx.sound(level, hand, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 0.5F, 1.3F);
                  }

                  victim.setYRot(yaw);
                  victim.setYBodyRot(yaw);
                  victim.setYHeadRot(yaw);
               }
            }
         } else {
            release(holder);
         }
      }
   }

   private static GripAnchorEntity spawnGripAnchor(ServerPlayer holder, ServerPlayer victim) {
      ServerLevel level = Dmg.serverLevel(holder);
      GripAnchorEntity anchor = new GripAnchorEntity(AwakenedEntities.GRIP_ANCHOR.get(), level);
      Vec3 hand = handAnchor(holder);
      anchor.setPos(hand.x, hand.y, hand.z);
      level.addFreshEntity(anchor);
      forceRide(victim, anchor);
      return anchor;
   }

   private static void forceRide(LivingEntity rider, GripAnchorEntity seat) {
      GripAnchorEntity.mount(rider, seat);
   }

   private static void detachAnchor(SeizeControl.Seize seize) {
      if (seize.anchor != null) {
         if (seize.victim.getVehicle() == seize.anchor) {
            seize.victim.stopRiding();
         }

         seize.anchor.discard();
         seize.anchor = null;
      }
   }

   static void pinTo(LivingEntity entity, Vec3 point) {
      entity.setDeltaMovement(Vec3.ZERO);
      entity.fallDistance = 0.0F;
      if (entity instanceof ServerPlayer sp) {
         sp.teleportTo(point.x, point.y, point.z);
      } else {
         entity.setPos(point.x, point.y, point.z);
      }
   }

   private static Vec3 handAnchor(ServerPlayer holder) {
      double width = (double)holder.getBbWidth();
      Vec3 forward = bodyForward(holder.getYRot());
      Vec3 right = bodyRight(holder.getYRot());
      double height = (double)holder.getEyeHeight() - 0.4 * (double)holder.getScale();
      return holder.position().add(forward.scale(width * 0.8)).add(right.scale(width * 0.45)).add(0.0, Math.max(0.2, height), 0.0);
   }

   public static void struggle(ServerPlayer victim) {
      for (Entry<UUID, SeizeControl.Seize> entry : SEIZES.entrySet()) {
         SeizeControl.Seize seize = entry.getValue();
         if (seize.victim == victim) {
            if (++seize.struggles < seize.escapeNeeded) {
               Fx.burst(Dmg.serverLevel(victim), ParticleTypes.CRIT, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 3, 0.3, 0.06);
               return;
            }

            ServerPlayer holder = victim.level().getServer() == null ? null : victim.level().getServer().getPlayerList().getPlayer(entry.getKey());
            if (holder != null) {
               release(holder);
               holder.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
               holder.displayClientMessage(Component.translatable("awakened.scaler.escaped"), true);
               Vec3 out = bodyForward(holder.getYRot());
               victim.setDeltaMovement(out.x * 0.4, 0.4, out.z * 0.4);
               victim.hurtMarked = true;
            } else {
               SEIZES.remove(entry.getKey());
               detachAnchor(seize);
               restoreVictim(seize.mob, victim);
               RayEffects.syncRestraint(victim);
            }

            victim.displayClientMessage(Component.translatable("awakened.scaler.broke_free"), true);
            return;
         }
      }
   }

   public static void drop(ServerPlayer holder) {
      SeizeControl.Seize seize = SEIZES.remove(holder.getUUID());
      if (seize != null) {
         detachAnchor(seize);
         LivingEntity victim = seize.victim;
         restoreVictim(seize.mob, victim);
         Vec3 spot = holder.position().add(bodyForward(holder.getYRot()).scale((double)holder.getBbWidth() * 0.9));
         if (victim.isAlive()) {
            pinTo(victim, spot);
         }

         finishRelease(holder, victim);
      }
   }

   private static void release(ServerPlayer holder) {
      SeizeControl.Seize seize = SEIZES.remove(holder.getUUID());
      if (seize != null) {
         detachAnchor(seize);
         restoreVictim(seize.mob, seize.victim);
         finishRelease(holder, seize.victim);
      }
   }

   private static void restoreVictim(boolean mob, LivingEntity victim) {
      if (mob && victim instanceof Mob m && victim.isAlive()) {
         suspend(m, false);
      }

      victim.setDeltaMovement(Vec3.ZERO);
      victim.fallDistance = 0.0F;
   }

   private static void finishRelease(ServerPlayer holder, LivingEntity victim) {
      PowerService.stopAnimation(holder);
      RayEffects.syncRestraint(holder);
      if (victim instanceof ServerPlayer sp) {
         RayEffects.syncRestraint(sp);
      }
   }

   public static boolean throwVictim(ServerPlayer holder) {
      SeizeControl.Seize seize = SEIZES.remove(holder.getUUID());
      if (seize == null) {
         return false;
      } else {
         detachAnchor(seize);
         SeizeControl.Thrown previous = THROWN.get(holder.getUUID());
         if (previous != null) {
            settleThrown(holder, previous);
         }

         LivingEntity victim = seize.victim;
         int offset = Scaling.stage(PowerService.data(holder)).ordinal() - SizeStage.NORMAL.ordinal();
         double speed = 1.6 + 0.3 * (double)offset;
         Vec3 velocity = holder.getLookAngle().scale(speed);
         victim.setDeltaMovement(velocity);
         victim.hurtMarked = true;
         victim.fallDistance = 0.0F;
         THROWN.put(
            holder.getUUID(),
            new SeizeControl.Thrown(
               victim,
               seize.mob,
               velocity,
               MasteryScaling.scaledDamage(8.0F + 2.0F * (float)offset, holder, "ClamberSeizeAbility"),
               MasteryScaling.scaledDamage(6.0F + 1.5F * (float)offset, holder, "ClamberSeizeAbility")
            )
         );
         PowerService.playAnimation(holder, "scaler_throw");
         RayEffects.syncRestraint(holder);
         if (victim instanceof ServerPlayer sp) {
            RayEffects.syncRestraint(sp);
         }

         ServerLevel level = Dmg.serverLevel(holder);
         Fx.sound(level, holder.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
         Fx.sound(level, holder.position(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.8F, 0.8F);
         return true;
      }
   }

   private static void tickThrown(ServerPlayer thrower) {
      SeizeControl.Thrown thrown = THROWN.get(thrower.getUUID());
      if (thrown != null) {
         LivingEntity victim = thrown.victim;
         if (victim.isAlive() && !victim.isRemoved() && victim.level() == thrower.level()) {
            thrown.ticks++;
            victim.fallDistance = 0.0F;
            ServerLevel level = Dmg.serverLevel(thrower);
            Vec3 stepVelocity;
            if (thrown.mob) {
               Vec3 v = thrown.velocity;
               stepVelocity = new Vec3(v.x * 0.98, (v.y - 0.08) * 0.98, v.z * 0.98);
               thrown.velocity = stepVelocity;
               victim.move(MoverType.SELF, stepVelocity);
            } else {
               stepVelocity = victim.getDeltaMovement();
            }

            Fx.burst(level, Fx.dust(6021288, 1.0F), victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 2, 0.15, 0.0);
            AABB swept = victim.getBoundingBox().expandTowards(stepVelocity).inflate(0.2);
            UUID throwerId = thrower.getUUID();
            List<LivingEntity> struck = level.getEntitiesOfClass(
               LivingEntity.class, swept, e -> e != victim && e.isAlive() && !e.getUUID().equals(throwerId) && Harm.allowed(thrower, e)
            );
            if (!struck.isEmpty()) {
               LivingEntity target = struck.get(0);
               DamageSource source = Dmg.by(thrower, DamageTypes.PLAYER_ATTACK);
               Dmg.hurt(victim, level, source, thrown.hitDamage);
               Dmg.hurt(target, level, source, thrown.hitDamage);
               Vec3 push = stepVelocity.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : stepVelocity.normalize();
               target.push(push.x * 0.6, 0.25, push.z * 0.6);
               target.hurtMarked = true;
               impactFx(level, victim.position());
               MasterySystem.award(thrower, MasteryReason.HIT, target);
               settleThrown(thrower, thrown);
            } else {
               boolean landed = thrown.ticks >= 3 && (victim.onGround() || victim.horizontalCollision || victim.verticalCollision || victim.isInWater());
               if (landed) {
                  Dmg.hurt(victim, level, Dmg.by(thrower, DamageTypes.PLAYER_ATTACK), thrown.crashDamage);
                  impactFx(level, victim.position());
                  settleThrown(thrower, thrown);
               } else if (thrown.ticks >= 60) {
                  settleThrown(thrower, thrown);
               }
            }
         } else {
            THROWN.remove(thrower.getUUID());
            if (thrown.mob && victim instanceof Mob m && m.isAlive()) {
               suspend(m, false);
            }
         }
      }
   }

   private static void impactFx(ServerLevel level, Vec3 pos) {
      Vfx.ring(level, pos.add(0.0, 0.15, 0.0), 1.6, 12582880, 8);
      Fx.burst(level, ParticleTypes.CLOUD, pos.add(0.0, 0.2, 0.0), 10, 0.6, 0.08);
      Fx.sound(level, pos, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 0.7F, 1.1F);
   }

   private static void settleThrown(ServerPlayer thrower, SeizeControl.Thrown thrown) {
      THROWN.remove(thrower.getUUID());
      if (thrown.mob && thrown.victim instanceof Mob m && m.isAlive()) {
         suspend(m, false);
      }

      thrown.victim.fallDistance = 0.0F;
   }

   public static void tick(ServerPlayer player) {
      tickClamber(player);
      tickSeize(player);
      tickThrown(player);
   }

   public static void clearPlayer(ServerPlayer player) {
      endClamber(player, false);
      release(player);
      SeizeControl.Thrown thrown = THROWN.remove(player.getUUID());
      if (thrown != null && thrown.mob && thrown.victim instanceof Mob m && m.isAlive()) {
         suspend(m, false);
      }
   }

   private static void suspend(Mob mob, boolean on) {
      mob.setNoAi(on);
      if (on) {
         mob.getNavigation().stop();
      }

      mob.setDeltaMovement(Vec3.ZERO);
   }

   private static Vec3 bodyForward(float yawDeg) {
      double rad = Math.toRadians((double)yawDeg);
      return new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
   }

   private static float billed(float rawCost) {
      return StaminaSystem.effectiveCost(rawCost, ClamberSeizeAbility.class.getSimpleName());
   }

   private static Vec3 bodyRight(float yawDeg) {
      double rad = Math.toRadians((double)yawDeg);
      return new Vec3(-Math.cos(rad), 0.0, -Math.sin(rad));
   }

   private static final class Clamber {
      private final LivingEntity victim;
      private boolean running;
      private boolean released;
      private int dashTicks;
      private int runTicks;
      private int crouchToggles;
      private boolean lastCrouch;

      private Clamber(LivingEntity victim) {
         this.victim = victim;
      }
   }

   private static final class Seize {
      private final LivingEntity victim;
      private final boolean mob;
      private final int escapeNeeded;
      private int holdTicks;
      private int struggles;
      private GripAnchorEntity anchor;

      private Seize(LivingEntity victim, boolean mob, int escapeNeeded) {
         this.victim = victim;
         this.mob = mob;
         this.escapeNeeded = escapeNeeded;
      }
   }

   private static final class Thrown {
      private final LivingEntity victim;
      private final boolean mob;
      private Vec3 velocity;
      private int ticks;
      private final float hitDamage;
      private final float crashDamage;

      private Thrown(LivingEntity victim, boolean mob, Vec3 velocity, float hitDamage, float crashDamage) {
         this.victim = victim;
         this.mob = mob;
         this.velocity = velocity;
         this.hitDamage = hitDamage;
         this.crashDamage = crashDamage;
      }
   }
}
