package com.devilfruits.core.npc;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

public abstract class FactionFighter extends Monster {
   private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(FactionFighter.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(FactionFighter.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ACTION_SEQ = SynchedEntityData.defineId(FactionFighter.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(FactionFighter.class, EntityDataSerializers.BYTE);
   public static final int FLAG_ARMED = 1;
   public static final int FLAG_AIMING = 2;
   public static final int FLAG_ENRAGED = 4;
   public static final int FLAG_LOADED = 8;
   public static final int DEATH_TICKS = 30;
   public final AnimData anim = new AnimData();
   private int action = -1;
   private int actionTick;
   private int actionLength;
   private int armedTimer;
   private int armedTicks;
   @Nullable
   private UUID leaderId;
   @Nullable
   private FactionFighter leader;
   @Nullable
   private DamageSource pendingLoot;

   protected FactionFighter(EntityType<? extends FactionFighter> type, Level level) {
      super(type, level);
   }

   public abstract Faction faction();

   public abstract int rank();

   protected abstract int variantCount();

   public abstract Locomotion.Speeds locomotionSpeeds();

   public int bounty() {
      return 0;
   }

   public int getHakiLevel() {
      return 0;
   }

   public Vec3 getGroundSpeedRelativeToSupport() {
      return new Vec3(this.getX() - this.xo, 0.0, this.getZ() - this.zo).scale(20.0);
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_VARIANT, 0);
      builder.define(DATA_ACTION, -1);
      builder.define(DATA_ACTION_SEQ, 0);
      builder.define(DATA_FLAGS, (byte)0);
   }

   public int variant() {
      return (Integer)this.entityData.get(DATA_VARIANT);
   }

   public void setVariant(int variant) {
      this.entityData.set(DATA_VARIANT, Math.floorMod(variant, Math.max(1, this.variantCount())));
   }

   public boolean hasFlag(int flag) {
      return ((Byte)this.entityData.get(DATA_FLAGS) & flag) != 0;
   }

   public void setFlag(int flag, boolean on) {
      byte b = (Byte)this.entityData.get(DATA_FLAGS);
      byte next = (byte)(on ? b | flag : b & ~flag);
      if (next != b) {
         this.entityData.set(DATA_FLAGS, next);
      }
   }

   public int armedTicks() {
      return this.hasFlag(1) ? this.armedTicks : 0;
   }

   public void stayArmed(int ticks) {
      this.armedTimer = Math.max(this.armedTimer, ticks);
   }

   public void playAction(int id, int length) {
      this.action = id;
      this.actionTick = 0;
      this.actionLength = Math.max(1, length);
      this.stayArmed(60);
      this.entityData.set(DATA_ACTION, id);
      this.entityData.set(DATA_ACTION_SEQ, (Integer)this.entityData.get(DATA_ACTION_SEQ) + 1);
   }

   public boolean isActing() {
      return this.action >= 0;
   }

   public int currentAction() {
      return this.action;
   }

   public int actionTick() {
      return this.actionTick;
   }

   public int syncedAction() {
      return (Integer)this.entityData.get(DATA_ACTION);
   }

   public int actionSeq() {
      return (Integer)this.entityData.get(DATA_ACTION_SEQ);
   }

   protected void onActionTick(int id, int tick) {
   }

   protected void onActionEnd(int id) {
   }

   @Nullable
   public FactionFighter leader() {
      if (this.leaderId == null) {
         return null;
      } else {
         if ((this.leader == null || !this.leader.isAlive() || this.tickCount % 40 == 0) && this.level() instanceof ServerLevel server) {
            FactionFighter var10001;
            label29: {
               if (server.getEntity(this.leaderId) instanceof FactionFighter f && f.isAlive()) {
                  var10001 = f;
                  break label29;
               }

               var10001 = null;
            }

            this.leader = var10001;
         }

         return this.leader != null && this.leader.isAlive() ? this.leader : null;
      }
   }

   public void setLeader(@Nullable FactionFighter leader) {
      this.leader = leader;
      this.leaderId = leader == null ? null : leader.getUUID();
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         this.anim.update(this, this.locomotionSpeeds());
      } else {
         if (this.action >= 0) {
            int id = this.action;
            this.onActionTick(id, this.actionTick);
            if (this.action == id && ++this.actionTick >= this.actionLength) {
               this.action = -1;
               this.onActionEnd(id);
            }
         }

         if (this.getTarget() != null || this.action >= 0 || this.hasFlag(2)) {
            this.stayArmed(60);
         }

         if (this.armedTimer > 0) {
            this.armedTimer--;
         }

         boolean armed = this.armedTimer > 0 && this.isAlive();
         this.armedTicks = armed ? this.armedTicks + 1 : 0;
         this.setFlag(1, armed);
      }
   }

   public boolean isAlliedTo(Entity other) {
      if (other instanceof FactionFighter f && f.faction() == this.faction()) {
         return true;
      }

      return super.isAlliedTo(other);
   }

   public boolean canHarm(LivingEntity e) {
      return e != this && e.isAlive() && !this.isAlliedTo(e) && (!(e instanceof FactionFighter f) || this.faction().hostileTo(f.faction()));
   }

   public void die(DamageSource source) {
      boolean first = !this.dead && !this.isRemoved();
      super.die(source);
      if (first && !this.level().isClientSide()) {
         NeoForge.EVENT_BUS.post(new FactionMemberKilledEvent(this, source.getEntity(), source));
      }

      this.action = -1;
   }

   protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
      if (this.deathTime >= 29) {
         super.dropAllDeathLoot(level, source);
      } else {
         this.pendingLoot = source;
      }
   }

   protected void tickDeath() {
      this.deathTime++;
      if (this.deathTime >= 30 && !this.level().isClientSide() && !this.isRemoved()) {
         if (this.pendingLoot != null && this.level() instanceof ServerLevel server) {
            DamageSource source = this.pendingLoot;
            this.pendingLoot = null;
            super.dropAllDeathLoot(server, source);
         }

         this.level().broadcastEntityEvent(this, (byte)60);
         this.remove(RemovalReason.KILLED);
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("Variant", this.variant());
      tag.putByte("FighterFlags", (byte)((Byte)this.entityData.get(DATA_FLAGS) & 12));
      if (this.leaderId != null) {
         tag.putUUID("Leader", this.leaderId);
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setVariant(tag.getInt("Variant"));
      this.entityData.set(DATA_FLAGS, tag.getByte("FighterFlags"));
      this.leaderId = tag.hasUUID("Leader") ? tag.getUUID("Leader") : null;
   }
}
