package com.devilfruits.fruits.kage_kage.entity;

import com.devilfruits.core.npc.Faction;
import com.devilfruits.core.npc.FactionFighter;
import com.devilfruits.core.util.Targeting;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class ShadowServant extends FactionFighter {
   public static final int ATTACK = 0;
   @Nullable
   private UUID ownerId;
   private WeakReference<ServerPlayer> ownerRef = new WeakReference<>(null);
   @Nullable
   private LivingEntity commanded;
   @Nullable
   private LivingEntity striking;

   protected ShadowServant(EntityType<? extends ShadowServant> type, Level level) {
      super(type, level);
      this.xpReward = 0;
      this.setPersistenceRequired();
   }

   public abstract int attackTicks();

   public abstract int attackHit();

   public abstract boolean standing();

   @Override
   public Faction faction() {
      return Faction.SHADOW;
   }

   public void setOwner(ServerPlayer owner) {
      this.ownerId = owner.getUUID();
      this.ownerRef = new WeakReference<>(owner);
   }

   @Nullable
   public UUID ownerId() {
      return this.ownerId;
   }

   @Nullable
   public ServerPlayer owner() {
      if (this.ownerId != null && this.level() instanceof ServerLevel server) {
         ServerPlayer var3 = server.getServer().getPlayerList().getPlayer(this.ownerId);
         if (var3 instanceof ServerPlayer) {
            return var3;
         } else {
            ServerPlayer p = this.ownerRef.get();
            return p != null && !p.isRemoved() && p.getUUID().equals(this.ownerId) ? p : null;
         }
      } else {
         return null;
      }
   }

   public boolean mayFight(@Nullable LivingEntity e) {
      ServerPlayer owner = this.owner();
      return e != null
         && owner != null
         && e != owner
         && e != this
         && e.isAlive()
         && !(e instanceof ShadowServant)
         && this.canHarm(e)
         && Targeting.canHurt(owner, e);
   }

   public void command(LivingEntity target) {
      if (this.mayFight(target)) {
         this.commanded = target;
         this.setTarget(target);
      }
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true));
      this.goalSelector.addGoal(2, new ShadowServant.FollowOwnerGoal());
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new ShadowServant.OwnerTargetGoal());
   }

   public boolean isImmobile() {
      return super.isImmobile() || !this.standing();
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      return this.ownerId != null && other.getUUID().equals(this.ownerId) || other instanceof ShadowServant || super.isAlliedTo(other);
   }

   public boolean canAttack(LivingEntity target) {
      return !(this.level() instanceof ServerLevel) || this.mayFight(target);
   }

   public boolean doHurtTarget(Entity target) {
      if (!this.isActing() && target instanceof LivingEntity living) {
         this.striking = living;
         this.playAction(0, this.attackTicks());
      }

      return true;
   }

   @Override
   protected void onActionTick(int id, int tick) {
      if (id == 0 && tick == this.attackHit() && this.striking != null) {
         LivingEntity t = this.striking;
         this.striking = null;
         if (this.mayFight(t) && this.distanceToSqr(t) < 12.25) {
            float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE) - 0.5F + this.getRandom().nextFloat();
            t.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 push = t.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-4) {
               t.knockback(0.4, -push.x, -push.z);
            }

            this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 0.6F);
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.commanded != null && !this.mayFight(this.commanded)) {
         this.commanded = null;
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            && this.ownerId != null
            && source.getEntity() != null
            && this.ownerId.equals(source.getEntity().getUUID())
         ? false
         : super.hurt(source, amount);
   }

   @Override
   protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
   }

   public boolean shouldDespawnInPeaceful() {
      return false;
   }

   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
   }

   private final class FollowOwnerGoal extends Goal {
      FollowOwnerGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         ServerPlayer owner = ShadowServant.this.owner();
         return owner != null
            && owner.level() == ShadowServant.this.level()
            && ShadowServant.this.getTarget() == null
            && ShadowServant.this.distanceToSqr(owner) > 36.0;
      }

      public boolean canContinueToUse() {
         ServerPlayer owner = ShadowServant.this.owner();
         return owner != null
            && owner.level() == ShadowServant.this.level()
            && ShadowServant.this.getTarget() == null
            && ShadowServant.this.distanceToSqr(owner) > 9.0
            && !ShadowServant.this.getNavigation().isDone();
      }

      public void tick() {
         ServerPlayer owner = ShadowServant.this.owner();
         if (owner != null) {
            ShadowServant.this.getLookControl().setLookAt(owner, 10.0F, (float)ShadowServant.this.getMaxHeadXRot());
            if (ShadowServant.this.tickCount % 10 == 0) {
               ShadowServant.this.getNavigation().moveTo(owner, 1.2);
            }
         }
      }

      public void stop() {
         ShadowServant.this.getNavigation().stop();
      }
   }

   private final class OwnerTargetGoal extends Goal {
      OwnerTargetGoal() {
         this.setFlags(EnumSet.of(Flag.TARGET));
      }

      @Nullable
      private LivingEntity pick() {
         if (ShadowServant.this.commanded != null && ShadowServant.this.mayFight(ShadowServant.this.commanded)) {
            return ShadowServant.this.commanded;
         } else {
            ServerPlayer owner = ShadowServant.this.owner();
            if (owner == null) {
               return null;
            } else {
               LivingEntity by = owner.getLastHurtByMob();
               if (by != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 100 && ShadowServant.this.mayFight(by)) {
                  return by;
               } else {
                  LivingEntity hit = owner.getLastHurtMob();
                  if (hit != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 100 && ShadowServant.this.mayFight(hit)) {
                     return hit;
                  } else {
                     LivingEntity current = ShadowServant.this.getTarget();
                     return ShadowServant.this.mayFight(current) ? current : null;
                  }
               }
            }
         }
      }

      public boolean canUse() {
         LivingEntity next = this.pick();
         return next != null && next != ShadowServant.this.getTarget();
      }

      public void start() {
         ShadowServant.this.setTarget(this.pick());
      }

      public boolean canContinueToUse() {
         return false;
      }
   }
}
