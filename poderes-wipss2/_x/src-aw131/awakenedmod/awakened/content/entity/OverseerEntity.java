package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.content.summoning.MinionSupport;
import com.awakenedmod.awakened.content.summoning.PossessMelee;
import com.awakenedmod.awakened.core.AwakenedThreat;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.relic.RelicDrops;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class OverseerEntity extends Monster implements Enemy, RangedAttackMob, AwakenedThreat, PossessMelee {
   private static final int SUMMON_INTERVAL = 240;
   private static final int SHOCKWAVE_INTERVAL = 160;
   private static final int CHARGE_INTERVAL = 110;
   private static final int ERUPT_INTERVAL = 90;
   private static final int MAX_ADDS = 2;
   public static final int BARRAGE_WINDUP_TICKS = 50;
   public static final int SHOCKWAVE_TELL_TICKS = 18;
   public static final int CHARGE_TELL_TICKS = 12;
   public static final int ERUPT_TELL_TICKS = 16;
   public static final byte PHASE_NONE = 0;
   public static final byte PHASE_BARRAGE = 1;
   public static final byte PHASE_SHOCKWAVE = 2;
   public static final byte PHASE_CHARGE = 3;
   public static final byte PHASE_ERUPT = 4;
   private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(OverseerEntity.class, EntityDataSerializers.BYTE);
   private final ServerBossEvent bossEvent = new ServerBossEvent(
      Component.translatable("entity.awakened.overseer"), BossBarColor.RED, BossBarOverlay.NOTCHED_10
   );
   private int summonCooldown = 240;
   private int shockwaveCooldown = 160;
   private int chargeCooldown = 110;
   private int eruptCooldown = 90;
   private int barrageWindup = 0;
   private int shockwaveTell = 0;
   private int chargeTell = 0;
   private int eruptTell = 0;
   private Vec3 eruptCenter = Vec3.ZERO;
   private int healthPhase = 1;

   public OverseerEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = 50;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 300.0)
         .add(Attributes.ATTACK_DAMAGE, 16.0)
         .add(Attributes.MOVEMENT_SPEED, 0.28)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.95)
         .add(Attributes.ARMOR, 10.0)
         .add(Attributes.FOLLOW_RANGE, 40.0);
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_PHASE, (byte)0);
   }

   public byte getPhase() {
      return (Byte)this.entityData.get(DATA_PHASE);
   }

   private void setPhase(byte phase) {
      this.entityData.set(DATA_PHASE, phase);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
      this.goalSelector.addGoal(2, new RangedAttackGoal(this, 0.8, 50, 20.0F));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public void setTarget(LivingEntity target) {
      LivingEntity prev = this.getTarget();
      super.setTarget(target);
      if (target != null && prev == null && this.level() instanceof ServerLevel server) {
         Fx.scanLock(server, this.position().add(0.0, (double)this.getBbHeight() * 0.85, 0.0), SoundEvents.BEACON_ACTIVATE, 1.6F, 0.7F);
      }
   }

   public void performRangedAttack(LivingEntity target, float velocity) {
      if (this.level() instanceof ServerLevel server) {
         Vec3 var11 = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
         int bolts = 2 + this.healthPhase;
         double first = (double)(-(bolts - 1)) / 2.0;

         for (int i = 0; i < bolts; i++) {
            KineticProjectile bolt = new KineticProjectile(AwakenedEntities.KINETIC_CHARGE.get(), this, server, new ItemStack(Items.FIRE_CHARGE));
            bolt.configure(6.0F, 2.0F, 2.0F, 0.4F);
            Vec3 aim = var11.subtract(bolt.position()).yRot((float)Math.toRadians((first + (double)i) * 10.0));
            bolt.shoot(aim.x, aim.y, aim.z, 1.7F, 2.0F);
            Fx.burst(server, Fx.OPTIC_RED, bolt.position(), 8, 0.2, 0.05);
            server.addFreshEntity(bolt);
         }

         this.barrageWindup = 0;
         this.setPhase((byte)0);
         Fx.sound(server, this.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.3F, 0.7F);
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
      this.updateHealthPhase(level);
      LivingEntity target = this.getTarget();
      if (target != null && this.distanceToSqr(target) <= 400.0) {
         if (this.barrageWindup < 50) {
            this.barrageWindup++;
         }

         if (this.barrageWindup >= 49 && this.getPhase() == 0) {
            this.setPhase((byte)1);
         }

         if (this.getPhase() == 1 && this.barrageWindup % 6 == 0) {
            this.emitPylonCharge(level);
         }
      } else if (this.barrageWindup > 0) {
         this.barrageWindup = 0;
         if (this.getPhase() == 1) {
            this.setPhase((byte)0);
         }
      }

      if (target != null) {
         double dsq = this.distanceToSqr(target);
         if (this.chargeTell > 0) {
            if (--this.chargeTell <= 0) {
               ThreatMoves.leapToward(this, target, 1.5, 0.6);
               if (this.getPhase() == 3) {
                  this.setPhase((byte)0);
               }

               this.chargeCooldown = this.chargeInterval();
            }
         } else if (--this.chargeCooldown <= 0 && this.onGround() && dsq <= 484.0 && dsq >= 36.0 && this.getPhase() == 0) {
            this.setPhase((byte)3);
            this.chargeTell = 12;
            Fx.sound(level, this.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 0.6F);
            Fx.burst(level, Fx.OPTIC_RED, this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0), 10, 0.5, 0.04);
         }

         if (this.eruptTell > 0) {
            Fx.burst(level, Fx.OPTIC_RED, this.eruptCenter.add(0.0, 0.1, 0.0), 6, 0.35, 0.01);
            if (--this.eruptTell <= 0) {
               ThreatMoves.groundErupt(level, this, this.eruptCenter, 3.5, 7.0F + (float)this.healthPhase, 0.55);
               if (this.getPhase() == 4) {
                  this.setPhase((byte)0);
               }

               this.eruptCooldown = this.eruptInterval();
            }
         } else if (--this.eruptCooldown <= 0 && dsq <= 784.0 && this.getPhase() == 0) {
            this.setPhase((byte)4);
            this.eruptTell = 16;
            this.eruptCenter = target.position();
            Fx.sound(level, this.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0F, 0.5F);
         }

         if (--this.summonCooldown <= 0) {
            this.summonCooldown = this.summonInterval();
            this.summonAdds(level);
         }

         if (this.shockwaveTell <= 0 && --this.shockwaveCooldown <= 18 && this.getPhase() == 0) {
            this.setPhase((byte)2);
            this.shockwaveTell = 18;
         }

         if (this.shockwaveTell > 0 && --this.shockwaveTell <= 0) {
            this.shockwaveCooldown = this.shockwaveInterval();
            if (this.getPhase() == 2) {
               this.setPhase((byte)0);
            }

            this.shockwave(level);
         }
      }
   }

   private int summonInterval() {
      return Math.max(120, 240 - (this.healthPhase - 1) * 60);
   }

   private int shockwaveInterval() {
      return Math.max(70, 160 - (this.healthPhase - 1) * 40);
   }

   private int chargeInterval() {
      return Math.max(60, 110 - (this.healthPhase - 1) * 25);
   }

   private int eruptInterval() {
      return Math.max(50, 90 - (this.healthPhase - 1) * 20);
   }

   private void updateHealthPhase(ServerLevel level) {
      float frac = this.getHealth() / this.getMaxHealth();
      int target = frac > 0.66F ? 1 : (frac > 0.33F ? 2 : 3);
      if (target > this.healthPhase) {
         this.healthPhase = target;
         this.onPhaseEscalate(level);
      }
   }

   private void onPhaseEscalate(ServerLevel level) {
      Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0);
      Fx.burst(level, Fx.OPTIC_RED, mid, 40, 1.2, 0.15);
      Fx.burst(level, ParticleTypes.SONIC_BOOM, mid, 1, 0.0, 0.0);
      Fx.sound(level, this.position(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.6F, 0.7F);
      this.shockwave(level);
      this.summonCooldown = 30;
      this.shockwaveCooldown = this.shockwaveInterval();
      boolean desperate = this.healthPhase >= 3;
      this.bossEvent.setName(Component.translatable(desperate ? "entity.awakened.overseer.desperate" : "entity.awakened.overseer.enraged"));
      if (desperate) {
         this.bossEvent.setColor(BossBarColor.PURPLE);
      }
   }

   private void emitPylonCharge(ServerLevel level) {
      Vec3 base = this.position();

      for (int side = -1; side <= 1; side += 2) {
         Vec3 pylon = base.add((double)side * 0.95, (double)this.getBbHeight() * 0.9, 0.0);
         Fx.burst(level, Fx.OPTIC_RED, pylon, 6, 0.15, 0.02);
      }
   }

   private void summonAdds(ServerLevel level) {
      long nearby = (long)level.getEntitiesOfClass(SuppressorEntity.class, this.getBoundingBox().inflate(28.0)).size();
      if (nearby < 2L) {
         for (int i = 0; i < 1; i++) {
            SuppressorEntity add = new SuppressorEntity(AwakenedEntities.SUPPRESSOR.get(), level);
            double dx = (this.getRandom().nextDouble() - 0.5) * 4.0;
            double dz = (this.getRandom().nextDouble() - 0.5) * 4.0;
            add.moveTo(this.getX() + dx, this.getY() + 1.0, this.getZ() + dz, this.getRandom().nextFloat() * 360.0F, 0.0F);
            add.setTarget(this.getTarget());
            MinionSupport.inheritAllegiance(this, add);
            level.addFreshEntity(add);
            Fx.burst(level, Fx.HAZARD_AMBER, add.position().add(0.0, 1.0, 0.0), 20, 0.4, 0.1);
            Fx.burst(level, ParticleTypes.SMOKE, add.position().add(0.0, 1.0, 0.0), 18, 0.4, 0.05);
         }

         Fx.sound(level, this.position(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.2F, 0.8F);
      }
   }

   private void shockwave(ServerLevel level) {
      ThreatMoves.shockwave(level, this, 6.0, 5.0F, 1.2, 0.5);
   }

   public boolean doHurtTarget(Entity target) {
      ServerLevel level = (ServerLevel)this.level();
      boolean hit = super.doHurtTarget(target);
      if (hit) {
         Fx.burst(level, ParticleTypes.EXPLOSION, target.position().add(0.0, 0.5, 0.0), 4, 0.3, 0.05);
         Fx.sound(level, this.position(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 0.6F);
      }

      return hit;
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
   public void onCreditedKill(ServerPlayer player) {
      AwakenedAttachments.progress(player).bossDefeated();
      PowerService.syncProgress(player);
      QuestSystem.onBossDefeated(player);
   }

   public void die(DamageSource cause) {
      super.die(cause);
      if (this.level() instanceof ServerLevel server) {
         Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
         Fx.burst(server, ParticleTypes.SCULK_SOUL, mid, 40, 1.0, 0.15);
         Fx.burst(server, ParticleTypes.SOUL, mid, 40, 1.2, 0.2);
         RelicDrops.dropSignature(server, this.position().add(0.0, 1.0, 0.0), "wardens_eye");
      }
   }

   @Override
   public boolean isBoss() {
      return true;
   }

   @Override
   public double bonusMastery() {
      return 120.0;
   }

   @Override
   public double bonusXp() {
      return 400.0;
   }

   public void startSeenByPlayer(ServerPlayer player) {
      super.startSeenByPlayer(player);
      this.bossEvent.addPlayer(player);
   }

   public void stopSeenByPlayer(ServerPlayer player) {
      super.stopSeenByPlayer(player);
      this.bossEvent.removePlayer(player);
   }
}
