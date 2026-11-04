package com.devilfruits.fruits.doru_doru.entity;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.AbilityEntity;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruEntities;
import com.devilfruits.fruits.doru_doru.DoruFx;
import com.devilfruits.fruits.doru_doru.DoruSounds;
import com.devilfruits.fruits.doru_doru.atelier.TechniqueGen;
import com.devilfruits.fruits.doru_doru.atelier.VoxelSet;
import com.devilfruits.fruits.doru_doru.coating.WaxCoating;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.devilfruits.fruits.doru_doru.wax.WaxOwners;
import com.devilfruits.fruits.doru_doru.wax.WaxWorld;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;

public class WaxLanceEntity extends ThrowableProjectile implements AbilityEntity {
   private static final int MAX_LIFE = 100;
   private float damage = 8.0F;
   private float knockback = 1.2F;
   private int pegLifetime = 1200;
   private int color;
   private int life;

   public WaxLanceEntity(EntityType<? extends WaxLanceEntity> type, Level level) {
      super(type, level);
   }

   public static WaxLanceEntity shoot(ServerPlayer owner, Vec3 from, Vec3 velocity, float damage, float knockback, int pegLifetime, int color) {
      WaxLanceEntity e = new WaxLanceEntity(DoruEntities.LANCE.get(), owner.level());
      e.setOwner(owner);
      e.setPos(from);
      e.setDeltaMovement(velocity);
      e.damage = damage;
      e.knockback = knockback;
      e.pegLifetime = pegLifetime;
      e.color = color;
      owner.level().addFreshEntity(e);
      return e;
   }

   protected void defineSynchedData(Builder builder) {
   }

   protected double getDefaultGravity() {
      return 0.015;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && ++this.life > 100) {
         this.discard();
      }
   }

   protected boolean canHitEntity(Entity target) {
      return !(target instanceof AbilityEntity)
         && super.canHitEntity(target)
         && (!(this.getOwner() instanceof ServerPlayer owner) || target == owner || Targeting.canHurt(owner, target));
   }

   protected void onHitEntity(EntityHitResult hit) {
      super.onHitEntity(hit);
      if (this.level() instanceof ServerLevel level && this.getOwner() instanceof ServerPlayer owner) {
         Entity target = Targeting.resolve(hit.getEntity());
         if (target == owner) {
            return;
         }

         Vec3 dir = this.getDeltaMovement().normalize();
         Targeting.hurt(owner, target, this.damage, DoruDoru.WAX_DAMAGE);
         Targeting.launch(target, dir.scale((double)this.knockback).add(0.0, 0.25, 0.0));
         WaxCoating.add(target, 1, owner);
         level.playSound(
            null, target.getX(), target.getY(), target.getZ(), DoruSounds.LANCE_HIT.get(), SoundSource.PLAYERS, 1.0F, 0.9F + this.random.nextFloat() * 0.2F
         );
         ServerFx.sendNear(level, DoruFx.LANCE_HIT, target.getId(), hit.getLocation(), dir, 1.0F, this.random.nextLong(), this.color, 64.0);
         this.discard();
         return;
      }

      this.discard();
   }

   protected void onHitBlock(BlockHitResult hit) {
      super.onHitBlock(hit);
      if (this.level() instanceof ServerLevel level && this.getOwner() instanceof ServerPlayer owner) {
         Vec3 dir = this.getDeltaMovement().lengthSqr() > 1.0E-6
            ? this.getDeltaMovement().normalize()
            : Vec3.atLowerCornerOf(hit.getDirection().getOpposite().getNormal());
         Vec3 at = hit.getLocation();
         if (DoruDoru.canWax(owner)) {
            VoxelSet peg = TechniqueGen.peg(at, dir, 1.75, 0.32);
            int ownerIndex = WaxOwners.get(level.getServer()).index(owner.getUUID());
            long expires = level.getGameTime() + (long)this.pegLifetime;
            ObjectIterator var10 = peg.blocks().long2LongEntrySet().iterator();

            while (var10.hasNext()) {
               Entry e = (Entry)var10.next();
               BlockPos pos = BlockPos.of(e.getLongKey());
               if (level.isLoaded(pos) && level.mayInteract(owner, pos)) {
                  boolean fresh = !(level.getBlockEntity(pos) instanceof WaxBlockEntity);
                  if ((
                        !fresh
                           || WaxWorld.replaceable(level.getBlockState(pos))
                              && !((EntityPlaceEvent)NeoForge.EVENT_BUS
                                    .post(new EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos), level.getBlockState(pos.below()), owner)))
                                 .isCanceled()
                     )
                     && (fresh || ((WaxBlockEntity)level.getBlockEntity(pos)).owner() == ownerIndex)) {
                     WaxWorld.pour(level, pos, e.getLongValue(), this.color, ownerIndex, 2, expires);
                  }
               }
            }
         }

         level.playSound(null, at.x, at.y, at.z, DoruSounds.LANCE_EMBED.get(), SoundSource.PLAYERS, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
         ServerFx.sendNear(level, DoruFx.LANCE_EMBED, this.getId(), at, dir, 1.0F, this.random.nextLong(), this.color, 64.0);
         this.discard();
         return;
      }

      this.discard();
   }

   public int color() {
      return this.color;
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("damage", this.damage);
      tag.putFloat("knockback", this.knockback);
      tag.putInt("peg", this.pegLifetime);
      tag.putInt("color", this.color);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.damage = tag.getFloat("damage");
      this.knockback = tag.getFloat("knockback");
      this.pegLifetime = tag.getInt("peg");
      this.color = tag.getInt("color");
   }
}
