package com.awakenedmod.awakened.content.entity.boss;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.content.entity.TelegraphedAoe;
import com.awakenedmod.awakened.core.AwakenedThreat;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public abstract class AwakenedBossEntity extends Monster implements Enemy, AwakenedThreat {
   public static final byte ACTION_NONE = 0;
   private static final EntityDataAccessor<Byte> DATA_ESCALATION = SynchedEntityData.defineId(AwakenedBossEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(AwakenedBossEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Float> DATA_ACTION_PROGRESS = SynchedEntityData.defineId(AwakenedBossEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Byte> DATA_MOVE_WINDUP = SynchedEntityData.defineId(AwakenedBossEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> DATA_MOVE_RECOVER = SynchedEntityData.defineId(AwakenedBossEntity.class, EntityDataSerializers.BYTE);
   public static final float[] ESCALATION_GATES = new float[]{0.66F, 0.33F};
   protected final ServerBossEvent bossEvent;
   protected int escalation = 1;
   @Nullable
   private MoveScheduler<?> moveScheduler;
   private final List<TelegraphedAoe.Zone> aoeZones = new ArrayList<>();
   private int hiddenTicks;
   @Nullable
   private LivingEntity hiddenTarget;
   private int destroyBlocksTick;
   private int lastDeflectMsgTick = -100;
   private boolean lootReleased;
   @Nullable
   private DamageSource pendingLootSource;
   private boolean pendingLootFromAbility;
   @Nullable
   private Player savedKiller;
   private int savedKillerMemory;

   protected AwakenedBossEntity(EntityType<? extends Monster> type, Level level, Component name, BossBarColor color, BossBarOverlay overlay) {
      super(type, level);
      this.bossEvent = new ServerBossEvent(name, color, overlay);
   }

   protected abstract BossDefenses.Profile defenseProfile();

   protected abstract void onEscalate(ServerLevel var1, int var2);

   protected boolean suppressEscalationJuice() {
      return false;
   }

   protected void onTargetHidden(ServerLevel level, LivingEntity target) {
   }

   protected boolean bossHurt(ServerLevel level, DamageSource source, float amount) {
      return super.hurt(source, amount);
   }

   protected int deathDuration() {
      return 20;
   }

   protected void deathBeat(ServerLevel level, int deathTime) {
   }

   protected void dropCinematicLoot(ServerLevel level) {
   }

   protected void onDeathSequenceStart() {
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_ESCALATION, (byte)1);
      builder.define(DATA_ACTION, (byte)0);
      builder.define(DATA_ACTION_PROGRESS, 0.0F);
      builder.define(DATA_MOVE_WINDUP, (byte)0);
      builder.define(DATA_MOVE_RECOVER, (byte)0);
   }

   public final byte getAction() {
      return (Byte)this.entityData.get(DATA_ACTION);
   }

   public final void setAction(byte action) {
      this.entityData.set(DATA_ACTION, action);
   }

   public final float getActionProgress() {
      return (Float)this.entityData.get(DATA_ACTION_PROGRESS);
   }

   protected final void setActionProgress(float progress) {
      this.entityData.set(DATA_ACTION_PROGRESS, progress);
   }

   public final byte getMoveWindup() {
      return (Byte)this.entityData.get(DATA_MOVE_WINDUP);
   }

   public final byte getMoveRecover() {
      return (Byte)this.entityData.get(DATA_MOVE_RECOVER);
   }

   protected final void setMoveBudget(int windupTicks, int recoverTicks) {
      this.entityData.set(DATA_MOVE_WINDUP, (byte)Math.min(127, Math.max(0, windupTicks)));
      this.entityData.set(DATA_MOVE_RECOVER, (byte)Math.min(127, Math.max(0, recoverTicks)));
   }

   final void attachScheduler(MoveScheduler<?> scheduler) {
      this.moveScheduler = scheduler;
   }

   public final boolean aiMovementSuppressed() {
      return this.moveScheduler != null && this.moveScheduler.suppressesMovement();
   }

   @Nullable
   public final Vec3 committedAim() {
      return this.moveScheduler != null ? this.moveScheduler.committedAim() : null;
   }

   @Nullable
   public final Vec3 moveAnchor() {
      return this.moveScheduler != null ? this.moveScheduler.moveAnchor() : null;
   }

   public final void steerCommittedFacing(float yawDeg) {
      if (this.moveScheduler != null) {
         this.moveScheduler.steerLockedFacing(yawDeg);
      }
   }

   public final boolean latchMoveEvent() {
      return this.moveScheduler != null && this.moveScheduler.latchMoveEvent();
   }

   public final int moveEventTick() {
      return this.moveScheduler != null ? this.moveScheduler.moveEventTick() : -1;
   }

   public final int currentWindup() {
      return this.moveScheduler != null ? this.moveScheduler.currentWindup() : 0;
   }

   public final int currentTotal() {
      return this.moveScheduler != null ? this.moveScheduler.currentTotal() : 0;
   }

   public final int currentMoveTick() {
      return this.moveScheduler != null ? this.moveScheduler.currentMoveTick() : 0;
   }

   public final boolean isInRecover() {
      return this.moveScheduler != null && this.moveScheduler.isInRecover();
   }

   public final void addAoe(TelegraphedAoe.Zone zone) {
      this.aoeZones.add(zone);
   }

   public final byte getEscalation() {
      return (Byte)this.entityData.get(DATA_ESCALATION);
   }

   public final int escalationPhase() {
      return this.escalation;
   }

   public float scaleDamage(float base) {
      return base * (1.0F + (float)(this.escalation - 1) * 0.25F);
   }

   public void aiStep() {
      super.aiStep();
      if (this.level() instanceof ServerLevel server) {
         this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
         TelegraphedAoe.tick(server, this, this.aoeZones);
         if (!this.isDeadOrDying()) {
            this.updateEscalation(server);
            Fx.wearVent(server, this, this.escalation);
            this.tickHidden(server);
            if (this.destroyBlocksTick > 0 && --this.destroyBlocksTick <= 0) {
               BossDefenses.blockBreakTick(server, this);
            }
         }
      }
   }

   public final float nextGateHealth() {
      float max = this.getMaxHealth();

      for (int i = 0; i < ESCALATION_GATES.length; i++) {
         if (i + 2 > this.escalation) {
            float at = max * ESCALATION_GATES[i];
            if (this.getHealth() > at) {
               return at;
            }
         }
      }

      return 0.0F;
   }

   protected final void updateEscalation(ServerLevel level) {
      float frac = this.getHealth() / this.getMaxHealth();
      int tier = 1;

      for (float gate : ESCALATION_GATES) {
         if (frac <= gate) {
            tier++;
         }
      }

      if (tier > this.escalation) {
         this.escalation = tier;
         this.entityData.set(DATA_ESCALATION, (byte)this.escalation);
         if (!this.suppressEscalationJuice()) {
            Fx.tierCrack(level, this);
            Fx.shake(level, this.position(), 30.0, 0.4F, 10, 40);
         }

         this.onEscalate(level, this.escalation);
      }
   }

   private void tickHidden(ServerLevel level) {
      LivingEntity target = this.getTarget();
      if (target != this.hiddenTarget) {
         this.hiddenTarget = target;
         this.hiddenTicks = 0;
      }

      if (target != null) {
         boolean hidden = !this.hasLineOfSight(target) || target.getY() <= this.getY() - 3.0;
         if (!hidden) {
            this.hiddenTicks = 0;
         } else {
            if (++this.hiddenTicks >= this.defenseProfile().hiddenGraceTicks()) {
               this.onTargetHidden(level, target);
               this.hiddenTicks = this.defenseProfile().hiddenGraceTicks() / 2;
            }
         }
      }
   }

   public final boolean hurt(DamageSource source, float amount) {
      if (this.level() instanceof ServerLevel level) {
         if (this.isDeadOrDying()) {
            return this.bossHurt(level, source, amount);
         } else {
            if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
               BossDefenses.Profile profile = this.defenseProfile();
               if ((Boolean)Config.BOSS_RANGE_IMMUNITY.get() && BossDefenses.isSnipe(this, source, profile.sniperRange())) {
                  this.deflectWithHint(level, source.getEntity());
                  return false;
               }

               if ((Boolean)Config.BOSS_DAMAGE_CAP.get()) {
                  amount = Math.min(amount, profile.perHitCap());
               }

               if (this.hiddenTicks > 0 && (Boolean)Config.BOSS_BLOCK_BREAKING.get() && EventHooks.canEntityGrief(level, this)) {
                  this.destroyBlocksTick = 20;
               }
            }

            return this.bossHurt(level, source, amount);
         }
      } else {
         return super.hurt(source, amount);
      }
   }

   protected final void deflectWithHint(ServerLevel level, @Nullable Entity attacker) {
      boolean hint = attacker instanceof ServerPlayer && this.tickCount - this.lastDeflectMsgTick >= 30;
      if (hint) {
         this.lastDeflectMsgTick = this.tickCount;
      }

      BossDefenses.deflectFx(level, this, hint ? attacker : null);
   }

   public void die(DamageSource cause) {
      boolean wasAlive = this.isAlive();
      super.die(cause);
      if (!this.level().isClientSide() && wasAlive && !this.isAlive()) {
         this.savedKiller = this.lastHurtByPlayer;
         this.savedKillerMemory = this.lastHurtByPlayerTime;
         if (this.level() instanceof ServerLevel server) {
            Fx.shake(server, this.position(), 35.0, 0.5F, 10, 50);
         }

         this.onDeathSequenceStart();
      }
   }

   protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
      if (!this.lootReleased) {
         this.pendingLootSource = source;
         this.pendingLootFromAbility = Dmg.inAbilityHit();
      } else {
         super.dropAllDeathLoot(level, source);
      }
   }

   protected void tickDeath() {
      this.deathTime++;
      this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
      if (this.level() instanceof ServerLevel server) {
         boolean cinematic = (Boolean)Config.BOSS_DEATH_CINEMATICS.get();
         int duration = cinematic ? this.deathDuration() : 20;
         if (cinematic) {
            this.deathBeat(server, this.deathTime);
         }

         if (this.deathTime >= duration && !this.isRemoved()) {
            this.releaseLoot(server);
            server.broadcastEntityEvent(this, (byte)60);
            this.remove(RemovalReason.KILLED);
         }
      }
   }

   private void releaseLoot(ServerLevel server) {
      if (!this.lootReleased) {
         this.lootReleased = true;
         if (this.savedKiller != null) {
            this.lastHurtByPlayer = this.savedKiller;
            this.lastHurtByPlayerTime = Math.max(1, this.savedKillerMemory);
         }

         DamageSource lootSource = this.pendingLootSource != null ? this.pendingLootSource : this.damageSources().genericKill();
         if (this.pendingLootFromAbility) {
            Dmg.asAbilityHit(() -> this.dropAllDeathLoot(server, lootSource));
         } else {
            this.dropAllDeathLoot(server, lootSource);
         }

         this.dropCinematicLoot(server);
      }
   }

   public void remove(RemovalReason reason) {
      if (!this.lootReleased && this.isDeadOrDying() && this.level() instanceof ServerLevel server) {
         this.releaseLoot(server);
      }

      super.remove(reason);
   }

   @Nullable
   public static ItemEntity bless(@Nullable ItemEntity e) {
      if (e == null) {
         return null;
      } else {
         e.setDeltaMovement(0.0, 0.45, 0.0);
         e.setGlowingTag(true);
         e.setExtendedLifetime();
         e.setDefaultPickUpDelay();
         return e;
      }
   }

   @Nullable
   protected ItemEntity spawnAtFeet(ServerLevel level, ItemStack stack) {
      return this.spawnAtLocation(stack, 0.3F);
   }

   public ItemEntity spawnAtLocation(ItemStack stack, float yOffset) {
      ItemEntity e = super.spawnAtLocation(stack, yOffset);
      return this.lootReleased ? bless(e) : e;
   }

   public void startSeenByPlayer(ServerPlayer player) {
      super.startSeenByPlayer(player);
      this.bossEvent.addPlayer(player);
   }

   public void stopSeenByPlayer(ServerPlayer player) {
      super.stopSeenByPlayer(player);
      this.bossEvent.removePlayer(player);
   }

   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   @Override
   public boolean isBoss() {
      return true;
   }
}
