package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.Fx;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SoulOrbEntity extends Entity {
   private static final int PICKUP_DELAY = 12;
   private static final double HOME_RANGE = 7.0;
   private static final double BANK_DISTANCE = 1.1;
   private static final int MAX_AGE = 6000;
   private static final UUID NIL = new UUID(0L, 0L);
   private UUID owner = NIL;
   private String soulType = "";
   private int pickupDelay = 12;

   public SoulOrbEntity(EntityType<? extends SoulOrbEntity> type, Level level) {
      super(type, level);
   }

   public void configure(UUID owner, String soulType) {
      this.owner = owner != null ? owner : NIL;
      this.soulType = soulType == null ? "" : soulType;
   }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel server) {
         if (this.tickCount < 6000 && !this.soulType.isEmpty()) {
            if (this.pickupDelay > 0) {
               this.pickupDelay--;
            }

            Vec3 v = this.getDeltaMovement();
            if (!this.onGround()) {
               v = v.add(0.0, -0.03, 0.0);
            }

            ServerPlayer ownerPlayer = server.getServer().getPlayerList().getPlayer(this.owner);
            if (this.pickupDelay == 0 && ownerPlayer != null && ownerPlayer.isAlive()) {
               Vec3 toOwner = ownerPlayer.position().add(0.0, (double)ownerPlayer.getBbHeight() * 0.5, 0.0).subtract(this.position());
               double dist = toOwner.length();
               if (dist <= 1.1) {
                  this.bank(server, ownerPlayer);
                  return;
               }

               if (dist <= 7.0) {
                  Vec3 pull = toOwner.scale(1.0 / Math.max(dist, 0.001)).scale(0.18 + 0.12 * (1.0 - dist / 7.0));
                  v = v.scale(0.6).add(pull);
               }
            }

            this.setDeltaMovement(v.scale(0.92));
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.tickCount % 2 == 0) {
               server.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 0.2, this.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
            }

            if (this.random.nextInt(6) == 0) {
               server.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.15, this.getZ(), 1, 0.05, 0.02, 0.05, 0.01);
            }
         } else {
            this.discard();
         }
      }
   }

   private void bank(ServerLevel server, ServerPlayer ownerPlayer) {
      int stored = SummonAttachments.souls(ownerPlayer).add(this.soulType, 1, SummonSouls.cap(ownerPlayer));
      if (stored <= 0) {
         this.setDeltaMovement(Vec3.ZERO);
         this.pickupDelay = 8;
      } else {
         ownerPlayer.setData(SummonAttachments.SOULS, SummonAttachments.souls(ownerPlayer));
         Fx.sound(server, ownerPlayer.position(), (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.35F, 0.6F);
         server.sendParticles(
            ParticleTypes.SCULK_SOUL,
            ownerPlayer.getX(),
            ownerPlayer.getY() + (double)ownerPlayer.getBbHeight() * 0.6,
            ownerPlayer.getZ(),
            6,
            0.2,
            0.3,
            0.2,
            0.01
         );
         this.discard();
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   protected void defineSynchedData(Builder builder) {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.owner = tag.hasUUID("owner") ? tag.getUUID("owner") : NIL;
      this.soulType = tag.getString("soulType");
      this.pickupDelay = tag.getInt("pickupDelay");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putUUID("owner", this.owner);
      tag.putString("soulType", this.soulType);
      tag.putInt("pickupDelay", this.pickupDelay);
   }
}
