package com.devilfruits.mobs.pirate.entity;

import com.devilfruits.core.npc.Faction;
import com.devilfruits.core.npc.FactionFighter;
import com.devilfruits.core.npc.FollowLeaderGoal;
import com.devilfruits.core.npc.Locomotion;
import com.devilfruits.mobs.pirate.PirateConfig;
import com.devilfruits.mobs.pirate.Pirates;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class PirateFighter extends FactionFighter {
   private static final Locomotion.Speeds SPEEDS = new Locomotion.Speeds(2.2F, 4.6F);

   protected PirateFighter(EntityType<? extends PirateFighter> type, Level level) {
      super(type, level);
      this.xpReward = 8;
   }

   @Override
   public Faction faction() {
      return Faction.PIRATE;
   }

   @Override
   public Locomotion.Speeds locomotionSpeeds() {
      return SPEEDS;
   }

   protected abstract void addCombatGoals();

   protected abstract void applyStats();

   protected void registerGoals() {
      int scan = (Integer)PirateConfig.TARGET_SCAN_INTERVAL.get();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.addCombatGoals();
      this.goalSelector.addGoal(5, new FollowLeaderGoal(this, 1.0));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.75));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[]{FactionFighter.class}).setAlertOthers(new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, scan, true, false, null));
      this.targetSelector
         .addGoal(3, new NearestAttackableTargetGoal(this, AbstractVillager.class, scan, true, false, e -> (Boolean)PirateConfig.HUNT_VILLAGERS.get()));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, IronGolem.class, scan, true, false, e -> (Boolean)PirateConfig.HUNT_GOLEMS.get()));
      this.targetSelector
         .addGoal(4, new NearestAttackableTargetGoal(this, AbstractIllager.class, scan, true, false, e -> (Boolean)PirateConfig.HUNT_ILLAGERS.get()));
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
      RandomSource random = level.getRandom();
      this.applyStats();
      AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
      if (health != null) {
         health.setBaseValue(health.getBaseValue() * (1.0 + 0.25 * (double)difficulty.getSpecialMultiplier()));
      }

      this.setHealth(this.getMaxHealth());
      this.setVariant(random.nextInt(Math.max(1, this.variantCount())));
      this.populateDefaultEquipmentSlots(random, difficulty);
      float drop = ((Double)PirateConfig.GEAR_DROP_CHANCE.get()).floatValue();
      this.setDropChance(EquipmentSlot.MAINHAND, drop);
      this.setDropChance(EquipmentSlot.OFFHAND, drop);
      return result;
   }

   public static boolean checkPirateSpawnRules(
      EntityType<? extends Mob> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random
   ) {
      if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
         return false;
      } else if (MobSpawnType.isSpawner(reason)) {
         return true;
      } else {
         return random.nextDouble() >= PirateConfig.SPAWN_CHANCE.get()
            ? false
            : level.getEntitiesOfClass(PirateFighter.class, new AABB(pos).inflate(64.0)).size() < (Integer)PirateConfig.MAX_NEARBY.get();
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      if (this.tickCount % 10 == 0 && this.getTarget() == null) {
         FactionFighter leader = this.leader();
         if (leader != null && leader.getTarget() != null && leader.getTarget().isAlive() && this.distanceToSqr(leader) < 2304.0) {
            this.setTarget(leader.getTarget());
         }
      }
   }

   protected boolean meleeHit(@Nullable LivingEntity target, float scale, double knockback, double extraReach) {
      if (target != null && target.isAlive() && this.canHarm(target) && this.getSensing().hasLineOfSight(target)) {
         double reach = (double)this.getBbWidth() * 2.0 + (double)target.getBbWidth() + extraReach;
         if (!this.isWithinMeleeAttackRange(target) && this.distanceToSqr(target) > reach * reach) {
            return false;
         } else {
            DamageSource source = this.damageSources().mobAttack(this);
            float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE) * scale;
            if (this.level() instanceof ServerLevel server) {
               damage = EnchantmentHelper.modifyDamage(server, this.getMainHandItem(), target, source, damage);
            }

            boolean hurt = target.hurt(source, damage);
            if (hurt) {
               float yaw = this.getYRot() * (float) (Math.PI / 180.0);
               target.knockback(0.4 + knockback, (double)Mth.sin(yaw), (double)(-Mth.cos(yaw)));
               if (this.level() instanceof ServerLevel server) {
                  EnchantmentHelper.doPostAttackEffects(server, target, source);
               }

               this.setLastHurtMob(target);
               this.playSound(Pirates.CUTLASS_HIT.get(), 0.9F, 0.9F + this.random.nextFloat() * 0.2F);
            }

            return hurt;
         }
      } else {
         return false;
      }
   }

   protected void swingFx(float pitch) {
      this.playSound(Pirates.CUTLASS_SWING.get(), 1.0F, pitch);
      if (this.level() instanceof ServerLevel server) {
         Vec3 f = Vec3.directionFromRotation(0.0F, this.getYRot());
         server.sendParticles(
            ParticleTypes.SWEEP_ATTACK,
            this.getX() + f.x * 1.1 * (double)this.getScale(),
            this.getY() + (double)this.getBbHeight() * 0.55,
            this.getZ() + f.z * 1.1 * (double)this.getScale(),
            1,
            0.0,
            0.0,
            0.0,
            0.0
         );
      }
   }

   public void faceTarget(Entity target, float maxTurn) {
      double dx = target.getX() - this.getX();
      double dz = target.getZ() - this.getZ();
      float yaw = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
      float y = Mth.approachDegrees(this.getYRot(), yaw, maxTurn);
      this.setYRot(y);
      this.setYBodyRot(y);
   }

   public boolean clearShot(LivingEntity target) {
      if ((Boolean)PirateConfig.FRIENDLY_FIRE.get()) {
         return true;
      } else {
         Vec3 from = this.getEyePosition();
         Vec3 to = target.getEyePosition();

         for (FactionFighter ally : this.level()
            .getEntitiesOfClass(FactionFighter.class, new AABB(from, to).inflate(1.0), a -> a != this && this.isAlliedTo(a))) {
            if (ally.getBoundingBox().inflate(0.3).clip(from, to).isPresent()) {
               return false;
            }
         }

         return true;
      }
   }

   protected void shootAt(LivingEntity target, float damage, float inaccuracy, boolean musket, boolean offhand) {
      Vec3 look = Vec3.directionFromRotation(this.getXRot(), this.getYHeadRot());
      Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize().scale(offhand ? -0.32 : 0.32);
      Vec3 muzzle = this.getEyePosition().add(look.scale(0.7 * (double)this.getScale())).add(right).add(0.0, -0.25 * (double)this.getScale(), 0.0);
      double dx = target.getX() - muzzle.x;
      double dz = target.getZ() - muzzle.z;
      double dy = target.getY(0.6) - muzzle.y;
      float speed = (float)((Double)PirateConfig.BALL_SPEED.get() * (musket ? 1.0 : 0.85));
      double flight = Math.sqrt(dx * dx + dz * dz) / (double)speed;
      dy += 0.003 * flight * flight;
      MusketBall.fire(this, muzzle, new Vec3(dx, dy, dz), speed, inaccuracy, damage, ((Double)PirateConfig.ARMOR_PIERCING.get()).floatValue(), musket);
   }

   protected abstract SoundEvent ambient();

   protected abstract SoundEvent hurt();

   protected abstract SoundEvent death();

   protected SoundEvent getAmbientSound() {
      return this.ambient();
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return this.hurt();
   }

   protected SoundEvent getDeathSound() {
      return this.death();
   }
}
