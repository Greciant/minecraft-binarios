package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.OwnedProjectile;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.HashSet;
import java.util.Set;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public final class AeroCrescentEntity extends ThrowableItemProjectile implements OwnedProjectile {
   private static final double WIDTH_RADIUS = 0.9;
   private static final double HEIGHT_RADIUS = 0.35;
   private float damage = 7.0F;
   private boolean pierce = false;
   private int maxLifeTicks = 20;
   private final Set<Integer> alreadyHit = new HashSet<>();
   private int fightablesStruck = 0;
   private static final int PIERCE_FEAT_TARGETS = 2;

   @Override
   public Entity shooter() {
      return this.getOwner();
   }

   @Override
   public float shotDamage() {
      return this.damage;
   }

   public AeroCrescentEntity(EntityType<? extends AeroCrescentEntity> type, Level level) {
      super(type, level);
   }

   public AeroCrescentEntity(EntityType<? extends AeroCrescentEntity> type, LivingEntity owner, Level level) {
      super(type, owner, level);
      this.setItem(new ItemStack(Items.FEATHER));
   }

   public void configure(float damage, boolean pierce, int maxLifeTicks) {
      this.damage = damage;
      this.pierce = pierce;
      this.maxLifeTicks = maxLifeTicks;
   }

   protected Item getDefaultItem() {
      return Items.FEATHER;
   }

   protected double getDefaultGravity() {
      return 0.0;
   }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel server && !this.isRemoved()) {
         Entity owner = this.getOwner();
         AABB sweep = new AABB(this.getX() - 0.9, this.getY() - 0.35, this.getZ() - 0.9, this.getX() + 0.9, this.getY() + 0.35, this.getZ() + 0.9);

         for (LivingEntity victim : server.getEntitiesOfClass(
            LivingEntity.class, sweep, e -> e.isAlive() && e != owner && !this.alreadyHit.contains(e.getId()) && Harm.allowed(owner, e)
         )) {
            this.strike(server, victim, owner);
            if (!this.pierce) {
               this.discard();
               return;
            }
         }

         if (this.tickCount > this.maxLifeTicks) {
            this.discard();
         }

         return;
      }
   }

   private void strike(ServerLevel server, LivingEntity target, Entity owner) {
      this.alreadyHit.add(target.getId());
      boolean dealt = Dmg.hurt(target, server, this.damageSources().thrown(this, owner), this.damage);
      if (dealt && owner instanceof ServerPlayer player) {
         MasterySystem.award(player, MasteryReason.HIT, target);
         if (Foes.counts(player, target)) {
            if (++this.fightablesStruck == 2) {
               QuestSystem.feat(player, "aero_pierce");
            }
         }
      }
   }

   protected void onHitEntity(EntityHitResult result) {
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (this.level() instanceof ServerLevel && !this.isRemoved()) {
         this.discard();
      }
   }
}
