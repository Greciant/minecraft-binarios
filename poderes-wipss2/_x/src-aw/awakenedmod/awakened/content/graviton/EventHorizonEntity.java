package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.vfx.AbstractFxEntity;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EventHorizonEntity extends AbstractFxEntity {
   private static final EntityDataAccessor<Integer> DATA_COLLAPSE_AT = SynchedEntityData.defineId(EventHorizonEntity.class, EntityDataSerializers.INT);
   private static final String KEY_RUMBLE = "graviton.horizon.rumble";
   private static final String KEY_WHINE = "graviton.horizon.whine";
   private static final float RUMBLE_VOLUME = 1.0F;
   private static final float WHINE_VOLUME = 1.0F;
   private int born;
   private int sustainTicks;
   private float pullRadius = 16.0F;
   private float groundDrop = 4.0F;
   private float yaw;
   private float tilt;
   private float roll;
   private Vec3[] frame;
   private boolean opened;
   private boolean boomed;

   public EventHorizonEntity(EntityType<? extends EventHorizonEntity> type, Level level) {
      super(type, level);
   }

   void configure(int bornGameTime, int sustainTicks, float pullRadius, float groundDrop, float yaw, float tilt, float roll) {
      this.born = bornGameTime;
      this.sustainTicks = sustainTicks;
      this.pullRadius = pullRadius;
      this.groundDrop = groundDrop;
      this.yaw = yaw;
      this.tilt = tilt;
      this.roll = roll;
      this.frame = null;
      this.lifetime = EventHorizonVisuals.totalTicks(sustainTicks);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_COLLAPSE_AT, -1);
   }

   public float age(float partialTick) {
      return (float)((int)this.level().getGameTime() - this.born) + partialTick;
   }

   public int collapseAt() {
      return (Integer)this.entityData.get(DATA_COLLAPSE_AT);
   }

   public EventHorizonVisuals.Phase phase() {
      return this.phase(0.0F);
   }

   public EventHorizonVisuals.Phase phase(float partialTick) {
      return EventHorizonVisuals.phase(this.age(partialTick), this.sustainTicks, this.collapseAt());
   }

   public void collapseNow() {
      if (!this.level().isClientSide() && this.phase().holding()) {
         int age = Math.max(20, (int)this.age(0.0F));
         this.entityData.set(DATA_COLLAPSE_AT, age);
         this.lifetime = age + 16 + 18;
      }
   }

   public int sustainTicks() {
      return this.sustainTicks;
   }

   public float pullRadius() {
      return this.pullRadius;
   }

   public float groundDrop() {
      return this.groundDrop;
   }

   public float yaw() {
      return this.yaw;
   }

   public Vec3[] diskFrame() {
      if (this.frame == null) {
         this.frame = EventHorizonVisuals.diskFrame(this.yaw, this.tilt, this.roll);
      }

      return this.frame;
   }

   public Vec3 diskNormal() {
      return this.diskFrame()[2];
   }

   @Override
   public void tick() {
      this.baseTick();
      if (this.level() instanceof ServerLevel server) {
         EventHorizonVisuals.Phase p = this.phase();
         this.sound(server, p);
         if (p.done()) {
            this.discard();
         }
      }
   }

   private void sound(ServerLevel server, EventHorizonVisuals.Phase p) {
      Vec3 at = this.position();
      if (!this.opened) {
         this.opened = true;
         Fx.sound(server, at, (SoundEvent)AwakenedSounds.HORIZON_OPEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      }

      if (!p.holding() && !(p.collapse() < 1.0F)) {
         LoopSound.stop(this, "graviton.horizon.rumble");
      } else {
         float vol = 1.0F * (0.35F + 0.65F * p.open()) * (0.8F + 0.2F * p.sustain());
         float pitch = 0.9F + 0.1F * p.sustain() + 0.35F * p.collapse();
         LoopSound.keep(
            this, "graviton.horizon.rumble", (SoundEvent)AwakenedSounds.HORIZON_RUMBLE.get(), SoundSource.PLAYERS, vol * (1.0F - 0.5F * p.collapse()), pitch
         );
      }

      if (p.whine() > 0.0F && p.collapse() < 1.0F) {
         float w = p.whine();
         LoopSound.keep(
            this,
            "graviton.horizon.whine",
            (SoundEvent)AwakenedSounds.HORIZON_WHINE.get(),
            SoundSource.PLAYERS,
            1.0F * (0.15F + 0.85F * w) * (1.0F - 0.6F * p.collapse()),
            0.6F + 1.2F * w * w
         );
      } else if (p.collapse() >= 1.0F) {
         LoopSound.stop(this, "graviton.horizon.whine");
      }

      if (p.collapse() >= 1.0F && !this.boomed) {
         this.boomed = true;
         Fx.sound(server, at, (SoundEvent)AwakenedSounds.HORIZON_COLLAPSE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      }
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeInt(this.born);
      buf.writeVarInt(this.sustainTicks);
      buf.writeFloat(this.pullRadius);
      buf.writeFloat(this.groundDrop);
      buf.writeFloat(this.yaw);
      buf.writeFloat(this.tilt);
      buf.writeFloat(this.roll);
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.born = buf.readInt();
      this.sustainTicks = buf.readVarInt();
      this.pullRadius = buf.readFloat();
      this.groundDrop = buf.readFloat();
      this.yaw = buf.readFloat();
      this.tilt = buf.readFloat();
      this.roll = buf.readFloat();
      this.frame = null;
   }
}
