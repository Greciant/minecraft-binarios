package com.awakenedmod.awakened.core.npc;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.entity.ai.GatedGoal;
import com.awakenedmod.awakened.core.AwakenedThreat;
import com.awakenedmod.awakened.core.PowerSuppression;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class PoweredNpc extends Monster implements Enemy, AwakenedThreat, ClipBearer {
   public static final int MIN_MASTERY = 3;
   public static final double SIGHT_RANGE = 24.0;
   private static final EntityDataAccessor<Integer> DATA_MASTERY = SynchedEntityData.defineId(PoweredNpc.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Byte> DATA_CLIP = SynchedEntityData.defineId(PoweredNpc.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Integer> DATA_CLIP_SEQ = SynchedEntityData.defineId(PoweredNpc.class, EntityDataSerializers.INT);
   private final NpcKit kit;
   private final NpcCaster caster = new NpcCaster(this);
   private final NpcStamina stamina = new NpcStamina();
   private final Map<String, Float> vars = new HashMap<>();
   private final NpcBrain brain;
   private final NpcCarriage carriage = new NpcCarriage(this);
   private int clipStartTick;
   private int aimedTick = Integer.MIN_VALUE;

   public PoweredNpc(EntityType<? extends PoweredNpc> type, Level level, NpcKit kit) {
      super(type, level);
      this.kit = kit;
      this.xpReward = 20;
      this.lookControl = new NpcCarriage.Look(this, this.carriage);
      this.moveControl = new NpcCarriage.Move(this, this.carriage);
      this.brain = new NpcBrain(this, kit.createMoves(), kit.createTactics());
      if (kit.avoidsWater()) {
         this.setPathfindingMalus(PathType.WATER, -1.0F);
         this.setPathfindingMalus(PathType.WATER_BORDER, 8.0F);
      }
   }

   public NpcKit kit() {
      return this.kit;
   }

   public NpcCaster caster() {
      return this.caster;
   }

   public NpcStamina stamina() {
      return this.stamina;
   }

   public NpcBrain npcBrain() {
      return this.brain;
   }

   public int mastery() {
      return (Integer)this.entityData.get(DATA_MASTERY);
   }

   public void setMastery(int mastery) {
      this.entityData.set(DATA_MASTERY, Math.max(3, mastery));
   }

   public float var(String key, float fallback) {
      Float v = this.vars.get(key);
      return v == null ? fallback : v;
   }

   public void setVar(String key, float value) {
      this.vars.put(key, value);
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_MASTERY, 3);
      builder.define(DATA_CLIP, (byte)0);
      builder.define(DATA_CLIP_SEQ, 0);
   }

   public void showClip(ResourceLocation clip) {
      int index = this.kit.clips().indexOf(clip);
      if (index < 0) {
         throw new IllegalArgumentException("Clip " + clip + " is not in kit " + this.kit.id());
      } else {
         this.entityData.set(DATA_CLIP, (byte)(index + 1));
         this.entityData.set(DATA_CLIP_SEQ, (Integer)this.entityData.get(DATA_CLIP_SEQ) + 1);
         this.clipStartTick = this.tickCount;
      }
   }

   public void clearClip() {
      this.entityData.set(DATA_CLIP, (byte)0);
   }

   public boolean isCasting() {
      return (Byte)this.entityData.get(DATA_CLIP) != 0;
   }

   @Nullable
   @Override
   public ResourceLocation activeClip() {
      int index = (Byte)this.entityData.get(DATA_CLIP) - 1;
      List<ResourceLocation> clips = this.kit.clips();
      return index >= 0 && index < clips.size() ? clips.get(index) : null;
   }

   @Override
   public int clipSequence() {
      return (Integer)this.entityData.get(DATA_CLIP_SEQ);
   }

   @Override
   public float clipAge(float partialTick) {
      return this.isCasting() ? (float)(this.tickCount - this.clipStartTick) + partialTick : 0.0F;
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (DATA_CLIP_SEQ.equals(key)) {
         this.clipStartTick = this.tickCount;
      }
   }

   public void aimAt(Vec3 point, float maxTurnDeg) {
      Vec3 eye = this.getEyePosition();
      double dx = point.x - eye.x;
      double dy = point.y - eye.y;
      double dz = point.z - eye.z;
      double flat = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
      float pitch = (float)(-(Mth.atan2(dy, flat) * 180.0F / (float)Math.PI));
      this.setYHeadRot(Mth.approachDegrees(this.getYHeadRot(), yaw, maxTurnDeg));
      this.setXRot(Mth.approach(this.getXRot(), pitch, maxTurnDeg));
      this.aimedTick = this.tickCount;
   }

   public void tick() {
      super.tick();
      if (this.isCasting()) {
         this.setYRot(this.getYHeadRot());
         this.yRotO = this.yHeadRotO;
      }
   }

   boolean aimedThisTick() {
      return this.aimedTick == this.tickCount;
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(5, new NpcRoamGoal(this));
      this.goalSelector.addGoal(6, new GatedGoal(new LookAtPlayerGoal(this, Player.class, 24.0F), () -> this.getTarget() == null));
      this.goalSelector.addGoal(7, new GatedGoal(new RandomLookAroundGoal(this), () -> this.getTarget() == null));
      this.targetSelector.addGoal(1, new NpcHurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true, e -> e.distanceToSqr(this) <= 576.0).setUnseenMemoryTicks(600));
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      NpcObserver watching = NpcBrain.observer();
      long began = watching == null ? 0L : System.nanoTime();
      if (!PowerSuppression.test(this)) {
         this.kit.passiveTick(this.caster);
      }

      if (this.isAlive()) {
         this.stamina.regenTick(level.getGameTime());
         this.brain.tick(level);
         if (watching != null) {
            watching.tickCost(this, System.nanoTime() - began);
         }
      }
   }

   protected BodyRotationControl createBodyControl() {
      return new NpcCarriage.Body(this);
   }

   public boolean removeWhenFarAway(double distanceToClosestPlayerSqr) {
      int far = this.getType().getCategory().getDespawnDistance();
      return this.getTarget() == null && distanceToClosestPlayerSqr > (double)far * (double)far;
   }

   public void die(DamageSource cause) {
      if (!this.level().isClientSide()) {
         this.brain.interrupt();
      }

      super.die(cause);
   }

   public void onRemovedFromLevel() {
      if (!this.level().isClientSide()) {
         this.brain.interrupt();
         this.kit.onRemoved(this.caster);
      }

      super.onRemovedFromLevel();
   }

   protected Component getTypeName() {
      return Component.translatable("entity.awakened.powered_npc.titled", new Object[]{this.kit.powerName(), this.mastery()});
   }

   public boolean shouldShowName() {
      return true;
   }

   @Override
   public double bonusMastery() {
      return (20.0 + 8.0 * (double)this.mastery()) * NpcConfig.masteryMultiplier();
   }

   @Override
   public double bonusXp() {
      return (10.0 + 4.0 * (double)this.mastery()) * NpcConfig.xpMultiplier();
   }

   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, source, recentlyHit);
      if (recentlyHit && this.random.nextDouble() < NpcConfig.catalystChance(this.mastery())) {
         this.spawnAtLocation((ItemLike)AwakenedItems.CATALYST.get());
      }
   }

   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
      this.setMastery(NpcMasteryRoll.roll(this.random, level.getLevel().getGameTime()));
      return super.finalizeSpawn(level, difficulty, reason, data);
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("Mastery", this.mastery());
      tag.putFloat("Stamina", this.stamina.current());
      CompoundTag saved = new CompoundTag();
      this.vars.forEach(saved::putFloat);
      tag.put("Vars", saved);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setMastery(tag.contains("Mastery") ? tag.getInt("Mastery") : 3);
      this.stamina.load(tag.contains("Stamina") ? tag.getFloat("Stamina") : this.stamina.max());
      this.vars.clear();
      CompoundTag saved = tag.getCompound("Vars");

      for (String key : saved.getAllKeys()) {
         this.vars.put(key, saved.getFloat(key));
      }
   }
}
