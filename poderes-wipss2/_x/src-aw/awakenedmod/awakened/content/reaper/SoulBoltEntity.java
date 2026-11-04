package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.OwnedProjectile;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Iterator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class SoulBoltEntity extends ThrowableItemProjectile implements OwnedProjectile {
   private float damage = 4.0F;
   private double visualRadius = 0.35;
   private double contactRadius = 0.45;
   private int maxLifeTicks = 50;
   private int targetId = -1;
   private double homeStrength = 0.18;
   private double speed = 1.6;

   @Override
   public Entity shooter() {
      return this.getOwner();
   }

   @Override
   public float shotDamage() {
      return this.damage;
   }

   public SoulBoltEntity(EntityType<? extends SoulBoltEntity> type, Level level) {
      super(type, level);
   }

   public SoulBoltEntity(EntityType<? extends SoulBoltEntity> type, LivingEntity owner, Level level) {
      super(type, owner, level);
      this.setItem(new ItemStack(Items.COAL));
   }

   public void configure(float damage, double visualRadius) {
      this.damage = damage;
      this.visualRadius = visualRadius;
      this.contactRadius = Math.max(0.4, visualRadius);
   }

   public void setHoming(Entity target, double strength) {
      this.targetId = target != null ? target.getId() : -1;
      this.homeStrength = strength;
   }

   public void setSpeed(double speed) {
      this.speed = speed;
   }

   public void setMaxLife(int ticks) {
      this.maxLifeTicks = ticks;
   }

   protected Item getDefaultItem() {
      return Items.COAL;
   }

   protected double getDefaultGravity() {
      return 0.0;
   }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel server) {
         label35:
         if (this.targetId >= 0) {
            if (server.getEntity(this.targetId) instanceof LivingEntity living && living.isAlive()) {
               Vec3 toTarget = living.getBoundingBox().getCenter().subtract(this.position());
               double len = toTarget.length();
               if (len > 0.001) {
                  Vec3 desired = toTarget.scale(1.0 / len);
                  Vec3 cur = this.getDeltaMovement();
                  double curLen = cur.length();
                  Vec3 dir = curLen > 0.001 ? cur.scale(1.0 / curLen) : desired;
                  Vec3 steered = dir.add(desired.subtract(dir).scale(this.homeStrength)).normalize();
                  this.setDeltaMovement(steered.scale(this.speed));
               } else {
                  this.targetId = -1;
               }
               break label35;
            }

            this.targetId = -1;
         }

         ReaperFx.boltTrail(server, this.position(), this.visualRadius);
         Entity owner = this.getOwner();
         Iterator var15 = server.getEntitiesOfClass(
               LivingEntity.class, this.getBoundingBox().inflate(this.contactRadius), e -> e.isAlive() && e != owner && Harm.allowed(this, e)
            )
            .iterator();
         if (var15.hasNext()) {
            LivingEntity victim = (LivingEntity)var15.next();
            this.strike(server, victim, owner);
            this.detonate(server);
         } else {
            if (this.tickCount > this.maxLifeTicks) {
               this.fizzle(server);
            }
         }
      }
   }

   private void strike(ServerLevel server, LivingEntity target, Entity owner) {
      boolean dealt = Dmg.hurt(target, server, this.damageSources().thrown(this, owner), this.damage);
      if (dealt && owner instanceof ServerPlayer player) {
         MasterySystem.award(player, MasteryReason.HIT, target);
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (this.level() instanceof ServerLevel server) {
         this.fizzle(server);
      }
   }

   private void detonate(ServerLevel server) {
      if (!this.isRemoved()) {
         ReaperFx.boltImpact(server, this.position(), this.visualRadius);
         this.discard();
      }
   }

   private void fizzle(ServerLevel server) {
      if (!this.isRemoved()) {
         ReaperFx.wisps(server, this.position(), 4, this.visualRadius);
         this.discard();
      }
   }
}
