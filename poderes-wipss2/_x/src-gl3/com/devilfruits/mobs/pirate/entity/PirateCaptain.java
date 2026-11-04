package com.devilfruits.mobs.pirate.entity;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.npc.Locomotion;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.mobs.pirate.PirateClips;
import com.devilfruits.mobs.pirate.PirateConfig;
import com.devilfruits.mobs.pirate.PirateFx;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.goal.CaptainGoal;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public class PirateCaptain extends PirateFighter {
   private static final Locomotion.Speeds SPEEDS = new Locomotion.Speeds(2.0F, 4.0F);
   public static final PhysicsProfile SLAM_PROFILE = new PhysicsProfile(PhysicsProfile.Impact.RADIAL, 1.0, PhysicsProfile.Reaction.LANDING, 0.0, false);
   private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(), BossBarColor.RED, BossBarOverlay.NOTCHED_10);

   public PirateCaptain(EntityType<? extends PirateCaptain> type, Level level) {
      super(type, level);
      this.xpReward = 40;
      this.bossEvent.setVisible(false);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 150.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ATTACK_DAMAGE, 10.0)
         .add(Attributes.FOLLOW_RANGE, 40.0)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
         .add(Attributes.SCALE, 1.35)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected void applyStats() {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((Double)PirateConfig.CAPTAIN_HEALTH.get());
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((Double)PirateConfig.CAPTAIN_DAMAGE.get());
      this.getAttribute(Attributes.ARMOR).setBaseValue((Double)PirateConfig.CAPTAIN_ARMOR.get());
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((Double)PirateConfig.CAPTAIN_SPEED.get());
      this.getAttribute(Attributes.SCALE).setBaseValue((Double)PirateConfig.CAPTAIN_SCALE.get());
   }

   @Override
   public int rank() {
      return 2;
   }

   @Override
   public int bounty() {
      return 50;
   }

   @Override
   protected int variantCount() {
      return 2;
   }

   @Override
   public Locomotion.Speeds locomotionSpeeds() {
      return SPEEDS;
   }

   public boolean enraged() {
      return this.hasFlag(4);
   }

   @Override
   protected void addCombatGoals() {
      this.goalSelector.addGoal(1, new CaptainGoal(this));
   }

   protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack((ItemLike)Pirates.CAPTAIN_CUTLASS.get()));
      this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack((ItemLike)Pirates.FLINTLOCK_PISTOL.get()));
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
      this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      if ((Boolean)PirateConfig.CAPTAIN_CREW.get()
         && (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.EVENT)) {
         this.spawnCrew(level, difficulty);
      }

      return result;
   }

   public void spawnCrew(ServerLevelAccessor level, DifficultyInstance difficulty) {
      RandomSource random = level.getRandom();
      int deckhands = Mth.randomBetweenInclusive(
         random,
         (Integer)PirateConfig.CREW_DECKHANDS_MIN.get(),
         Math.max((Integer)PirateConfig.CREW_DECKHANDS_MIN.get(), (Integer)PirateConfig.CREW_DECKHANDS_MAX.get())
      );
      int gunners = Mth.randomBetweenInclusive(
         random,
         (Integer)PirateConfig.CREW_GUNNERS_MIN.get(),
         Math.max((Integer)PirateConfig.CREW_GUNNERS_MIN.get(), (Integer)PirateConfig.CREW_GUNNERS_MAX.get())
      );

      for (int i = 0; i < deckhands + gunners; i++) {
         EntityType<? extends PirateFighter> type = i < deckhands ? Pirates.DECKHAND.get() : Pirates.GUNNER.get();
         PirateFighter mate = (PirateFighter)type.create(level.getLevel());
         if (mate != null) {
            double a = random.nextDouble() * (float) (Math.PI * 2);
            double r = 1.5 + random.nextDouble() * 2.5;
            BlockPos pos = BlockPos.containing(this.getX() + Math.cos(a) * r, this.getY(), this.getZ() + Math.sin(a) * r);
            mate.moveTo((double)pos.getX() + 0.5, this.getY(), (double)pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            mate.finalizeSpawn(level, difficulty, MobSpawnType.MOB_SUMMONED, null);
            mate.setLeader(this);
            level.addFreshEntityWithPassengers(mate);
         }
      }
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      LivingEntity target = this.getTarget();
      if (target instanceof Player) {
         this.setPersistenceRequired();
      }

      if (!this.enraged() && (double)this.getHealth() < (double)this.getMaxHealth() * (Double)PirateConfig.CAPTAIN_ENRAGE_HEALTH.get() && !this.isActing()) {
         this.setFlag(4, true);
         this.playAction(15, PirateClips.LENGTH[15]);
      }

      this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
      this.bossEvent.setVisible((Boolean)PirateConfig.CAPTAIN_BOSS_BAR.get() && target != null && this.isAlive());
      if (this.enraged() && this.tickCount % 4 == 0 && this.level() instanceof ServerLevel server) {
         server.sendParticles(
            ParticleTypes.SMALL_FLAME,
            this.getX(),
            this.getY() + (double)this.getBbHeight() * 0.6,
            this.getZ(),
            1,
            (double)this.getBbWidth() * 0.4,
            (double)this.getBbHeight() * 0.3,
            (double)this.getBbWidth() * 0.4,
            0.01
         );
      }
   }

   public void startSeenByPlayer(ServerPlayer player) {
      super.startSeenByPlayer(player);
      this.bossEvent.addPlayer(player);
   }

   public void stopSeenByPlayer(ServerPlayer player) {
      super.stopSeenByPlayer(player);
      this.bossEvent.removePlayer(player);
   }

   @Override
   protected void onActionTick(int id, int tick) {
      LivingEntity target = this.getTarget();
      int hit = PirateClips.HIT[id];
      switch (id) {
         case 2:
         case 10:
            if (tick == hit) {
               this.swingFx(0.8F);
               this.meleeHit(target, id == 2 ? 0.8F : 1.0F, 0.5, 0.6);
            }
         case 3:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         default:
            break;
         case 11:
            if (tick == 4) {
               this.playSound(Pirates.RALLY.get(), 0.6F, 1.4F);
            } else if (tick == hit) {
               this.slam();
            }
            break;
         case 12:
            if (tick == PirateClips.MOVE[id] && target != null) {
               Vec3 to = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
               Vec3 dash = to.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot()) : to.normalize();
               this.setDeltaMovement(dash.scale(Math.min(1.4, 0.4 + to.length() * 0.12)).add(0.0, 0.18, 0.0));
               this.hasImpulse = true;
               this.swingFx(0.7F);
            } else if (tick == hit) {
               this.meleeHit(target, 1.3F, 0.8, 1.2);
            }
            break;
         case 13:
            if (tick == hit && target != null && target.isAlive()) {
               this.shootAt(target, ((Double)PirateConfig.CAPTAIN_PISTOL_DAMAGE.get()).floatValue(), 1.0F, false, true);
            }
            break;
         case 14:
            if (tick == hit) {
               this.rally();
            }
            break;
         case 15:
            if (tick == hit && this.level() instanceof ServerLevel server) {
               this.playSound(Pirates.ENRAGE.get(), 2.0F, 0.7F);
               server.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + (double)this.getBbHeight() * 0.5, this.getZ(), 30, 0.6, 0.8, 0.6, 0.08);
               ServerFx.sendNear(server, PirateFx.ENRAGE, this.getId(), this.position(), Vec3.ZERO, 1.0F, this.random.nextLong(), 0, 48.0);
            }
      }
   }

   @Override
   protected void onActionEnd(int id) {
      if (id == 10 && this.enraged()) {
         LivingEntity target = this.getTarget();
         if (target != null && target.isAlive() && this.isWithinMeleeAttackRange(target)) {
            this.playAction(2, PirateClips.LENGTH[2]);
         }
      }
   }

   private void slam() {
      if (this.level() instanceof ServerLevel server) {
         Vec3 var15 = Vec3.directionFromRotation(0.0F, this.getYRot());
         Vec3 at = this.position().add(var15.scale(1.4 * (double)this.getScale()));
         double radius = (Double)PirateConfig.CAPTAIN_SLAM_RADIUS.get();
         float damage = ((Double)PirateConfig.CAPTAIN_SLAM_DAMAGE.get()).floatValue();

         for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, 2.0, radius), this::canHarm)) {
            double d = e.position().distanceTo(at);
            if (!(d > radius)) {
               float falloff = (float)Mth.clamp(1.0 - d / radius * 0.6, 0.4, 1.0);
               e.hurt(this.damageSources().mobAttack(this), damage * falloff);
               Vec3 out = e.position().subtract(at).multiply(1.0, 0.0, 1.0);
               Vec3 push = out.lengthSqr() < 1.0E-4 ? var15 : out.normalize();
               Targeting.launch(e, push.scale(1.1 * (double)falloff).add(0.0, 0.55 * (double)falloff, 0.0));
            }
         }

         this.playSound(Pirates.SLAM.get(), 2.0F, 0.8F);
         ServerFx.shake(server, at, 0.6F, 24);
         ServerFx.sendNear(server, PirateFx.SLAM, this.getId(), at, var15, (float)radius, this.random.nextLong(), 0, 64.0);
         BlockState below = this.level().getBlockState(BlockPos.containing(at).below());
         if (!below.isAir()) {
            ServerFx.blockBurst(server, at, below, 1.2F);
         }

         BlockImpactService.pushFrom(
            this, ImpactShape.sphere(at, radius), new Vec3(0.0, -1.0, 0.0), SLAM_PROFILE.strength((double)damage, 1.5), SLAM_PROFILE.impact()
         );
         if ((Boolean)PirateConfig.CAPTAIN_SLAM_BLOCK_DAMAGE.get() && EventHooks.canEntityGrief(server, this)) {
            this.crack(server, BlockPos.containing(at));
         }
      }
   }

   private void crack(ServerLevel server, BlockPos center) {
      int broken = 0;

      for (BlockPos p : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, -1, 2))) {
         BlockState state = server.getBlockState(p);
         if (broken < 6
            && !state.isAir()
            && !state.hasBlockEntity()
            && state.getDestroySpeed(server, p) >= 0.0F
            && state.getDestroySpeed(server, p) <= 1.5F
            && this.random.nextFloat() < 0.4F) {
            server.destroyBlock(p, true, this);
            broken++;
         }
      }
   }

   private void rally() {
      if (this.level() instanceof ServerLevel server) {
         double var7 = (Double)PirateConfig.RALLY_RADIUS.get();
         int ticks = (Integer)PirateConfig.RALLY_SECONDS.get() * 20;

         for (PirateFighter mate : server.getEntitiesOfClass(
            PirateFighter.class, this.getBoundingBox().inflate(var7), m -> m.isAlive() && (double)m.distanceTo(this) <= var7
         )) {
            mate.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0), this);
            mate.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0), this);
         }

         this.playSound(Pirates.RALLY.get(), 3.0F, 0.8F);
         ServerFx.sendNear(server, PirateFx.RALLY, this.getId(), this.position(), Vec3.ZERO, (float)var7, this.random.nextLong(), 0, 64.0);
      }
   }

   public int crewNearby(double r) {
      return this.level().getEntitiesOfClass(PirateFighter.class, this.getBoundingBox().inflate(r), m -> m != this && m.isAlive()).size();
   }

   public boolean removeWhenFarAway(double distance) {
      return !(this.getTarget() instanceof Player) && super.removeWhenFarAway(distance);
   }

   protected void playStepSound(BlockPos pos, BlockState state) {
      super.playStepSound(pos, state);
      this.playSound(Pirates.HEAVY_STEP.get(), 0.5F, 0.7F + this.random.nextFloat() * 0.1F);
   }

   @Override
   protected SoundEvent ambient() {
      return Pirates.CAPTAIN_AMBIENT.get();
   }

   @Override
   protected SoundEvent hurt() {
      return Pirates.CAPTAIN_HURT.get();
   }

   @Override
   protected SoundEvent death() {
      return Pirates.CAPTAIN_DEATH.get();
   }
}
