package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.entity.boss.AwakenedBossEntity;
import com.awakenedmod.awakened.content.entity.boss.BossDefenses;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.relic.RelicDrops;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public final class LeviathanEntity extends AwakenedBossEntity implements MultipartThreat {
   private static final int SALVO_INTERVAL = 140;
   private static final int BEAM_INTERVAL = 220;
   private static final int STOMP_INTERVAL = 90;
   private static final BossDefenses.Profile DEFENSE_PROFILE = new BossDefenses.Profile(40.0, 40.0F, 4.0, 100);
   private static final int HAZARD_COLOR = 16756768;
   private static final float CANNON_POOL_HIT_CAP = 30.0F;
   private static final double BEAM_LENGTH = 26.0;
   private static final double SCALE = 1.5;
   private static final double STOMP_RADIUS = 10.0;
   public static final int BEAM_SWEEP_TICKS = 36;
   public static final int STOMP_TELL_TICKS = 20;
   public static final int SALVO_RECOVER_TICKS = 12;
   public static final int STOMP_RECOVER_TICKS = 10;
   public static final byte PHASE_NONE = 0;
   public static final byte PHASE_SALVO = 1;
   public static final byte PHASE_BEAM = 2;
   public static final byte PHASE_STOMP = 3;
   public static final byte PHASE_DEATH = 4;
   private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Float> DATA_PHASE_PROGRESS = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Byte> DATA_CANNON_R = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> DATA_CANNON_L = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.BYTE);
   private final List<SiegeHazards.Zone> hazards = new ArrayList<>();
   private final BossPart<LeviathanEntity>[] parts = new BossPart[]{new BossPart(this, "cannon_r", 2.1F, 3.9F), new BossPart(this, "cannon_l", 2.1F, 3.9F)};
   private final float[] cannonHp = new float[2];
   private final UUID[] cannonLastHurtBy = new UUID[2];
   private final int[] cookOffTicks = new int[2];
   private final int[] cannonNextHurtTick = new int[2];
   private final Set<UUID> cannonHintShown = new HashSet<>();
   private int cannonsDestroyed;
   private int salvoCooldown = 60;
   private int beamCooldown = 220;
   private int stompCooldown = 90;
   private int salvoWindup = 0;
   private int salvoWindupTotal = salvoWindupTicks(1);
   private int salvoRecover = 0;
   private int beamWindup = 0;
   private int beamWindupTotal = beamWindupTicks(1);
   private int beamSweep = 0;
   private int beamSweepTotal = 36;
   private double beamArc = Math.toRadians(70.0);
   private double beamAngle = 0.0;
   private double beamStartAngle = 0.0;
   private int stompTell = 0;
   private int stompRecover = 0;
   private int roarTicks = 0;

   public static int salvoWindupTicks(int phase) {
      return phase <= 1 ? 32 : (phase == 2 ? 27 : 22);
   }

   public static int beamWindupTicks(int phase) {
      return phase <= 1 ? 28 : (phase == 2 ? 24 : 20);
   }

   public static int beamSweepTicks(int aliveCannons) {
      return aliveCannons >= 2 ? 36 : 30;
   }

   public static double beamArcDegrees(int aliveCannons) {
      return aliveCannons >= 2 ? 70.0 : 50.0;
   }

   public LeviathanEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level, Component.translatable("entity.awakened.leviathan"), BossBarColor.RED, BossBarOverlay.NOTCHED_12);
      this.xpReward = 80;
      this.setPersistenceRequired();
      this.setId(this.getId());
      float hp = (float)((Double)Config.LEVIATHAN_CANNON_HP.get()).doubleValue();
      this.cannonHp[0] = hp;
      this.cannonHp[1] = hp;
   }

   @Override
   protected BossDefenses.Profile defenseProfile() {
      return DEFENSE_PROFILE;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 600.0)
         .add(Attributes.ATTACK_DAMAGE, 20.0)
         .add(Attributes.MOVEMENT_SPEED, 0.18)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ARMOR, 14.0)
         .add(Attributes.FOLLOW_RANGE, 48.0);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_PHASE, (byte)0);
      builder.define(DATA_PHASE_PROGRESS, 0.0F);
      builder.define(DATA_CANNON_R, (byte)0);
      builder.define(DATA_CANNON_L, (byte)0);
   }

   public byte getPhase() {
      return (Byte)this.entityData.get(DATA_PHASE);
   }

   private void setPhase(byte phase) {
      this.entityData.set(DATA_PHASE, phase);
   }

   public float getPhaseProgress() {
      return (Float)this.entityData.get(DATA_PHASE_PROGRESS);
   }

   private void setPhaseProgress(float progress) {
      this.entityData.set(DATA_PHASE_PROGRESS, progress);
   }

   public byte getCannonState(int side) {
      return (Byte)this.entityData.get(side == 0 ? DATA_CANNON_R : DATA_CANNON_L);
   }

   private void setCannonState(int side, byte state) {
      this.entityData.set(side == 0 ? DATA_CANNON_R : DATA_CANNON_L, state);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.9, true));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public int getMaxHeadYRot() {
      return 30;
   }

   public int getHeadRotSpeed() {
      return 4;
   }

   public void setTarget(LivingEntity target) {
      LivingEntity prev = this.getTarget();
      super.setTarget(target);
      if (target != null && prev == null && this.level() instanceof ServerLevel server) {
         Fx.scanLock(server, this.position().add(0.0, (double)this.getBbHeight() * 0.85, 0.0), SoundEvents.BEACON_ACTIVATE, 2.0F, 0.6F);
      }
   }

   public boolean isMultipartEntity() {
      return true;
   }

   public PartEntity<?>[] getParts() {
      return this.parts;
   }

   public void setId(int id) {
      super.setId(id);
      if (this.parts != null) {
         for (int i = 0; i < this.parts.length; i++) {
            this.parts[i].setId(id + i + 1);
         }
      }
   }

   public void tick() {
      super.tick();

      for (int side = 0; side < 2; side++) {
         BossPart<LeviathanEntity> part = this.parts[side];
         part.setOldPosAndRot();
         Vec3 socket = this.cannonSocket(side);
         part.setPos(socket.x, socket.y - (double)part.getBbHeight() * 0.5, socket.z);
      }
   }

   @Override
   public boolean hurtPart(BossPart<?> part, ServerLevel level, DamageSource source, float amount) {
      if (!this.isAlive()) {
         return false;
      } else if (this.isInvulnerableTo(source)) {
         return false;
      } else {
         if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if ((Boolean)Config.BOSS_RANGE_IMMUNITY.get() && BossDefenses.isSnipe(this, source, this.defenseProfile().sniperRange())) {
               this.deflectWithHint(level, source.getEntity());
               return false;
            }

            if ((Boolean)Config.BOSS_DAMAGE_CAP.get()) {
               amount = Math.min(amount, this.defenseProfile().perHitCap());
            }
         }

         int side = "cannon_r".equals(part.partName()) ? 0 : 1;
         if (this.getCannonState(side) >= 3) {
            return this.hurtSelf(level, source, amount);
         } else if (this.tickCount < this.cannonNextHurtTick[side]) {
            return false;
         } else {
            this.cannonNextHurtTick[side] = this.tickCount + this.invulnerableTime;
            this.cannonHp[side] = this.cannonHp[side] - Math.min(amount, 30.0F);
            if (source.getEntity() instanceof ServerPlayer player) {
               this.cannonLastHurtBy[side] = player.getUUID();
               if (this.cannonHintShown.add(player.getUUID())) {
                  player.displayClientMessage(Component.translatable("entity.awakened.leviathan.cannon_hit").withStyle(ChatFormatting.YELLOW), true);
               }
            }

            Vec3 hit = part.position().add(0.0, (double)part.getBbHeight() * 0.5, 0.0);
            Fx.sound(level, hit, SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 1.3F, 1.35F);
            Fx.sound(level, hit, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.7F, 1.5F);
            Fx.burst(level, ParticleTypes.ELECTRIC_SPARK, hit, 12, 0.4, 0.12);
            Fx.burst(level, ParticleTypes.CRIT, hit, 8, 0.5, 0.1);
            Fx.burst(level, ParticleTypes.LAVA, hit, 3, 0.2, 0.05);
            Vfx.ring(level, this.cannonMuzzle(side), 0.9, 16756768, 4);
            this.maybeAdvanceWearTier(level, side);
            if (this.cannonHp[side] <= 0.0F) {
               this.destroyCannon(level, side);
            }

            return this.hurtSelf(level, source, amount * (float)((Double)Config.LEVIATHAN_CANNON_BLEED.get()).doubleValue());
         }
      }
   }

   private boolean hurtSelf(ServerLevel level, DamageSource source, float amount) {
      return this.hurt(source, amount);
   }

   private float cannonMaxHp() {
      return (float)((Double)Config.LEVIATHAN_CANNON_HP.get()).doubleValue();
   }

   private int aliveCannons() {
      return (this.getCannonState(0) < 3 ? 1 : 0) + (this.getCannonState(1) < 3 ? 1 : 0);
   }

   private void maybeAdvanceWearTier(ServerLevel level, int side) {
      if (!(this.cannonHp[side] <= 0.0F)) {
         float frac = this.cannonHp[side] / this.cannonMaxHp();
         byte tier = (byte)(frac > 0.66F ? 0 : (frac > 0.33F ? 1 : 2));
         if (tier > this.getCannonState(side)) {
            this.setCannonState(side, tier);
            Fx.sound(level, this.cannonSocket(side), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 0.7F);
            Fx.burst(level, ParticleTypes.LARGE_SMOKE, this.cannonMuzzle(side), 12, 0.4, 0.04);
         }
      }
   }

   private void destroyCannon(ServerLevel level, int side) {
      this.setCannonState(side, (byte)3);
      this.cannonHp[side] = 0.0F;
      this.cookOffTicks[side] = 16;
      Vec3 muzzle = this.cannonMuzzle(side);
      Vfx.sphere(level, muzzle, 2.2, 16756768, 12);
      Fx.burst(level, ParticleTypes.EXPLOSION_EMITTER, muzzle, 2, 0.3, 0.0);
      Fx.burst(level, ParticleTypes.LAVA, muzzle, 20, 0.8, 0.1);
      Fx.sound(level, muzzle, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F, 0.6F);
      Fx.shake(level, muzzle, 24.0, 0.45F, 14, 20);
      Fx.sound(level, this.position(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.6F, 0.9F);
      this.salvoCooldown += 60;
      this.cannonsDestroyed++;
      ServerPlayer breaker = this.cannonLastHurtBy[side] == null ? null : level.getServer().getPlayerList().getPlayer(this.cannonLastHurtBy[side]);
      if (breaker != null) {
         MasterySystem.grant(breaker, 40.0);
         breaker.displayClientMessage(Component.translatable("entity.awakened.leviathan.cannon_destroyed").withStyle(ChatFormatting.GOLD), true);
      }

      if (this.aliveCannons() == 0) {
         this.actionbarNear(level, 48.0, Component.translatable("entity.awakened.leviathan.disarmed").withStyle(ChatFormatting.GOLD));
      }
   }

   private void tickCannons(ServerLevel level) {
      for (int side = 0; side < 2; side++) {
         Vec3 muzzle = this.cannonMuzzle(side);
         byte state = this.getCannonState(side);
         if (state < 3 && this.tickCount % 16 == 0) {
            Fx.burst(level, Fx.HAZARD_AMBER, muzzle, 2, 0.15, 0.01);
         }

         if (state == 1 && this.tickCount % 6 == 0) {
            Fx.burst(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, muzzle.add(0.0, 0.6, 0.0), 5, 0.3, 0.06);
         }

         if (state == 2) {
            if (this.tickCount % 4 == 0) {
               Fx.burst(level, ParticleTypes.LARGE_SMOKE, muzzle.add(0.0, 0.8, 0.0), 7, 0.4, 0.08);
               Fx.burst(level, ParticleTypes.LAVA, muzzle, 2, 0.15, 0.03);
            }

            if (this.tickCount % 24 == 0) {
               Fx.burst(level, ParticleTypes.FLAME, muzzle, 4, 0.25, 0.04);
               Fx.sound(level, muzzle, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.7F, 1.5F);
            }
         }

         if (this.cookOffTicks[side] > 0) {
            int t = --this.cookOffTicks[side];
            if (t == 11 || t == 7 || t == 2) {
               Fx.sound(level, muzzle, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.4F, 0.7F);
               Fx.burst(level, ParticleTypes.FLAME, muzzle, 6, 0.3, 0.05);
            }
         }
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      if (!this.isDeadOrDying()) {
         SiegeHazards.tick(level, this, this.hazards, (long)this.tickCount);
         this.tickCannons(level);
         LivingEntity target = this.getTarget();
         if (target == null) {
            if (this.getPhase() != 0) {
               this.setPhase((byte)0);
            }

            this.clearAttackTimers();
         } else if (this.roarTicks > 0) {
            this.roarTicks--;
            if (this.roarTicks == 24) {
               this.cannonSalute(level, 0);
            }

            if (this.roarTicks == 18) {
               this.cannonSalute(level, 1);
            }

            if (this.roarTicks == 8) {
               Vfx.dome(level, this.position(), 5.0, this.escalation >= 3 ? 11563263 : 16756768, 16);
            }
         } else {
            double dsq = this.distanceToSqr(target);
            if (target.getY() - this.getY() >= DEFENSE_PROFILE.towerHeight() && this.salvoCooldown > 0) {
               this.salvoCooldown -= 2;
            }

            if (this.stompRecover > 0) {
               if (--this.stompRecover <= 0) {
                  if (this.getPhase() == 3) {
                     this.setPhase((byte)0);
                  }

                  this.setPhaseProgress(0.0F);
                  this.stompCooldown = this.stompInterval();
               } else {
                  this.setPhaseProgress((float)(20 + (10 - this.stompRecover)) / 30.0F);
               }
            } else if (this.stompTell > 0) {
               this.stompTell--;
               this.setPhaseProgress((float)(20 - this.stompTell) / 30.0F);
               Telegraphs.groundMarker(level, this.position(), 10.0, 16777215, this.stompTell, 20);
               if (this.stompTell == 3) {
                  Telegraphs.commitCue(level, this.position(), SoundEvents.IRON_GOLEM_ATTACK, 1.3F);
                  Telegraphs.dangerClose(level, this.position(), 10.0);
               }

               if (this.stompTell <= 0) {
                  ThreatMoves.shockwave(level, this, 10.0, 7.0F + (float)this.escalation, 0.05F, 80, 1.4, 0.5);
                  this.stompRecover = 10;
               }
            } else if (this.getPhase() == 0 && --this.stompCooldown <= 0 && dsq <= 100.0) {
               this.setPhase((byte)3);
               this.stompTell = 20;
               Fx.sound(level, this.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 0.5F);
            }

            if (this.beamSweep > 0) {
               if (this.aliveCannons() == 0) {
                  this.beamSweep = 0;
                  if (this.getPhase() == 2) {
                     this.setPhase((byte)0);
                  }

                  this.setPhaseProgress(0.0F);
               } else {
                  this.sweepBeam(level);
                  this.beamSweep--;
                  this.setPhaseProgress(
                     (float)(this.beamWindupTotal + (this.beamSweepTotal - this.beamSweep)) / (float)(this.beamWindupTotal + this.beamSweepTotal)
                  );
                  if (this.beamSweep <= 0) {
                     if (this.getPhase() == 2) {
                        this.setPhase((byte)0);
                     }

                     this.setPhaseProgress(0.0F);
                  }
               }
            } else if (this.beamWindup > 0) {
               if (--this.beamWindup <= 0) {
                  this.startSweep(level);
               } else {
                  this.setPhaseProgress((float)(this.beamWindupTotal - this.beamWindup) / (float)(this.beamWindupTotal + this.beamSweepTotal));

                  for (int side = 0; side < 2; side++) {
                     if (this.getCannonState(side) < 3) {
                        Fx.burst(level, Fx.OPTIC_RED, this.cannonMuzzle(side), 6, 0.2, 0.02);
                     }
                  }

                  Vec3 dirStart = new Vec3(Math.cos(this.beamStartAngle), 0.0, Math.sin(this.beamStartAngle));
                  Vec3 beamEnd = this.beamOrigin().add(dirStart.scale(26.0)).add(0.0, 0.6, 0.0);
                  if (this.beamWindup <= this.beamWindupTotal - 4) {
                     Vfx.beam(level, this.beamOrigin(), beamEnd, 0.35, 16726832, 3);
                  }

                  if (this.beamWindup <= 10) {
                     Telegraphs.aimTracer(level, this.beamOrigin(), beamEnd, 16726832);
                  }

                  if (this.beamWindup == 2) {
                     Telegraphs.commitCue(level, this.position(), SoundEvents.WARDEN_SONIC_CHARGE, 1.4F);
                     Telegraphs.dangerCloseCone(level, this.beamOrigin(), dirStart, 26.0, 10.0);
                  }
               }
            } else if (this.getPhase() == 0
               && --this.beamCooldown <= 0
               && dsq <= 900.0
               && target.getY() - this.getY() < DEFENSE_PROFILE.towerHeight()
               && this.aliveCannons() > 0) {
               this.setPhase((byte)2);
               this.beamWindupTotal = beamWindupTicks(this.escalation);
               this.beamWindup = this.beamWindupTotal;
               this.beamCooldown = this.beamInterval();
               this.beamSweepTotal = beamSweepTicks(this.aliveCannons());
               this.beamArc = Math.toRadians(beamArcDegrees(this.aliveCannons()));
               Vec3 toTarget = target.position().subtract(this.position());
               this.beamStartAngle = Math.atan2(toTarget.z, toTarget.x) - this.beamArc / 2.0;
               Fx.sound(level, this.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.4F, 0.5F);
            }

            if (this.salvoRecover > 0) {
               if (--this.salvoRecover <= 0) {
                  if (this.getPhase() == 1) {
                     this.setPhase((byte)0);
                  }

                  this.setPhaseProgress(0.0F);
                  this.salvoCooldown = Math.max(this.salvoCooldown, this.salvoInterval());
               } else {
                  this.setPhaseProgress((float)(this.salvoWindupTotal + (12 - this.salvoRecover)) / (float)(this.salvoWindupTotal + 12));
               }
            } else if (this.salvoWindup > 0) {
               this.salvoWindup--;
               this.setPhaseProgress((float)(this.salvoWindupTotal - this.salvoWindup) / (float)(this.salvoWindupTotal + 12));
               if (this.salvoWindup <= 0) {
                  this.fireSalvo(level, target);
                  this.salvoRecover = 12;
               } else {
                  if (this.salvoWindup == 3) {
                     Telegraphs.commitCue(level, this.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8F);
                  }

                  if (this.salvoWindup % 2 == 0) {
                     int count = 6 + (this.salvoWindupTotal - this.salvoWindup) / 2;
                     double swell = 0.8 + 1.4 * ((double)(this.salvoWindupTotal - this.salvoWindup) / (double)this.salvoWindupTotal);

                     for (int sidex = 0; sidex < 2; sidex++) {
                        if (this.getCannonState(sidex) < 3) {
                           Vec3 muzzle = this.cannonMuzzle(sidex);
                           Telegraphs.chargeMotes(level, Fx.HAZARD_AMBER, muzzle, this.salvoWindupTotal - this.salvoWindup, this.salvoWindupTotal, 1.6);
                           Fx.burst(level, Fx.HAZARD_AMBER, muzzle, count, 0.25, 0.02);
                           Vfx.ring(level, muzzle, swell, 16756768, 3);
                        }
                     }
                  }
               }
            } else if (this.getPhase() == 0 && --this.salvoCooldown <= 0 && dsq <= 1296.0) {
               if (this.aliveCannons() > 0) {
                  this.setPhase((byte)1);
                  this.salvoWindupTotal = salvoWindupTicks(this.escalation);
                  this.salvoWindup = this.salvoWindupTotal;
                  Fx.sound(level, this.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5F, 0.4F);
               } else {
                  this.coreVent(level, target);
                  this.salvoCooldown = this.salvoInterval() + 40;
               }
            }
         }
      }
   }

   private void clearAttackTimers() {
      this.salvoWindup = 0;
      this.salvoRecover = 0;
      this.beamWindup = 0;
      this.beamSweep = 0;
      this.stompTell = 0;
      this.stompRecover = 0;
      this.setPhaseProgress(0.0F);
   }

   @Override
   protected void onTargetHidden(ServerLevel level, LivingEntity target) {
      this.salvoCooldown = 0;
   }

   private void fireSalvo(ServerLevel level, LivingEntity target) {
      int alive = this.aliveCannons();
      if (alive > 0) {
         int shells = 2 + this.escalation;
         if (alive == 1) {
            shells = (shells + 1) / 2;
         }

         int survivor = this.getCannonState(0) < 3 ? 0 : 1;
         Vec3 base = target.position();

         for (int i = 0; i < shells; i++) {
            int side = alive >= 2 ? i % 2 : survivor;
            double ang = (Math.PI * 2) / (double)shells * (double)i;
            double spread = 2.0 + this.getRandom().nextDouble() * 3.0;
            if (this.getCannonState(side) == 2) {
               spread *= 2.0;
            }

            Vec3 land = i == 0 && this.getCannonState(side) < 2 ? base : base.add(Math.cos(ang) * spread, 0.0, Math.sin(ang) * spread);
            Vfx.arc(level, this.cannonMuzzle(side), land.add(0.0, 0.5, 0.0), 0.4, 16756768, 18);
            this.hazards.add(new SiegeHazards.Zone(land, 2.6, 4.0F + (float)this.escalation, 24, 60 + this.escalation * 25));
         }

         Fx.sound(level, this.position(), (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.6F);
      }
   }

   private void coreVent(ServerLevel level, LivingEntity target) {
      Vec3 core = this.position().add(0.0, (double)this.getBbHeight() * 0.45, 0.0);
      Fx.burst(level, Fx.HAZARD_AMBER, core, 14, 0.6, 0.08);
      Fx.burst(level, ParticleTypes.LAVA, core, 6, 0.5, 0.08);
      Fx.sound(level, this.position(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.3F, 0.5F);
      this.hazards.add(new SiegeHazards.Zone(this.position(), 2.6, 3.0F, 28, 50));

      for (int i = 0; i < 2; i++) {
         Vec3 land = target.position().add((this.getRandom().nextDouble() - 0.5) * 8.0, 0.0, (this.getRandom().nextDouble() - 0.5) * 8.0);
         this.hazards.add(new SiegeHazards.Zone(land, 2.2, 3.0F, 28, 50));
      }
   }

   private void startSweep(ServerLevel level) {
      this.beamAngle = this.beamStartAngle;
      this.beamSweep = this.beamSweepTotal;
      Fx.sound(level, this.position(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.3F, 0.8F);
   }

   private void sweepBeam(ServerLevel level) {
      double progress = 1.0 - (double)this.beamSweep / (double)this.beamSweepTotal;
      this.beamAngle = this.beamStartAngle + this.beamArc * progress;
      Vec3 muzzle = this.beamOrigin();
      Vec3 dir = new Vec3(Math.cos(this.beamAngle), 0.0, Math.sin(this.beamAngle));
      Vec3 end = this.position().add(dir.scale(26.0)).add(0.0, 0.6, 0.0);
      Vfx.beam(level, muzzle, end, 0.8, 16726832, 3);
      Fx.burst(level, ParticleTypes.FLAME, end, 6, 0.5, 0.02);
      float damage = this.aliveCannons() >= 2 ? 6.0F + (float)this.escalation : 4.0F + 0.5F * (float)this.escalation;
      AABB area = this.getBoundingBox().inflate(26.0);

      for (Player p : level.getEntitiesOfClass(Player.class, area, px -> px.isAlive() && !px.isCreative() && !px.isSpectator())) {
         Vec3 flat = new Vec3(p.getX() - this.getX(), 0.0, p.getZ() - this.getZ());
         double d = flat.length();
         if (!(d < 1.5) && !(d > 26.0)) {
            double pAng = Math.atan2(flat.z, flat.x);
            double diff = Math.abs(Math.atan2(Math.sin(pAng - this.beamAngle), Math.cos(pAng - this.beamAngle)));
            if (diff < Math.toRadians(7.0)) {
               Dmg.hurt(p, level, this.damageSources().mobAttack(this), damage);
               p.setRemainingFireTicks(30);
            }
         }
      }
   }

   private Vec3 beamOrigin() {
      if (this.getCannonState(0) >= 3) {
         return this.cannonMuzzle(1);
      } else {
         return this.getCannonState(1) >= 3 ? this.cannonMuzzle(0) : this.cannonMuzzle(0).add(this.cannonMuzzle(1)).scale(0.5);
      }
   }

   private Vec3 cannonMuzzle(int side) {
      return this.cannonPoint(side, 0.82, 1.2, -0.6);
   }

   private Vec3 cannonSocket(int side) {
      return this.cannonPoint(side, 0.7, 1.25, -0.9);
   }

   private Vec3 cannonPoint(int side, double heightFrac, double rightOffset, double fwdOffset) {
      float yaw = (float)Math.toRadians((double)this.yBodyRot);
      double s = side == 0 ? -1.0 : 1.0;
      Vec3 right = new Vec3(Math.cos((double)yaw), 0.0, Math.sin((double)yaw));
      Vec3 fwd = new Vec3(-Math.sin((double)yaw), 0.0, Math.cos((double)yaw));
      return this.position().add(0.0, (double)this.getBbHeight() * heightFrac, 0.0).add(right.scale(s * rightOffset * 1.5)).add(fwd.scale(fwdOffset * 1.5));
   }

   private int salvoInterval() {
      return Math.max(70, 140 - (this.escalation - 1) * 35);
   }

   private int beamInterval() {
      return Math.max(120, 220 - (this.escalation - 1) * 50);
   }

   private int stompInterval() {
      return Math.max(50, 90 - (this.escalation - 1) * 20);
   }

   @Override
   protected void onEscalate(ServerLevel level, int newTier) {
      Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0);
      Fx.burst(level, Fx.OPTIC_RED, mid, 50, 1.6, 0.18);
      Fx.burst(level, ParticleTypes.SONIC_BOOM, mid, 1, 0.0, 0.0);
      Fx.sound(level, this.position(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.8F, 0.5F);
      this.roarTicks = 30;
      this.clearAttackTimers();
      this.setPhase((byte)0);
      this.beamCooldown = 50;
      this.salvoCooldown = 50;
      boolean desperate = newTier >= 3;
      this.bossEvent.setName(Component.translatable(desperate ? "entity.awakened.leviathan.overload" : "entity.awakened.leviathan.enraged"));
      this.bossEvent.setColor(desperate ? BossBarColor.PURPLE : BossBarColor.YELLOW);
      if (desperate) {
         this.bossEvent.setDarkenScreen(true);
      }
   }

   private void cannonSalute(ServerLevel level, int side) {
      if (this.getCannonState(side) < 3) {
         Vec3 muzzle = this.cannonMuzzle(side);
         Vfx.arc(level, muzzle, muzzle.add(0.0, 10.0, 0.0), 0.3, 16756768, 14);
         Fx.burst(level, Fx.HAZARD_AMBER, muzzle, 12, 0.3, 0.1);
         Fx.sound(level, muzzle, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 1.6F);
      }
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.WARDEN_HEARTBEAT;
   }

   protected SoundEvent getHurtSound(DamageSource damageSource) {
      return SoundEvents.NETHERITE_BLOCK_HIT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.WARDEN_DEATH;
   }

   @Override
   protected void onDeathSequenceStart() {
      this.setPhase((byte)4);
      this.bossEvent.setProgress(0.0F);
      this.bossEvent.setDarkenScreen(false);
      this.roarTicks = 0;
      this.clearAttackTimers();
   }

   @Override
   protected int deathDuration() {
      return 140;
   }

   @Override
   protected void deathBeat(ServerLevel level, int t) {
      if (t == 1) {
         Fx.sound(level, this.position(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0F, 0.4F);
      }

      if (t == 20) {
         this.deathCookOff(level, 0, 1.6, 1.4F, 1.1F, 0.2F);
      }

      if (t == 45) {
         this.deathCookOff(level, 1, 2.0, 1.6F, 0.9F, 0.25F);
      }

      if (t >= 20 && t <= 110 && t % 9 == 0) {
         Vec3 pos = this.randHullPos();
         Fx.burst(level, ParticleTypes.LAVA, pos, 4, 0.3, 0.1);
         Fx.burst(level, Fx.HAZARD_AMBER, pos, 6, 0.3, 0.05);
         Fx.sound(level, pos, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.5F);
      }

      if (t == 70) {
         Fx.sound(level, this.position(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.6F, 0.3F);
         Fx.disk(level, Fx.HAZARD_AMBER, this.position().add(0.0, 0.2, 0.0), 5.0);
         Vfx.dustDome(level, this.position(), 5.0, 20);
         Fx.shake(level, this.position(), 28.0, 0.3F, 0, 18);
      }

      if (t >= 70 && t <= 110) {
         if (t % 4 == 0) {
            Fx.burst(level, ParticleTypes.CLOUD, this.randHullPos(), 2, 0.2, 0.04);
         }

         if (t % 20 == 0) {
            Fx.sound(level, this.position(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 0.7F);
         }
      }

      if (t == 110) {
         Vfx.mushroom(level, this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0), 4.0, 30);
         ThreatMoves.shockwave(level, this, 8.0, 0.0F, 0.9, 0.35);
         Fx.sound(level, this.position(), (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.5F);
         Fx.sound(level, this.position(), SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 1.2F, 0.4F);
         Fx.shake(level, this.position(), 40.0, 0.5F, 4, 40);
      }

      if (t > 110 && t % 3 == 0) {
         Fx.burst(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.position().add(0.0, (double)this.getBbHeight() * 0.7, 0.0), 3, 1.0, 0.04);
      }
   }

   private void deathCookOff(ServerLevel level, int side, double sphereRadius, float volume, float pitch, float shake) {
      Vec3 muzzle = this.cannonMuzzle(side);
      Vfx.sphere(level, muzzle, sphereRadius, 16756768, 12, 1.4F);
      Fx.burst(level, ParticleTypes.EXPLOSION, muzzle, 6, 0.4, 0.05);
      Fx.sound(level, muzzle, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, volume, pitch);
      Fx.shake(level, this.position(), 24.0, shake, 0, 14);
   }

   private Vec3 randHullPos() {
      return this.position()
         .add(
            (this.getRandom().nextDouble() - 0.5) * (double)this.getBbWidth(),
            this.getRandom().nextDouble() * (double)this.getBbHeight(),
            (this.getRandom().nextDouble() - 0.5) * (double)this.getBbWidth()
         );
   }

   @Override
   protected void dropCinematicLoot(ServerLevel level) {
      bless(RelicDrops.dropSignature(level, this.position().add(0.0, 1.0, 0.0), "tidecaller_pearl"));
      bless(this.spawnAtFeet(level, new ItemStack(Items.ECHO_SHARD, 2 + this.cannonsDestroyed + this.getRandom().nextInt(3))));
      bless(this.spawnAtFeet(level, new ItemStack((ItemLike)AwakenedItems.CORDON_CORE.get())));
   }

   private void actionbarNear(ServerLevel level, double radius, Component message) {
      for (ServerPlayer player : level.getPlayers(p -> p.position().distanceToSqr(this.position()) <= radius * radius)) {
         player.displayClientMessage(message, true);
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("CannonHpR", this.cannonHp[0]);
      tag.putFloat("CannonHpL", this.cannonHp[1]);
      tag.putInt("CannonsDestroyed", this.cannonsDestroyed);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("CannonHpR")) {
         this.cannonHp[0] = tag.getFloat("CannonHpR");
         this.cannonHp[1] = tag.getFloat("CannonHpL");
         this.cannonsDestroyed = tag.getInt("CannonsDestroyed");
      }

      this.refreshCannonStates();
   }

   private void refreshCannonStates() {
      for (int side = 0; side < 2; side++) {
         float frac = this.cannonHp[side] / this.cannonMaxHp();
         byte state = (byte)(this.cannonHp[side] <= 0.0F ? 3 : (frac > 0.66F ? 0 : (frac > 0.33F ? 1 : 2)));
         this.setCannonState(side, state);
      }
   }

   @Override
   public void onCreditedKill(ServerPlayer player) {
      AwakenedAttachments.progress(player).bossDefeated();
      PowerService.syncProgress(player);
      QuestSystem.onBossDefeated(player);
   }

   @Override
   public double bonusMastery() {
      return 200.0;
   }

   @Override
   public double bonusXp() {
      return 700.0;
   }
}
