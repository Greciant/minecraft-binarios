package com.awakenedmod.awakened.content.entity.duelist;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.entity.ThreatMoves;
import com.awakenedmod.awakened.content.entity.ai.GatedGoal;
import com.awakenedmod.awakened.content.entity.boss.AwakenedBossEntity;
import com.awakenedmod.awakened.content.entity.boss.BossDefenses;
import com.awakenedmod.awakened.content.entity.boss.MoveScheduler;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.AwakenedAdvancements;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.relic.RelicDrops;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class VarkosEntity extends AwakenedBossEntity {
   private static final BossDefenses.Profile DEFENSE_PROFILE = new BossDefenses.Profile(14.0, 18.0F, 4.0, 60);
   private static final int WEAPON_COOLDOWN = 40;
   private static final int NEUTRAL_GAP_P1 = 20;
   private static final int NEUTRAL_GAP_P2 = 14;
   private static final int NEUTRAL_GAP_P3 = 10;
   private static final int INTENT_TIMEOUT = 80;
   private static final int INTENT_COOLDOWN_HORIZON = 30;
   private static final double TRAVEL_DISCOUNT_RANGE = 14.0;
   private static final double TRAVEL_DISCOUNT_FLOOR = 0.1;
   private static final double REPEAT_PENALTY = 0.6;
   private static final double THIRD_REPEAT_PENALTY = 0.2;
   private static final double HAMMER_LONG_RANGE = 18.0;
   private static final int FUTILITY_WHIFFS = 4;
   private static final double POST_CLICK_LONG_HAMMER_FACTOR = 0.25;
   private static final int CLICK_BEAT_TICKS = 8;
   private static final double REACTIVE_BOOST = 20.0;
   private static final double HUG_RADIUS = 4.0;
   private static final int HUG_TRIGGER_TICKS = 60;
   private static final int HUG_ANSWER_GAP = 6;
   private static final int FLEE_WINDOW = 20;
   private static final double FLEE_GAIN = 2.5;
   private static final int COMBO_COOLDOWN = 200;
   private static final int COMBO_GAP = 8;
   private static final double COMBO_CHANCE = 0.5;
   private static final double DAGGER_CLOSE_RANGE = 7.0;
   private static final double DAGGER_CLOSE_BOOST = 3.0;
   private static final double WHIFF_RADIUS = 4.5;
   private static final int WHIFF_ANSWER_GAP = 6;
   private static final EntityDataAccessor<Byte> DATA_WEAPON = SynchedEntityData.defineId(VarkosEntity.class, EntityDataSerializers.BYTE);
   private final MoveScheduler<VarkosEntity> scheduler = new MoveScheduler<>(this);
   private int globalCooldown = 20;
   private int daggerReadyAt;
   private int iaiReadyAt;
   private int hammerReadyAt;
   @Nullable
   private VarkosMove intentMove;
   private int intentChosenAt;
   private boolean lastPickForcedHammer;
   private boolean intentBandWaived;
   private double lastTargetDist = Double.MAX_VALUE;
   private byte lastUsed = 0;
   private byte usedBeforeLast = 0;
   private boolean pendingCombo;
   private int comboReadyAt;
   boolean iaiChainArmed;
   private int hugSince = Integer.MIN_VALUE;
   private boolean hugged;
   private int fleeSampleTick = Integer.MIN_VALUE;
   private double fleeSampleDist;
   private boolean fleeing;
   private boolean prevTargetSwinging;
   private int prevTargetSwingTime;
   private int targetSwingTick = Integer.MIN_VALUE;
   private boolean targetSwingNear;
   private int lastTargetHitAt = -1000;
   private int lastHurtAt = -1000;
   private int sidestepTicks;
   private Vec3 sidestepDir = Vec3.ZERO;
   private int sidestepReadyAt;
   private int staggerTicks;
   private int staggerDuration;
   private boolean forceHammer;
   @Nullable
   private Set<UUID> watchedSlamHits;
   private int watchedSlamCracks;
   private int longRangeWhiffs;
   private boolean pendingFutilityClick;
   private boolean futilityRealized;
   private int clickBeatUntil;
   private boolean p2SetPieceDone;
   private boolean p3SetPieceDone;
   private final List<HammerCrack.Crack> cracks = new ArrayList<>();
   double flurryDriftLeft;
   double iaiDashSpeed;
   @Nullable
   private BlockPos postPos;
   private static final byte CEREMONY_NONE = 0;
   private static final byte CEREMONY_INTRO_BOW = 1;
   private static final byte CEREMONY_VICTORY_BOW = 2;
   private static final byte CEREMONY_RETURN = 3;
   private static final byte CEREMONY_IDLE_AT_POST = 4;
   private static final double ETIQUETTE_CLEAR_RADIUS = 24.0;
   private static final double RETURN_ARRIVE_DIST = 2.0;
   private static final int RETURN_TIMEOUT = 600;
   private static final float IDLE_REGEN = 0.35F;
   private byte ceremonyStage = 0;
   private int ceremonyTicks;
   @Nullable
   private Vec3 fallPos;
   private int returnStartedAt;
   private boolean wasInCombat;
   @Nullable
   private LivingEntity lastCombatTarget;
   @Nullable
   private Vec3 lastCombatTargetPos;
   private final Set<UUID> damagedThisFight = new HashSet<>();
   private static final double DAGGER_ENGAGE = 3.0;
   private static final double DAGGER_ORBIT = 3.0;
   private static final double IAI_ORBIT = 8.0;
   private static final double IAI_FAR = 20.0;
   private static final double SERPENTINE_AMP_MIN = 1.5;
   private static final double SERPENTINE_AMP_MAX = 2.5;
   private static final double SERPENTINE_STRIDE = 8.0;
   private static final double FOOTWORK_BACKOFF_INSIDE = 4.0;
   private int retreatUntil;
   private int repathAt;
   private boolean serpentineRight;
   private static final double SIDESTEP_MIN = 6.0;
   private static final double SIDESTEP_MAX = 15.0;
   private static final double SIDESTEP_MISS_BY = 1.5;
   private static final int SIDESTEP_TICKS = 6;
   private static final double SIDESTEP_SPEED = 0.3;
   private static final int SIDESTEP_COOLDOWN = 80;
   private static final int SIDESTEP_SCAN_INTERVAL = 2;

   public VarkosEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level, Component.translatable("entity.awakened.varkos"), BossBarColor.PURPLE, BossBarOverlay.NOTCHED_6);
      this.xpReward = 100;
      this.setPersistenceRequired();
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 420.0)
         .add(Attributes.ARMOR, 12.0)
         .add(Attributes.ARMOR_TOUGHNESS, 4.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
         .add(Attributes.MOVEMENT_SPEED, 0.32)
         .add(Attributes.ATTACK_DAMAGE, 12.0)
         .add(Attributes.FOLLOW_RANGE, 64.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected BossDefenses.Profile defenseProfile() {
      return DEFENSE_PROFILE;
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_WEAPON, (byte)0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(5, new GatedGoal(new WaterAvoidingRandomStrollGoal(this, 0.7), () -> this.getTarget() == null && this.ceremonyStage == 0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
      this.goalSelector.addGoal(7, new GatedGoal(new RandomLookAroundGoal(this), () -> this.getTarget() == null && this.ceremonyStage == 0));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public byte getWeapon() {
      return (Byte)this.entityData.get(DATA_WEAPON);
   }

   private void setWeapon(byte weapon) {
      this.entityData.set(DATA_WEAPON, weapon);
   }

   public static boolean isHostileTarget(LivingEntity e) {
      if (e instanceof Player p && !p.isCreative() && !p.isSpectator()) {
         return true;
      }

      return false;
   }

   private static byte weaponFor(byte action) {
      return switch (action) {
         case 1 -> 1;
         case 4 -> 2;
         case 9 -> 3;
         default -> 0;
      };
   }

   void spawnHammerCrack(HammerCrack.Crack crack) {
      this.cracks.add(crack);
      if (this.watchedSlamHits != null && crack.sharesHits(this.watchedSlamHits)) {
         this.watchedSlamCracks++;
      }
   }

   void watchSlamForFutility(Set<UUID> slamHits, double launchDist) {
      if (!this.futilityRealized && !this.pendingFutilityClick && !this.intentBandWaived && !(launchDist <= 18.0)) {
         this.watchedSlamHits = slamHits;
         this.watchedSlamCracks = 0;
      } else {
         this.watchedSlamHits = null;
      }
   }

   void onHammerCrackFinished(Set<UUID> slamHits) {
      if (this.watchedSlamHits != null && slamHits == this.watchedSlamHits && --this.watchedSlamCracks <= 0) {
         boolean landed = !this.watchedSlamHits.isEmpty();
         this.watchedSlamHits = null;
         if (landed) {
            this.longRangeWhiffs = 0;
         } else if (++this.longRangeWhiffs >= 4) {
            this.pendingFutilityClick = true;
         }
      }
   }

   private void performFutilityClick(ServerLevel level, LivingEntity target) {
      this.pendingFutilityClick = false;
      this.futilityRealized = true;
      this.clickBeatUntil = this.tickCount + 8;
      this.sidestepTicks = 0;
      this.setWeapon((byte)0);
      this.scheduler.stopCircling();
      this.getNavigation().stop();
      this.getLookControl().setLookAt(target, 30.0F, 30.0F);
      Fx.sound(level, this.position(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.7F, 0.45F);
      this.setIntent(this.pickIntent(target, false));
   }

   private boolean tickClickBeat(ServerLevel level, LivingEntity target) {
      if (this.tickCount >= this.clickBeatUntil) {
         return false;
      } else {
         this.getLookControl().setLookAt(target, 30.0F, 30.0F);
         if (this.tickCount == this.clickBeatUntil - 4) {
            Fx.sound(level, this.position(), (SoundEvent)SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.HOSTILE, 0.8F, 0.55F);
         }

         return true;
      }
   }

   private void resetFutilityLedger() {
      this.watchedSlamHits = null;
      this.watchedSlamCracks = 0;
      this.longRangeWhiffs = 0;
      this.pendingFutilityClick = false;
      this.futilityRealized = false;
      this.clickBeatUntil = 0;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.cracks.isEmpty() && this.level() instanceof ServerLevel server) {
         HammerCrack.tick(server, this, this.cracks);
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      if (this.deathTime <= 0) {
         if (this.staggerTicks > 0) {
            this.tickStagger(level);
         } else {
            LivingEntity target = this.getTarget();
            this.tickTetherKite(level, target);
            this.tickThreatDetectors(target);
            if (target != null) {
               this.wasInCombat = true;
               this.lastCombatTarget = target;
               this.lastCombatTargetPos = target.position();
            }

            if (this.scheduler.isRunning()) {
               this.scheduler.advance(level, target);
               if (!this.scheduler.isRunning()) {
                  this.setWeapon((byte)0);
                  this.onMoveEnded(target);
               }
            } else if (this.ceremonyStage != 0) {
               this.tickCeremony(level, target);
            } else if (target == null) {
               this.intentMove = null;
               this.pendingCombo = false;
               this.maybeStartVictoryEtiquette(level);
            } else {
               if (this.intentMove == null) {
                  this.setIntent(this.pickIntent(target, false));
               }

               if (this.globalCooldown > 0) {
                  this.globalCooldown--;
               }

               if (this.pendingFutilityClick) {
                  this.performFutilityClick(level, target);
               } else if (!this.tickClickBeat(level, target)) {
                  if (this.sidestepTicks > 0) {
                     this.driveSidestep(target);
                  } else if (this.tickCount >= this.sidestepReadyAt && this.tickCount % 2 == 0 && this.tryStartSidestep(level, target)) {
                     this.driveSidestep(target);
                  } else if (this.globalCooldown > 0 || !this.tryFireIntent(level, target)) {
                     this.tickFootwork(target);
                     if (this.tickCount - this.intentChosenAt >= 80) {
                        if (this.isCommittedClosing(target)) {
                           if (this.futilityRealized) {
                              if (this.getRandom().nextDouble() < 0.25 && this.weaponReadyWithin((byte)9, 30)) {
                                 this.setIntent(VarkosMoves.HAMMER_SLAM);
                              } else {
                                 this.intentChosenAt = this.tickCount;
                              }
                           } else {
                              this.setIntent(this.pickIntent(target, false));
                           }
                        } else {
                           this.setIntent(this.pickIntent(target, true));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isCommittedClosing(LivingEntity target) {
      return this.intentMove != null && this.intentMove.action() != 9 && (double)this.distanceTo(target) > 20.0;
   }

   private void onMoveEnded(@Nullable LivingEntity target) {
      this.globalCooldown = this.neutralGap();
      this.pendingCombo = false;
      if (this.iaiChainArmed && this.lastUsed == 4 && target != null) {
         this.iaiChainArmed = false;
         this.pendingCombo = true;
         this.comboReadyAt = this.tickCount + 200;
         this.globalCooldown = 8;
         this.setIntent(VarkosMoves.DAGGER_FLURRY);
      } else {
         this.iaiChainArmed = false;
         if (target != null) {
            this.setIntent(this.pickIntent(target, false));
         } else {
            this.intentMove = null;
         }
      }
   }

   void tryArmIaiChain() {
      this.iaiChainArmed = this.escalationPhase() >= 2 && this.tickCount >= this.comboReadyAt && this.getRandom().nextDouble() < 0.5;
   }

   private void setIntent(VarkosMove move) {
      this.intentMove = move;
      this.intentChosenAt = this.tickCount;
      this.intentBandWaived = this.lastPickForcedHammer;
      this.lastPickForcedHammer = false;
   }

   private VarkosMove pickIntent(LivingEntity target, boolean inBandOnly) {
      if (this.forceHammer) {
         this.forceHammer = false;
         this.lastPickForcedHammer = true;
         return VarkosMoves.HAMMER_SLAM;
      } else {
         List<VarkosMove> pool = this.escalationPhase() >= 2 ? VarkosMoves.KIT_P2 : VarkosMoves.KIT_P1;
         double dist = (double)this.distanceTo(target);
         double total = 0.0;
         double[] weights = new double[pool.size()];

         for (int i = 0; i < weights.length; i++) {
            weights[i] = this.intentWeight(pool.get(i), dist, inBandOnly);
            total += weights[i];
         }

         if (total <= 0.0) {
            return inBandOnly ? this.pickIntent(target, false) : this.soonestReady(pool, dist);
         } else {
            double roll = this.getRandom().nextDouble() * total;

            for (int i = 0; i < weights.length; i++) {
               roll -= weights[i];
               if (roll < 0.0) {
                  return pool.get(i);
               }
            }

            return pool.get(pool.size() - 1);
         }
      }
   }

   private double intentWeight(VarkosMove m, double dist, boolean inBandOnly) {
      double distToBand = Math.max(0.0, Math.max(m.minRange() - dist, dist - m.maxRange()));
      if (inBandOnly && distToBand > 0.0) {
         return 0.0;
      } else if (m.action() == 9 && distToBand > 0.0) {
         return 0.0;
      } else if (!this.weaponReadyWithin(m.action(), 30)) {
         return 0.0;
      } else {
         double w = (double)m.weight();
         if (m.action() == 9 && !this.fleeing) {
            w *= this.hammerRangeFactor(dist);
         }

         boolean iaiAtRange = m.action() == 4 && dist >= m.minRange() && dist <= m.maxRange();
         if (!iaiAtRange) {
            w *= Mth.clamp(1.0 - distToBand / 14.0, 0.1, 1.0);
         }

         if (m.action() == 1 && dist <= 7.0) {
            w *= 3.0;
         }

         if (m.action() == this.lastUsed) {
            w *= this.lastUsed == this.usedBeforeLast ? 0.2 : 0.6;
         }

         if (this.hugged && m.action() == this.hugAnswer()) {
            w *= 20.0;
         }

         if (this.fleeing && m.action() == 9) {
            w *= 20.0;
         }

         return w;
      }
   }

   private double hammerRangeFactor(double dist) {
      if (dist <= 18.0) {
         return 1.0;
      } else {
         return this.futilityRealized ? 0.25 : 1.0;
      }
   }

   private byte hugAnswer() {
      return (byte)(this.escalationPhase() >= 2 ? 1 : 4);
   }

   private VarkosMove soonestReady(List<VarkosMove> pool, double dist) {
      VarkosMove best = null;

      for (VarkosMove m : pool) {
         if ((m.action() != 9 || !(dist < m.minRange()) && !(dist > m.maxRange()) && (!this.futilityRealized || this.fleeing || !(dist > 18.0)))
            && (best == null || this.weaponReadyAt(m.action()) < this.weaponReadyAt(best.action()))) {
            best = m;
         }
      }

      return best != null ? best : pool.get(0);
   }

   private boolean tryFireIntent(ServerLevel level, LivingEntity target) {
      VarkosMove move = this.intentMove;
      if (move == null) {
         return false;
      } else if (this.pendingCombo) {
         this.pendingCombo = false;
         if ((!move.requiresGround() || this.onGround()) && move.canStart(this, level, target)) {
            this.scheduler.stopCircling();
            this.startWeaponMove(level, move, target);
            return true;
         } else {
            this.setIntent(this.pickIntent(target, false));
            this.globalCooldown = this.neutralGap();
            return false;
         }
      } else if (!this.weaponReady(move.action())) {
         return false;
      } else {
         double dsq = this.distanceToSqr(target);
         boolean inBand = dsq >= move.minRange() * move.minRange() && dsq <= move.maxRange() * move.maxRange();
         if (!inBand && !this.intentBandWaived) {
            return false;
         } else if (move.requiresGround() && !this.onGround()) {
            return false;
         } else {
            if (move.action() == 4) {
               VarkosMoves.iaiHold = 10 + this.getRandom().nextInt(21);
            }

            if (!move.canStart(this, level, target)) {
               return false;
            } else {
               this.scheduler.stopCircling();
               this.startWeaponMove(level, move, target);
               return true;
            }
         }
      }
   }

   private int weaponReadyAt(byte action) {
      return switch (action) {
         case 1 -> this.daggerReadyAt;
         case 4 -> this.iaiReadyAt;
         case 9 -> this.hammerReadyAt;
         default -> 0;
      };
   }

   private boolean weaponReady(byte action) {
      return this.tickCount >= this.weaponReadyAt(action);
   }

   private boolean weaponReadyWithin(byte action, int horizon) {
      return this.weaponReadyAt(action) - this.tickCount <= horizon;
   }

   private void startWeaponMove(ServerLevel level, VarkosMove move, LivingEntity target) {
      byte action = move.action();
      this.setWeapon(weaponFor(action));
      switch (action) {
         case 1:
            this.daggerReadyAt = this.tickCount + 40;
            break;
         case 4:
            this.iaiReadyAt = this.tickCount + 40;
            break;
         case 9:
            this.hammerReadyAt = this.tickCount + 40;
      }

      this.usedBeforeLast = this.lastUsed;
      this.lastUsed = action;
      this.scheduler.start(level, move, target);
   }

   private void tickThreatDetectors(@Nullable LivingEntity target) {
      if (target == null) {
         this.hugSince = Integer.MIN_VALUE;
         this.hugged = false;
         this.fleeSampleTick = Integer.MIN_VALUE;
         this.fleeing = false;
         this.lastTargetDist = Double.MAX_VALUE;
         this.targetSwingTick = Integer.MIN_VALUE;
         this.prevTargetSwinging = false;
      } else {
         double dist = (double)this.distanceTo(target);
         this.tickWhiffWatch(target, dist);
         boolean wasHugged = this.hugged;
         if (dist <= 4.0) {
            if (this.hugSince == Integer.MIN_VALUE) {
               this.hugSince = this.tickCount;
            }

            this.hugged = this.tickCount - this.hugSince >= 60;
         } else {
            this.hugSince = Integer.MIN_VALUE;
            this.hugged = false;
         }

         if (this.hugged && !wasHugged) {
            this.onThreatShift(target, true);
         }

         if (this.fleeSampleTick == Integer.MIN_VALUE) {
            this.fleeSampleTick = this.tickCount;
            this.fleeSampleDist = dist;
         } else if (this.tickCount - this.fleeSampleTick >= 20) {
            boolean wasFleeing = this.fleeing;
            this.fleeing = dist - this.fleeSampleDist >= 2.5;
            this.fleeSampleTick = this.tickCount;
            this.fleeSampleDist = dist;
            if (this.fleeing && !wasFleeing) {
               this.onThreatShift(target, false);
            }
         }

         if (this.intentMove != null
            && !this.scheduler.isRunning()
            && !this.pendingCombo
            && dist < this.intentMove.minRange()
            && this.lastTargetDist >= this.intentMove.minRange()) {
            this.setIntent(this.pickIntent(target, false));
         }

         this.lastTargetDist = dist;
      }
   }

   private void tickWhiffWatch(LivingEntity target, double dist) {
      if (this.targetSwingTick != Integer.MIN_VALUE && this.tickCount > this.targetSwingTick) {
         boolean whiffed = this.targetSwingNear && this.lastTargetHitAt < this.targetSwingTick - 1;
         this.targetSwingTick = Integer.MIN_VALUE;
         if (whiffed && !this.scheduler.isRunning() && !this.pendingCombo) {
            this.globalCooldown = Math.min(this.globalCooldown, 6);
         }
      }

      boolean swingEdge = target.swinging && (!this.prevTargetSwinging || target.swingTime < this.prevTargetSwingTime);
      if (swingEdge && this.targetSwingTick == Integer.MIN_VALUE) {
         this.targetSwingTick = this.tickCount;
         this.targetSwingNear = dist <= 4.5;
      }

      this.prevTargetSwinging = target.swinging;
      this.prevTargetSwingTime = target.swingTime;
   }

   private void onThreatShift(LivingEntity target, boolean hug) {
      if (!this.scheduler.isRunning() && !this.pendingCombo) {
         this.setIntent(this.pickIntent(target, false));
         if (hug) {
            this.globalCooldown = Math.min(this.globalCooldown, 6);
         }
      }
   }

   private void tickFootwork(LivingEntity target) {
      this.getLookControl().setLookAt(target, 30.0F, 30.0F);
      double dist = (double)this.distanceTo(target);
      byte intent = this.intentMove == null ? 0 : this.intentMove.action();
      if (intent == 1) {
         if (dist > 3.0) {
            this.approach(target);
         } else {
            this.scheduler.tickCircling(target, true, 0.0, 4.5, 3.0, this.footworkSpeed());
         }
      } else if (intent == 9) {
         this.scheduler.stopCircling();
         this.getNavigation().stop();
      } else if (this.tickCount >= this.retreatUntil && !(dist < 4.0)) {
         if (dist > 20.0) {
            this.approach(target);
         } else {
            this.scheduler.tickCircling(target, true, 0.0, 22.0, 8.0, this.footworkSpeed());
         }
      } else {
         if (this.tickCount >= this.retreatUntil) {
            this.retreatUntil = this.tickCount + 15;
         }

         this.retreat(target);
      }
   }

   private void approach(LivingEntity target) {
      this.scheduler.stopCircling();
      if (this.tickCount >= this.repathAt || this.getNavigation().isDone()) {
         this.repathAt = this.tickCount + 10;
         double dist = (double)this.distanceTo(target);
         if (dist <= 20.0) {
            this.getNavigation().moveTo(target, this.footworkSpeed());
         } else {
            Vec3 dir = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ()).normalize();
            this.serpentineRight = !this.serpentineRight;
            double amp = 1.5 + this.getRandom().nextDouble() * 1.0;
            Vec3 perp = new Vec3(-dir.z, 0.0, dir.x).scale(this.serpentineRight ? amp : -amp);
            Vec3 dest = this.position().add(dir.scale(Math.min(8.0, dist - 8.0))).add(perp);
            this.getNavigation().moveTo(dest.x, dest.y, dest.z, this.footworkSpeed());
         }
      }
   }

   private void retreat(LivingEntity target) {
      this.scheduler.stopCircling();
      Vec3 away = new Vec3(this.getX() - target.getX(), 0.0, this.getZ() - target.getZ());
      away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot() + 180.0F) : away.normalize();
      if (this.tickCount >= this.repathAt || this.getNavigation().isDone()) {
         this.repathAt = this.tickCount + 10;
         Vec3 back = this.position().add(away.scale(4.0));
         this.getNavigation().moveTo(back.x, this.getY(), back.z, this.footworkSpeed());
      }
   }

   private double footworkSpeed() {
      return switch (this.escalationPhase()) {
         case 1 -> 1.0;
         case 2 -> 1.15;
         default -> 1.3;
      };
   }

   private boolean tryStartSidestep(ServerLevel level, LivingEntity target) {
      Vec3 center = this.getBoundingBox().getCenter();

      for (Projectile proj : level.getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(15.0))) {
         Vec3 vel = proj.getDeltaMovement();
         if (!(vel.lengthSqr() < 0.25)) {
            Entity toSelf = proj.getOwner();
            if (toSelf instanceof Player) {
               Player owner = (Player)toSelf;
               if (isHostileTarget(owner)) {
                  Vec3 toSelfx = center.subtract(proj.position());
                  double dist = toSelfx.length();
                  if (!(dist < 6.0) && !(dist > 15.0)) {
                     Vec3 dir = vel.normalize();
                     double along = toSelfx.dot(dir);
                     if (!(along <= 0.0)) {
                        Vec3 closest = proj.position().add(dir.scale(along));
                        if (!(closest.distanceToSqr(center) > 2.25)) {
                           Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
                           if (perp.lengthSqr() < 1.0E-4) {
                              perp = new Vec3(1.0, 0.0, 0.0);
                           }

                           perp = perp.normalize();
                           if (perp.dot(center.subtract(closest)) < 0.0) {
                              perp = perp.scale(-1.0);
                           }

                           this.sidestepDir = perp;
                           this.sidestepTicks = 6;
                           this.sidestepReadyAt = this.tickCount + 80;
                           this.scheduler.stopCircling();
                           this.getNavigation().stop();
                           Fx.sound(level, this.position(), (SoundEvent)SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.HOSTILE, 0.8F, 1.4F);
                           Fx.burst(level, VarkosMoves.VIOLET, this.position().add(0.0, 0.3, 0.0), 6, 0.4, 0.04);
                           return true;
                        }
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private void driveSidestep(LivingEntity target) {
      this.getLookControl().setLookAt(target, 30.0F, 30.0F);
      boolean first = this.sidestepTicks == 6;
      this.setDeltaMovement(this.sidestepDir.x * 0.3, this.getDeltaMovement().y, this.sidestepDir.z * 0.3);
      if (first) {
         this.hurtMarked = true;
      }

      this.sidestepTicks--;
   }

   private int neutralGap() {
      int gap = switch (this.escalationPhase()) {
         case 1 -> 20;
         case 2 -> 14;
         default -> 10;
      };
      return gap + this.getRandom().nextInt(gap / 3 + 1);
   }

   private void tickTetherKite(ServerLevel level, @Nullable LivingEntity target) {
      if (this.postPos != null && target != null) {
         if (this.distanceToSqr(Vec3.atCenterOf(this.postPos)) > 2304.0) {
            ThreatMoves.blinkNear(this, target, 2.0, VarkosMoves.VIOLET);
            this.heal(0.1F);
         }
      }
   }

   public void beginIntroBow(Player challenger) {
      this.setTarget(challenger);
      this.damagedThisFight.clear();
      this.resetFutilityLedger();
      this.startBow((byte)1);
   }

   private void startBow(byte stage) {
      this.ceremonyStage = stage;
      this.ceremonyTicks = 70;
      this.setWeapon((byte)0);
      this.setAction((byte)16);
      this.setActionProgress(0.0F);
      int windup = Math.round(24.5F);
      this.setMoveBudget(windup, 70 - windup);
      this.getNavigation().stop();
      this.scheduler.stopCircling();
      if (this.level() instanceof ServerLevel server) {
         Fx.sound(server, this.position(), (SoundEvent)SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.HOSTILE, 0.7F, 0.9F);
      }
   }

   private void tickCeremony(ServerLevel level, @Nullable LivingEntity target) {
      if (this.ceremonyStage != 1 && target != null) {
         this.cancelCeremonyIntoCombat();
      } else {
         switch (this.ceremonyStage) {
            case 1:
            case 2:
               this.tickBow(level, target);
               break;
            case 3:
               this.tickReturn(level);
               break;
            case 4:
               this.heal(0.35F);
               break;
            default:
               this.ceremonyStage = 0;
         }
      }
   }

   private void tickBow(ServerLevel level, @Nullable LivingEntity target) {
      Vec3 focus = this.ceremonyStage == 1 ? (target != null ? target.position() : this.fallPos) : this.fallPos;
      if (focus != null) {
         this.faceCeremonially(focus);
      }

      float progress = 1.0F - (float)this.ceremonyTicks / 70.0F;
      this.setActionProgress(progress);
      if (this.ceremonyTicks == 70 - Math.round(24.5F)) {
         Fx.sound(level, this.position(), (SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 0.5F, 0.7F);
      }

      if (--this.ceremonyTicks <= 0) {
         this.clearCeremonyAction();
         if (this.ceremonyStage == 1) {
            this.ceremonyStage = 0;
            this.globalCooldown = this.neutralGap();
            this.intentMove = null;
         } else {
            this.ceremonyStage = 3;
            this.returnStartedAt = this.tickCount;
         }
      }
   }

   private void tickReturn(ServerLevel level) {
      if (this.postPos != null && !(this.distanceToSqr(Vec3.atCenterOf(this.postPos)) <= 4.0) && this.tickCount - this.returnStartedAt <= 600) {
         if (this.tickCount >= this.repathAt || this.getNavigation().isDone()) {
            this.repathAt = this.tickCount + 20;
            Vec3 home = Vec3.atCenterOf(this.postPos);
            this.getNavigation().moveTo(home.x, home.y, home.z, 0.9);
         }
      } else {
         this.getNavigation().stop();
         this.settleAtPost();
      }
   }

   private void settleAtPost() {
      this.ceremonyStage = 4;
      this.damagedThisFight.clear();
      this.resetFutilityLedger();
      this.wasInCombat = false;
      this.lastCombatTarget = null;
      this.lastCombatTargetPos = null;
      this.fallPos = null;
   }

   private void maybeStartVictoryEtiquette(ServerLevel level) {
      if (this.wasInCombat) {
         if (level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0), p -> isHostileTarget(p) && p.isAlive()).isEmpty()) {
            boolean challengerDied = this.lastCombatTarget != null && this.lastCombatTarget.isDeadOrDying();
            this.wasInCombat = false;
            if (challengerDied && this.lastCombatTargetPos != null) {
               this.fallPos = this.lastCombatTargetPos;
               this.startBow((byte)2);
            } else {
               this.ceremonyStage = 3;
               this.returnStartedAt = this.tickCount;
            }
         }
      }
   }

   private void cancelCeremonyIntoCombat() {
      if (this.ceremonyStage != 0) {
         this.ceremonyStage = 0;
         this.ceremonyTicks = 0;
         this.clearCeremonyAction();
         this.getNavigation().stop();
         this.intentMove = null;
         this.globalCooldown = Math.max(this.globalCooldown, 10);
      }
   }

   private void clearCeremonyAction() {
      if (this.getAction() == 16) {
         this.setAction((byte)0);
         this.setActionProgress(0.0F);
         this.setMoveBudget(0, 0);
      }
   }

   private void faceCeremonially(Vec3 focus) {
      this.getLookControl().setLookAt(focus.x, this.getEyeY(), focus.z);
      double dx = focus.x - this.getX();
      double dz = focus.z - this.getZ();
      if (dx * dx + dz * dz > 1.0E-4) {
         float want = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
         float yaw = Mth.approachDegrees(this.getYRot(), want, 6.0F);
         this.setYRot(yaw);
         this.yBodyRot = yaw;
      }
   }

   boolean hurtChallenger(ServerLevel level, Player victim, float amount) {
      boolean applied = Dmg.hurt(victim, level, this.damageSources().mobAttack(this), amount);
      if (applied) {
         this.damagedThisFight.add(victim.getUUID());
      }

      return applied;
   }

   @Override
   protected void onTargetHidden(ServerLevel level, LivingEntity target) {
      this.forceHammer = true;
      this.hammerReadyAt = Math.min(this.hammerReadyAt, this.tickCount);
      if (!this.scheduler.isRunning() && !this.pendingCombo) {
         this.setIntent(this.pickIntent(target, false));
      }
   }

   @Override
   protected boolean bossHurt(ServerLevel level, DamageSource source, float amount) {
      if (this.deathTime > 0) {
         return false;
      } else {
         if (source.getEntity() instanceof Player p && p == this.getTarget()) {
            this.lastTargetHitAt = this.tickCount;
         }

         boolean applied = super.bossHurt(level, source, amount);
         if (applied) {
            this.lastHurtAt = this.tickCount;
            this.cancelCeremonyIntoCombat();
         }

         return applied;
      }
   }

   boolean hurtWithin(int ticks) {
      return this.tickCount - this.lastHurtAt <= ticks;
   }

   public void startStagger(int duration) {
      this.staggerTicks = duration;
      this.staggerDuration = duration;
      this.sidestepTicks = 0;
      this.iaiChainArmed = false;
      this.cancelCeremonyIntoCombat();
      this.scheduler.interrupt();
      this.setWeapon((byte)0);
      this.setAction((byte)18);
      this.setActionProgress(0.0F);
      this.setMoveBudget(8, duration - 8);
   }

   public boolean isCurrentlyStaggered() {
      return this.staggerTicks > 0;
   }

   private void tickStagger(ServerLevel level) {
      this.setActionProgress(1.0F - (float)this.staggerTicks / (float)Math.max(1, this.staggerDuration));
      if (--this.staggerTicks <= 0) {
         this.setAction((byte)0);
         this.setActionProgress(0.0F);
         this.setMoveBudget(0, 0);
         this.globalCooldown = 10;
         this.intentMove = null;
      }
   }

   @Override
   protected void onEscalate(ServerLevel level, int newTier) {
      this.bossEvent.setColor(newTier >= 3 ? BossBarColor.WHITE : BossBarColor.RED);
      if (newTier == 2 && !this.p2SetPieceDone) {
         this.p2SetPieceDone = true;
         this.triggerPhaseSetPiece(level, ChatFormatting.RED);
      }

      if (newTier == 3 && !this.p3SetPieceDone) {
         this.p3SetPieceDone = true;
         this.triggerPhaseSetPiece(level, ChatFormatting.AQUA);
      }
   }

   private void triggerPhaseSetPiece(ServerLevel level, ChatFormatting color) {
      this.startStagger(40);
      Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
      Vfx.sphere(level, mid, 2.0, 11563263, 18);
      Fx.burst(level, ParticleTypes.CRIT, mid, 20, 0.8, 0.1);
      Fx.sound(level, this.position(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.5F, 0.6F);
      Fx.shake(level, this.position(), 22.0, 0.3F, 4, 20);
   }

   @Override
   protected int deathDuration() {
      return 150;
   }

   @Override
   protected void onDeathSequenceStart() {
      this.setAction((byte)0);
      this.setActionProgress(0.0F);
      this.setWeapon((byte)0);
      this.scheduler.interrupt();
   }

   @Override
   protected void deathBeat(ServerLevel level, int t) {
      if (t == 1) {
         Fx.sound(level, this.position(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 1.6F, 0.3F);
         Fx.shake(level, this.position(), 35.0, 0.5F, 10, 50);
         Fx.burst(level, VarkosMoves.VIOLET, this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0), 20, 0.8, 0.1);
      }

      if (t == 20) {
         Fx.sound(level, this.position(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.9F, 0.8F);
         Fx.burst(level, VarkosMoves.VIOLET, this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0), 20, 1.0, 0.08);
         Fx.shake(level, this.position(), 16.0, 0.18F, 0, 10);
      }

      if (t == 90) {
         Vfx.ring(level, this.position().add(0.0, 0.2, 0.0), 3.0, 11563263, 30);
         Fx.sound(level, this.position(), (SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.2F, 0.5F);
      }

      if (t == 110) {
         Fx.burst(level, ParticleTypes.CLOUD, this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0), 14, 0.5, 0.04);
         Vfx.ring(level, this.position().add(0.0, 0.1, 0.0), 4.0, 11563263, 20);
      }

      if (t == 140) {
         Fx.shake(level, this.position(), 18.0, 0.2F, 0, 10);

         for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(this) <= 1024.0)) {
            sp.displayClientMessage(Component.translatable("entity.awakened.varkos.settled").withStyle(ChatFormatting.AQUA), true);
         }
      }
   }

   @Override
   protected void dropCinematicLoot(ServerLevel level) {
      bless(RelicDrops.dropSignature(level, this.position().add(0.0, 1.0, 0.0), "duelists_edge"));
      int shards = 4 + this.getRandom().nextInt(5);
      bless(this.spawnAtFeet(level, new ItemStack(Items.ECHO_SHARD, shards)));
      bless(this.spawnAtFeet(level, new ItemStack((ItemLike)AwakenedItems.IAI_BLADE.get())));
   }

   @Override
   public double bonusMastery() {
      return 250.0;
   }

   @Override
   public double bonusXp() {
      return 850.0;
   }

   @Override
   public void onCreditedKill(ServerPlayer player) {
      AwakenedAttachments.progress(player).bossDefeated();
      PowerService.syncProgress(player);
      QuestSystem.onBossDefeated(player);
      if (!this.damagedThisFight.contains(player.getUUID())) {
         AwakenedAdvancements.grant(player, "untouched_varkos");
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("P2Done", this.p2SetPieceDone);
      tag.putBoolean("P3Done", this.p3SetPieceDone);
      if (this.postPos != null) {
         tag.putLong("PostPos", this.postPos.asLong());
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.p2SetPieceDone = tag.getBoolean("P2Done");
      this.p3SetPieceDone = tag.getBoolean("P3Done");
      this.postPos = tag.contains("PostPos") ? BlockPos.of(tag.getLong("PostPos")) : null;
   }

   @Nullable
   public BlockPos postPos() {
      return this.postPos;
   }

   public void bindToPost(BlockPos pos) {
      this.postPos = pos;
   }
}
