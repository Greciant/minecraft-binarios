package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.OwnedProjectile;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ShadowBolt extends ThrowableItemProjectile implements OwnedProjectile {
   private float directDamage = 6.0F;
   private float aoeDamage = 0.0F;
   private float aoeRadius = 0.0F;
   private double visualRadius = 0.4;
   private double contactRadius = 0.4;
   private int pierce = 0;
   private int slowSeconds = 0;
   private int maxLifeTicks = 60;
   private Consumer<Vec3> onDetonate;
   private final Set<Integer> hit = new HashSet<>();
   private static final float CRESCENT_REACH = 0.8F;
   private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(ShadowBolt.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> CRESCENT = SynchedEntityData.defineId(ShadowBolt.class, EntityDataSerializers.BOOLEAN);

   @Override
   public Entity shooter() {
      return this.getOwner();
   }

   @Override
   public float shotDamage() {
      return this.directDamage;
   }

   public ShadowBolt(EntityType<? extends ShadowBolt> type, Level level) {
      super(type, level);
   }

   public ShadowBolt(EntityType<? extends ShadowBolt> type, LivingEntity owner, Level level) {
      super(type, owner, level);
      this.setItem(new ItemStack(Items.COAL));
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SIZE, 0.4F);
      builder.define(CRESCENT, false);
   }

   public void configure(float directDamage, float aoeDamage, float aoeRadius, double visualRadius) {
      this.directDamage = directDamage;
      this.aoeDamage = aoeDamage;
      this.aoeRadius = aoeRadius;
      this.visualRadius = visualRadius;
      this.contactRadius = Math.max(0.4, visualRadius);
      this.entityData.set(SIZE, (float)visualRadius);
   }

   public void setCrescent(boolean crescent) {
      this.entityData.set(CRESCENT, crescent);
      if (crescent) {
         this.contactRadius = (double)(crescentRadius((float)this.visualRadius) * 0.8F);
      }
   }

   public static float crescentRadius(float size) {
      return Math.max(0.7F, size * 1.7F);
   }

   public float size() {
      return (Float)this.entityData.get(SIZE);
   }

   public boolean isCrescent() {
      return (Boolean)this.entityData.get(CRESCENT);
   }

   public void setPierce(int pierce) {
      this.pierce = pierce;
   }

   public void setSlow(int seconds) {
      this.slowSeconds = seconds;
   }

   public void setMaxLife(int ticks) {
      this.maxLifeTicks = ticks;
   }

   public void setOnDetonate(Consumer<Vec3> onDetonate) {
      this.onDetonate = onDetonate;
   }

   protected Item getDefaultItem() {
      return Items.COAL;
   }

   protected double getDefaultGravity() {
      return 0.01;
   }

   public void tick() {
      Vec3 from = this.position();
      super.tick();
      if (this.level() instanceof ServerLevel server) {
         if (!this.isCrescent()) {
            ShadowFx.boltTrail(server, this.position(), this.visualRadius);
         }

         Entity owner = this.getOwner();
         Vec3 to = this.position();

         for (LivingEntity target : server.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(from, to).inflate(this.contactRadius),
            e -> e.isAlive()
                  && e != owner
                  && !this.hit.contains(e.getId())
                  && Harm.allowed(this, e)
                  && crosses(e.getBoundingBox().inflate(this.contactRadius), from, to)
         )) {
            this.hit.add(target.getId());
            this.strike(server, target, owner);
            if (this.pierce <= 0) {
               this.detonate(server);
               return;
            }

            this.pierce--;
            if (this.pierce <= 0) {
               this.detonate(server);
               return;
            }
         }

         if (this.tickCount > this.maxLifeTicks) {
            this.detonate(server);
         }
      }
   }

   private static boolean crosses(AABB box, Vec3 from, Vec3 to) {
      return box.contains(from) || box.contains(to) || box.clip(from, to).isPresent();
   }

   private void strike(ServerLevel server, LivingEntity target, Entity owner) {
      boolean dealt = Dmg.hurt(target, server, this.damageSources().thrown(this, owner), this.directDamage);
      if (this.slowSeconds > 0) {
         Harm.effect(this, target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.slowSeconds * 20, 1));
      }

      if (dealt && owner instanceof ServerPlayer player) {
         MasterySystem.award(player, MasteryReason.HIT, target);
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (this.level() instanceof ServerLevel server) {
         this.detonate(server);
      }
   }

   private void detonate(ServerLevel server) {
      if (!this.isRemoved()) {
         Vec3 center = this.position();
         Entity owner = this.getOwner();
         if (this.aoeRadius > 0.0F) {
            for (LivingEntity victim : server.getEntitiesOfClass(
               LivingEntity.class, new AABB(center, center).inflate((double)this.aoeRadius), e -> e.isAlive() && e != owner && !this.hit.contains(e.getId())
            )) {
               Dmg.hurt(victim, server, this.damageSources().thrown(this, owner), this.aoeDamage);
               if (this.slowSeconds > 0) {
                  Harm.effect(this, victim, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.slowSeconds * 20, 1));
               }
            }

            ShadowFx.orbBurst(server, center, (double)this.aoeRadius);
         } else if (this.isCrescent()) {
            ShadowFx.slashImpact(server, center);
         } else {
            ShadowFx.orbBurst(server, center, Math.max(0.9, this.visualRadius * 1.5));
         }

         if (this.onDetonate != null) {
            this.onDetonate.accept(center);
         }

         this.discard();
      }
   }
}
