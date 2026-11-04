package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.OwnedProjectile;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class WarhammerThrowEntity extends ThrowableItemProjectile implements OwnedProjectile {
   private static final double THROW_SPEED = 1.5;
   private static final double RETURN_SPEED = 1.5;
   private static final double RETURN_HOMING = 0.35;
   private static final double MAX_THROW_DISTANCE = 28.0;
   private static final int OUTBOUND_MAX_TICKS = 32;
   private static final int GLOBAL_MAX_TICKS = 200;
   private static final double RECALL_REACH = 1.7;
   private static final double CONTACT_RADIUS = 0.9;
   private static final float THROW_BASE_DAMAGE = 8.0F;
   private static final float THROW_DAMAGE_PER_BLOCK = 0.5F;
   private static final float THROW_MAX_DAMAGE = 26.0F;
   static final String BALANCE_KEY = "WarhammerThrow";
   private static final double THROW_KNOCKBACK = 1.0;
   private static final int MAX_THROW_HITS = 3;
   private boolean returning = false;
   private double distanceFlown = 0.0;
   private boolean delivered = false;
   private final Set<UUID> struck = new HashSet<>();

   @Override
   public Entity shooter() {
      return this.getOwner();
   }

   @Override
   public float shotDamage() {
      return Math.min(26.0F, 8.0F + (float)this.distanceFlown * 0.5F);
   }

   public WarhammerThrowEntity(EntityType<? extends WarhammerThrowEntity> type, Level level) {
      super(type, level);
   }

   public WarhammerThrowEntity(EntityType<? extends WarhammerThrowEntity> type, LivingEntity owner, Level level, ItemStack item) {
      super(type, owner, level);
      this.setItem(item);
   }

   protected Item getDefaultItem() {
      return (Item)AwakenedItems.EXOSAVANT_WARHAMMER.get();
   }

   protected double getDefaultGravity() {
      return 0.0;
   }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel server && !this.delivered && !this.isRemoved()) {
         ExoFx.exhaustTrail(server, this.position());
         if (this.tickCount > 200) {
            this.returnOrDrop(server);
            return;
         }

         if (!this.returning) {
            this.distanceFlown = this.distanceFlown + this.getDeltaMovement().length();
            Vec3 v = this.getDeltaMovement();
            if (v.lengthSqr() > 1.0E-6) {
               this.setDeltaMovement(v.normalize().scale(1.5));
            }

            this.contactSweep(server);
            if (!this.returning && (this.distanceFlown >= 28.0 || this.tickCount > 32)) {
               this.beginReturn();
            }
         } else {
            this.homeToOwner(server);
         }

         return;
      }
   }

   private void contactSweep(ServerLevel server) {
      Entity owner = this.getOwner();

      for (LivingEntity victim : server.getEntitiesOfClass(
         LivingEntity.class,
         this.getBoundingBox().inflate(0.9),
         e -> e.isAlive()
               && e != owner
               && e.attackable()
               && !this.struck.contains(e.getUUID())
               && (!(owner instanceof LivingEntity ol) || !e.isAlliedTo(ol))
               && Harm.allowed(this, e)
      )) {
         this.struck.add(victim.getUUID());
         this.hitFoe(server, victim, owner);
         if (this.struck.size() >= 3) {
            this.beginReturn();
            return;
         }
      }
   }

   private void hitFoe(ServerLevel server, LivingEntity victim, Entity owner) {
      float damage = Math.min(26.0F, 8.0F + (float)this.distanceFlown * 0.5F);
      damage = owner instanceof ServerPlayer thrower
         ? MasteryScaling.scaledDamage(damage, thrower, "WarhammerThrow")
         : MasteryScaling.configScaledDamage(damage, "WarhammerThrow");
      boolean dealt = Dmg.hurt(victim, server, this.damageSources().thrown(this, owner), damage);
      if (dealt) {
         Vec3 heading = this.getDeltaMovement();
         if (heading.lengthSqr() < 1.0E-6) {
            heading = victim.position().subtract(this.position());
         }

         victim.knockback(1.0, -heading.x, -heading.z);
         victim.hurtMarked = true;
         ExoFx.blast(server, victim.getBoundingBox().getCenter(), 1.0);
         if (owner instanceof ServerPlayer player) {
            ExoTags.applyTaggedBonus(player, victim, server, damage);
         }
      }
   }

   private void beginReturn() {
      if (!this.returning) {
         this.returning = true;
         this.struck.clear();
         this.playSound(SoundEvents.TRIDENT_RETURN, 0.9F, 1.1F);
      }
   }

   private void homeToOwner(ServerLevel server) {
      if (this.getOwner() instanceof ServerPlayer player && player.isAlive() && player.serverLevel() == server) {
         Vec3 target = player.position().add(0.0, (double)player.getBbHeight() * 0.6, 0.0);
         Vec3 to = target.subtract(this.position());
         double dist = to.length();
         if (dist <= 1.7) {
            this.returnToOwner(server, player);
            return;
         }

         Vec3 desired = to.scale(1.0 / dist);
         Vec3 cur = this.getDeltaMovement();
         double curLen = cur.length();
         Vec3 dir = curLen > 0.001 ? cur.scale(1.0 / curLen) : desired;
         Vec3 steered = dir.add(desired.subtract(dir).scale(0.35)).normalize();
         this.setDeltaMovement(steered.scale(1.5));
         return;
      }

      this.returnOrDrop(server);
   }

   private void returnToOwner(ServerLevel server, ServerPlayer player) {
      if (!this.delivered) {
         this.delivered = true;
         ItemStack stack = this.getItem().copy();
         if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
         } else if (!player.getInventory().add(stack)) {
            ItemEntity drop = new ItemEntity(server, player.getX(), player.getY(), player.getZ(), stack);
            drop.setDefaultPickUpDelay();
            server.addFreshEntity(drop);
         }

         server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.PLAYERS, 0.7F, 1.4F);
         ExoFx.blast(server, this.position(), 0.6);
         this.discard();
      }
   }

   private void returnOrDrop(ServerLevel server) {
      if (this.getOwner() instanceof ServerPlayer player && player.isAlive() && player.serverLevel() == server) {
         this.returnToOwner(server, player);
         return;
      }

      this.dropHere(server);
   }

   private void dropHere(ServerLevel server) {
      if (!this.delivered) {
         this.delivered = true;
         ItemEntity drop = new ItemEntity(server, this.getX(), this.getY(), this.getZ(), this.getItem().copy());
         drop.setDefaultPickUpDelay();
         server.addFreshEntity(drop);
         this.discard();
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (!this.returning && this.level() instanceof ServerLevel) {
         this.beginReturn();
      }
   }

   public void remove(RemovalReason reason) {
      if (!this.delivered && reason != null && reason.shouldDestroy() && this.level() instanceof ServerLevel server && !this.getItem().isEmpty()) {
         this.delivered = true;
         ItemEntity drop = new ItemEntity(server, this.getX(), this.getY(), this.getZ(), this.getItem().copy());
         drop.setDefaultPickUpDelay();
         server.addFreshEntity(drop);
      }

      super.remove(reason);
   }
}
