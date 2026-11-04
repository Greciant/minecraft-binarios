package com.devilfruits.fruits.kage_kage.entity;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.fruits.kage_kage.KageEntities;
import com.devilfruits.fruits.kage_kage.KageFx;
import com.devilfruits.fruits.kage_kage.KageItems;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageTriggers;
import com.devilfruits.fruits.kage_kage.shadow.ShadowVictims;
import com.devilfruits.fruits.kage_kage.shadow.Shadows;
import com.devilfruits.fruits.kage_kage.shadow.StolenShadow;
import java.lang.ref.WeakReference;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class HollowCorpse extends Entity {
   public static final int RAISE_AFTER = 24;
   private static final EntityDataAccessor<Boolean> DATA_INJECTED = SynchedEntityData.defineId(HollowCorpse.class, EntityDataSerializers.BOOLEAN);
   @Nullable
   private StolenShadow pending;
   @Nullable
   private UUID injector;
   private WeakReference<ServerPlayer> injectorRef = new WeakReference<>(null);
   private int twitchTicks;

   public HollowCorpse(EntityType<? extends HollowCorpse> type, Level level) {
      super(type, level);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_INJECTED, false);
   }

   public boolean injected() {
      return (Boolean)this.entityData.get(DATA_INJECTED);
   }

   public int twitchTicks() {
      return this.twitchTicks;
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (DATA_INJECTED.equals(key)) {
         this.twitchTicks = 0;
      }
   }

   @Nullable
   public static Component refusal(ServerPlayer user) {
      if (!KageKage.isUser(user)) {
         return Component.translatable("message.devilfruits.kage_kage.zombie.not_user");
      } else {
         int max = (int)KageKage.setting("maxZombies");
         if (ShadowVictims.get(user.server).zombies(user.getUUID()) >= max) {
            return Component.translatable("message.devilfruits.kage_kage.zombie.crew_full", new Object[]{max});
         } else {
            return Shadows.count(user) == 0 ? Component.translatable("message.devilfruits.kage_kage.zombie.no_shadows") : null;
         }
      }
   }

   public boolean inject(ServerPlayer user) {
      if (this.injected()) {
         return false;
      } else {
         Component why = refusal(user);
         if (why != null) {
            user.displayClientMessage(why, true);
            return false;
         } else {
            StolenShadow shadow = Shadows.spendStrongest(user);
            if (shadow == null) {
               return false;
            } else {
               this.pending = shadow;
               this.injector = user.getUUID();
               this.injectorRef = new WeakReference<>(user);
               this.entityData.set(DATA_INJECTED, true);
               this.twitchTicks = 0;
               ServerFx.send(user, KageFx.ZOMBIE_INJECT, this.position(), Vec3.ZERO, 1.0F, user.getRandom().nextLong(), this.getId());
               this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.6F, 0.6F);
               user.displayClientMessage(Component.translatable("message.devilfruits.kage_kage.zombie.injected", new Object[]{shadow.name()}), true);
               return true;
            }
         }
      }
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (this.injected() || hand != InteractionHand.MAIN_HAND) {
         return InteractionResult.PASS;
      } else if (player instanceof ServerPlayer user) {
         return this.inject(user) ? InteractionResult.CONSUME : InteractionResult.FAIL;
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide() && !this.injected() && !this.isRemoved()) {
         if (source.getEntity() instanceof Player p) {
            if (!p.getAbilities().instabuild) {
               this.spawnAtLocation(new ItemStack((ItemLike)KageItems.HOLLOW_CORPSE.get()));
            }

            this.discard();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public boolean isPushable() {
      return false;
   }

   public void tick() {
      super.tick();
      if (!this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0).scale(0.98));
         this.move(MoverType.SELF, this.getDeltaMovement());
      } else {
         this.setDeltaMovement(Vec3.ZERO);
      }

      if (this.injected()) {
         this.twitchTicks++;
         if (!this.level().isClientSide() && this.twitchTicks >= 24) {
            this.raise();
         }
      }
   }

   @Nullable
   private ServerPlayer injector() {
      if (this.injector != null && this.level() instanceof ServerLevel server) {
         ServerPlayer p = server.getServer().getPlayerList().getPlayer(this.injector);
         if (p != null) {
            return p;
         } else {
            ServerPlayer ref = this.injectorRef.get();
            return ref != null && !ref.isRemoved() && ref.getUUID().equals(this.injector) ? ref : null;
         }
      } else {
         return null;
      }
   }

   private void raise() {
      ServerLevel level = (ServerLevel)this.level();
      ServerPlayer owner = this.injector();
      if (this.pending != null && owner != null) {
         ShadowZombie z = (ShadowZombie)KageEntities.SHADOW_ZOMBIE.get().create(level);
         if (z != null) {
            z.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            z.setYBodyRot(this.getYRot());
            z.setYHeadRot(this.getYRot());
            z.raise(owner, this.pending);
            level.addFreshEntity(z);
            KageTriggers.ZOMBIE_RAISED.get().trigger(owner);
            if (ShadowVictims.get(level.getServer()).zombies(owner.getUUID()) >= (int)KageKage.setting("maxZombies")) {
               KageTriggers.FULL_CREW.get().trigger(owner);
            }

            this.discard();
         }
      } else {
         if (this.pending != null) {
            Shadows.returnHome(level.getServer(), this.pending.victim());
         }

         this.pending = null;
         this.entityData.set(DATA_INJECTED, false);
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.injector = tag.hasUUID("Injector") ? tag.getUUID("Injector") : null;
      this.pending = tag.contains("Pending") ? (StolenShadow)StolenShadow.CODEC.parse(NbtOps.INSTANCE, tag.get("Pending")).result().orElse(null) : null;
      this.entityData.set(DATA_INJECTED, this.pending != null);
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.injector != null) {
         tag.putUUID("Injector", this.injector);
      }

      if (this.pending != null) {
         StolenShadow.CODEC.encodeStart(NbtOps.INSTANCE, this.pending).result().ifPresent(t -> tag.put("Pending", t));
      }
   }

   public boolean shouldBeSaved() {
      return !this.isRemoved();
   }
}
