package com.awakenedmod.awakened.content.entity.sentinel;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.SuppressorEntity;
import com.awakenedmod.awakened.content.entity.ai.GatedGoal;
import com.awakenedmod.awakened.content.entity.boss.AwakenedBossEntity;
import com.awakenedmod.awakened.content.entity.boss.BossDefenses;
import com.awakenedmod.awakened.content.entity.boss.MoveScheduler;
import com.awakenedmod.awakened.content.summoning.MinionSupport;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.relic.RelicDrops;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SentinelEntity extends AwakenedBossEntity {
   private static final int STAGGER_DURATION = 100;
   private static final float STAGGER_DMG_MULT = 2.25F;
   private static final float SHIELD_REDUCT = 0.25F;
   private static final BossDefenses.Profile DEFENSE_PROFILE = new BossDefenses.Profile(34.0, 45.0F, 5.0, 100);
   private static final int SHIELD_INTERVAL = 220;
   private static final int SHIELD_DURATION = 90;
   private static final int SUMMON_INTERVAL = 360;
   private static final int HOVER_INTERVAL = 300;
   private static final int HOVER_DURATION = 90;
   private static final int MAX_ADDS = 2;
   private static final int COMBO_INTERVAL = 140;
   private static final EntityDataAccessor<Integer> DATA_ABSORBED = SynchedEntityData.defineId(SentinelEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_STAGGERED = SynchedEntityData.defineId(SentinelEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_SHIELDED = SynchedEntityData.defineId(SentinelEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(SentinelEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_IN_COMBAT = SynchedEntityData.defineId(SentinelEntity.class, EntityDataSerializers.BOOLEAN);
   private final EnumSet<AbsorbedPower> arsenal = EnumSet.noneOf(AbsorbedPower.class);
   private final List<SentinelMove> stolen = new ArrayList<>();
   private final MoveScheduler<SentinelEntity> scheduler = new MoveScheduler<>(this);
   private int globalCooldown = 30;
   private final Deque<SentinelMove> comboQueue = new ArrayDeque<>();
   private boolean comboRunning;
   private int staggerTicks;
   private int staggerDuration = 100;
   private int shieldTicks;
   private int shieldCooldown = 220;
   private int summonCooldown = 360;
   private int hoverTicks;
   private int hoverCooldown = 300;
   private double hoverDesiredY;
   private int comboCooldown = 140;

   public SentinelEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level, Component.translatable("entity.awakened.sentinel"), BossBarColor.PURPLE, BossBarOverlay.NOTCHED_10);
      this.xpReward = 80;
      this.setPersistenceRequired();
   }

   @Override
   protected BossDefenses.Profile defenseProfile() {
      return DEFENSE_PROFILE;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 700.0)
         .add(Attributes.ATTACK_DAMAGE, 18.0)
         .add(Attributes.MOVEMENT_SPEED, 0.26)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ARMOR, 12.0)
         .add(Attributes.FOLLOW_RANGE, 48.0)
         .add(Attributes.STEP_HEIGHT, 1.5);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_ABSORBED, 0);
      builder.define(DATA_STAGGERED, false);
      builder.define(DATA_SHIELDED, false);
      builder.define(DATA_FLYING, false);
      builder.define(DATA_IN_COMBAT, false);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new GatedGoal(new MeleeAttackGoal(this, 1.0, true), () -> !this.aiMovementSuppressed()));
      this.goalSelector.addGoal(3, new GatedGoal(new MoveTowardsTargetGoal(this, 0.85, 40.0F), () -> !this.aiMovementSuppressed()));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public int getAbsorbedMask() {
      return (Integer)this.entityData.get(DATA_ABSORBED);
   }

   public boolean isStaggered() {
      return (Boolean)this.entityData.get(DATA_STAGGERED);
   }

   public boolean isShielded() {
      return (Boolean)this.entityData.get(DATA_SHIELDED);
   }

   public boolean isHovering() {
      return (Boolean)this.entityData.get(DATA_FLYING);
   }

   public boolean isInCombat() {
      return (Boolean)this.entityData.get(DATA_IN_COMBAT);
   }

   public static boolean isHostileTarget(LivingEntity e) {
      if (e instanceof Player p && !p.isCreative() && !p.isSpectator()) {
         return true;
      }

      return false;
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      this.entityData.set(DATA_IN_COMBAT, this.getTarget() != null);
      if (this.staggerTicks > 0) {
         this.tickStagger(level);
      } else {
         LivingEntity target = this.getTarget();
         this.tickHover(level);
         this.tickShield(level);
         if (!this.scheduler.isRunning()) {
            if (target == null) {
               this.setAction((byte)0);
            } else if (this.escalation < 2 || !this.trySummon(level, target)) {
               if (this.escalation >= 2 && this.tryHoverLift(level, target)) {
               }

               if (--this.globalCooldown <= 0) {
                  if (this.escalation >= 3 && this.comboQueue.isEmpty() && --this.comboCooldown <= 0) {
                     this.buildCombo(level, target);
                  }

                  SentinelMove move = !this.comboQueue.isEmpty() ? this.pollComboQueue(level, target) : this.pickMove(level, target);
                  if (move != null) {
                     this.scheduler.start(level, move, target);
                  }
               }
            }
         } else {
            this.scheduler.advance(level, target);
            if (!this.scheduler.isRunning()) {
               this.globalCooldown = this.comboRunning && !this.comboQueue.isEmpty() ? 4 : this.cadence();
               if (this.comboQueue.isEmpty()) {
                  this.comboRunning = false;
               }
            }
         }
      }
   }

   private SentinelMove pickMove(ServerLevel level, LivingEntity target) {
      List<SentinelMove> pool = new ArrayList<>(SentinelMoves.BASE_KIT);
      pool.addAll(this.stolen);
      return this.scheduler.pickMove(level, target, pool, this.onGround() && this.hoverTicks <= 0);
   }

   private int cadence() {
      int base = switch (this.escalation) {
         case 1 -> 40;
         case 2 -> 24;
         default -> 14;
      };
      return base + this.getRandom().nextInt(base / 2 + 1);
   }

   private void buildCombo(ServerLevel level, LivingEntity target) {
      int n = 2 + this.getRandom().nextInt(2);

      for (int i = 0; i < n; i++) {
         SentinelMove m = this.pickMove(level, target);
         if (m != null) {
            this.comboQueue.add(m);
         }
      }

      if (!this.comboQueue.isEmpty()) {
         this.comboRunning = true;
         this.comboCooldown = 140;
         Fx.burst(level, Fx.OPTIC_RED, this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0), 30, 1.0, 0.1);
         Fx.sound(level, this.position(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.4F, 1.1F);
      }
   }

   @Nullable
   private SentinelMove pollComboQueue(ServerLevel level, LivingEntity target) {
      boolean grounded = this.onGround() && this.hoverTicks <= 0;

      while (!this.comboQueue.isEmpty()) {
         SentinelMove m = this.comboQueue.poll();
         if ((!m.requiresGround() || grounded) && m.canStart(this, level, target)) {
            return m;
         }
      }

      return this.pickMove(level, target);
   }

   @Override
   protected boolean bossHurt(ServerLevel level, DamageSource source, float amount) {
      if (this.staggerTicks <= 0 && source.getEntity() instanceof ServerPlayer attacker) {
         this.tryAbsorb(level, attacker);
      }

      if (this.staggerTicks > 0) {
         amount *= 2.25F;
      } else if (this.isShielded() && this.blockedByShield(source)) {
         amount *= 0.25F;
         Fx.burst(level, Fx.dust(-5213953, 1.2F), this.position().add(0.0, (double)this.getBbHeight() * 0.55, 0.0), 6, 0.6, 0.02);
         Fx.sound(level, this.position(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 1.0F, 1.7F);
         Fx.burst(
            level,
            ParticleTypes.ELECTRIC_SPARK,
            this.position().add(this.getLookAngle().scale(1.4)).add(0.0, (double)this.getBbHeight() * 0.55, 0.0),
            8,
            0.4,
            0.08
         );
      }

      return super.bossHurt(level, source, amount);
   }

   private void tryAbsorb(ServerLevel level, ServerPlayer attacker) {
      PowerData data = AwakenedAttachments.get(attacker);
      if (data.hasPower()) {
         AbsorbedPower power = AbsorbedPower.byPowerId(data.powerId());
         if (power != null && this.arsenal.add(power)) {
            this.rebuildStolen();
            this.entityData.set(DATA_ABSORBED, maskOf(this.arsenal));
            this.onAdapted(level, power);
         }
      }
   }

   private void rebuildStolen() {
      this.stolen.clear();

      for (AbsorbedPower p : this.arsenal) {
         this.stolen.addAll(p.moves());
      }
   }

   private static int maskOf(EnumSet<AbsorbedPower> set) {
      int mask = 0;

      for (AbsorbedPower p : set) {
         mask |= p.bit();
      }

      return mask;
   }

   private void onAdapted(ServerLevel level, AbsorbedPower power) {
      Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0);
      DustParticleOptions tint = Fx.dust(power.glowColor() & 16777215, 1.6F);

      for (double h = 0.0; h <= (double)this.getBbHeight(); h += 0.4) {
         Fx.burst(level, tint, this.position().add(0.0, h, 0.0), 3, (double)this.getBbWidth() * 0.6, 0.0);
      }

      Fx.burst(level, tint, mid, 40, 1.2, 0.08);
      Fx.sound(level, this.position(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.6F, 1.4F);
      Fx.sound(level, this.position(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.HOSTILE, 1.4F, 0.8F);
      Component msg = Component.translatable("entity.awakened.sentinel.adapted", new Object[]{Component.translatable("power.awakened." + power.powerId())})
         .withStyle(ChatFormatting.RED);

      for (ServerPlayer p : level.getPlayers(px -> px.distanceToSqr(this) <= 4096.0)) {
         p.displayClientMessage(msg, true);
      }
   }

   @Override
   protected void onEscalate(ServerLevel level, int newTier) {
      this.enterStagger(level);
   }

   private void enterStagger(ServerLevel level) {
      this.staggerTicks = 100;
      this.staggerDuration = 100;
      this.scheduler.interrupt();
      this.comboQueue.clear();
      this.comboRunning = false;
      this.setShield(false);
      this.endHover();
      this.setAction((byte)14);
      this.setActionProgress(0.0F);
      this.setMoveBudget(8, this.staggerDuration - 8);
      this.entityData.set(DATA_STAGGERED, true);
      Fx.burst(level, ParticleTypes.SONIC_BOOM, this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0), 1, 0.0, 0.0);
      Fx.sound(level, this.position(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.8F, 0.4F);
      Fx.sound(level, this.position(), SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 0.8F, 1.6F);
      this.bossEvent.setName(this.stageName());
   }

   private void tickStagger(ServerLevel level) {
      this.setActionProgress(1.0F - (float)this.staggerTicks / (float)this.staggerDuration);
      if (this.staggerTicks % 4 == 0) {
         Vec3 v = this.position()
            .add(
               (this.getRandom().nextDouble() - 0.5) * (double)this.getBbWidth(),
               this.getRandom().nextDouble() * (double)this.getBbHeight(),
               (this.getRandom().nextDouble() - 0.5) * (double)this.getBbWidth()
            );
         Fx.burst(level, ParticleTypes.LARGE_SMOKE, v, 2, 0.1, 0.01);
         Fx.burst(level, Fx.HAZARD_AMBER, v, 3, 0.1, 0.02);
      }

      if (--this.staggerTicks <= 0) {
         this.entityData.set(DATA_STAGGERED, false);
         this.setAction((byte)0);
         this.setActionProgress(0.0F);
         this.setMoveBudget(0, 0);
         this.globalCooldown = 6;
         this.comboCooldown = 40;
         this.shieldCooldown = 110;
         Fx.burst(level, Fx.OPTIC_RED, this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0), 50, 1.4, 0.18);
         Fx.sound(level, this.position(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.8F, 0.7F);
      }
   }

   private Component stageName() {
      String key = switch (this.escalation) {
         case 1 -> "entity.awakened.sentinel";
         case 2 -> "entity.awakened.sentinel.aggressive";
         default -> "entity.awakened.sentinel.overload";
      };
      return Component.translatable(key);
   }

   private void setShield(boolean on) {
      this.entityData.set(DATA_SHIELDED, on);
   }

   private void tickShield(ServerLevel level) {
      if (this.escalation >= 2) {
         if (this.shieldTicks > 0) {
            if (--this.shieldTicks <= 0) {
               this.setShield(false);
            } else if (this.shieldTicks % 6 == 0) {
               Vec3 fwd = this.getLookAngle();
               Vec3 c = this.position().add(fwd.x * 1.6, (double)this.getBbHeight() * 0.55, fwd.z * 1.6);
               Fx.sphereShell(level, Fx.dust(-5213953, 1.0F), c, 1.6, 18);
            }
         } else if (--this.shieldCooldown <= 0 && !this.scheduler.isRunning()) {
            this.shieldTicks = 90;
            this.shieldCooldown = 220;
            this.setShield(true);
            Fx.sound(level, this.position(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.6F, 0.5F);
         }
      }
   }

   private boolean blockedByShield(DamageSource source) {
      if (source.getEntity() instanceof LivingEntity attacker) {
         Vec3 to = attacker.position().subtract(this.position());
         return to.lengthSqr() < 1.0E-4 ? true : to.normalize().dot(this.getLookAngle()) > 0.2;
      } else {
         return false;
      }
   }

   private boolean trySummon(ServerLevel level, LivingEntity target) {
      if (--this.summonCooldown > 0) {
         return false;
      } else {
         this.summonCooldown = 360;
         long nearby = (long)level.getEntitiesOfClass(SuppressorEntity.class, this.getBoundingBox().inflate(30.0)).size();
         if (nearby >= 2L) {
            return false;
         } else {
            this.setAction((byte)13);
            this.setActionProgress(0.0F);

            for (int i = 0; i < 1; i++) {
               SuppressorEntity add = new SuppressorEntity(AwakenedEntities.SUPPRESSOR.get(), level);
               double dx = (this.getRandom().nextDouble() - 0.5) * 5.0;
               double dz = (this.getRandom().nextDouble() - 0.5) * 5.0;
               add.moveTo(this.getX() + dx, this.getY() + 1.0, this.getZ() + dz, this.getRandom().nextFloat() * 360.0F, 0.0F);
               add.setTarget(target);
               MinionSupport.inheritAllegiance(this, add);
               level.addFreshEntity(add);
               Fx.burst(level, Fx.HAZARD_AMBER, add.position().add(0.0, 1.0, 0.0), 18, 0.4, 0.1);
            }

            Fx.sound(level, this.position(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.4F, 0.7F);
            this.globalCooldown = 16;
            return true;
         }
      }
   }

   private boolean tryHoverLift(ServerLevel level, LivingEntity target) {
      if (this.hoverTicks > 0 || --this.hoverCooldown > 0) {
         return false;
      } else if (this.distanceToSqr(target) < 64.0) {
         this.hoverCooldown = 100;
         return false;
      } else {
         this.hoverTicks = 90;
         this.hoverCooldown = 300;
         this.hoverDesiredY = this.getY() + 4.0;
         this.entityData.set(DATA_FLYING, true);
         Fx.sound(level, this.position(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1.5F, 1.2F);
         return true;
      }
   }

   private void tickHover(ServerLevel level) {
      if (this.hoverTicks > 0) {
         this.setNoGravity(true);
         double dy = this.hoverDesiredY - this.getY();
         double vy = Math.abs(dy) > 0.3 ? Math.signum(dy) * 0.18 : Math.sin((double)this.tickCount * 0.2) * 0.03;
         this.setDeltaMovement(this.getDeltaMovement().x * 0.8, vy, this.getDeltaMovement().z * 0.8);
         this.hurtMarked = true;
         if (this.tickCount % 6 == 0) {
            Fx.burst(level, Fx.OPTIC_RED, this.position().add(0.0, 0.2, 0.0), 6, 0.5, 0.02);
         }

         if (--this.hoverTicks <= 0) {
            this.endHover();
         }
      }
   }

   private void endHover() {
      if (this.hoverTicks > 0 || this.isHovering()) {
         this.hoverTicks = 0;
         this.setNoGravity(false);
         this.entityData.set(DATA_FLYING, false);
      }
   }

   public void setTarget(LivingEntity target) {
      LivingEntity prev = this.getTarget();
      super.setTarget(target);
      if (target != null && prev == null && this.level() instanceof ServerLevel server) {
         Fx.scanLock(server, this.position().add(0.0, (double)this.getBbHeight() * 0.86, 0.0), SoundEvents.BEACON_ACTIVATE, 1.7F, 0.6F);
      }
   }

   public boolean doHurtTarget(Entity target) {
      ServerLevel level = (ServerLevel)this.level();
      boolean hit = super.doHurtTarget(target);
      if (hit) {
         Fx.burst(level, ParticleTypes.EXPLOSION, target.position().add(0.0, 0.5, 0.0), 4, 0.3, 0.05);
         Fx.sound(level, this.position(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 0.5F);
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
   public void die(DamageSource cause) {
      super.die(cause);
      if (this.level() instanceof ServerLevel server) {
         Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
         Fx.burst(server, ParticleTypes.SCULK_SOUL, mid, 50, 1.2, 0.18);
         Fx.burst(server, ParticleTypes.SOUL, mid, 50, 1.4, 0.2);
         RelicDrops.dropAt(server, this.position().add(0.0, 1.0, 0.0), this.getRandom(), Rarity.RARE, Rarity.EPIC, true);
      }
   }

   @Override
   public double bonusMastery() {
      return 160.0;
   }

   @Override
   public double bonusXp() {
      return 550.0;
   }

   @Override
   public void onCreditedKill(ServerPlayer player) {
      AwakenedAttachments.progress(player).bossDefeated();
      PowerService.syncProgress(player);
      QuestSystem.onBossDefeated(player);
   }
}
