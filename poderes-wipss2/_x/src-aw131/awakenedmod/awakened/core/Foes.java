package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.team.EntityOwners;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.entity.PartEntity;

public final class Foes {
   public static final int CROWD = 3;

   private Foes() {
   }

   @Nullable
   public static Entity whole(@Nullable Entity entity) {
      return entity instanceof PartEntity<?> part ? part.getParent() : entity;
   }

   public static boolean fightsBack(@Nullable Entity entity) {
      Entity target = whole(entity);
      if (target instanceof Foes.Inert) {
         return false;
      } else if (!(target instanceof Player) && !(target instanceof Enemy) && !(target instanceof NeutralMob)) {
         if (target instanceof Mob mob && (retaliates(mob) || holdsAGrudge(mob))) {
            return true;
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean retaliates(Mob mob) {
      for (WrappedGoal goal : mob.targetSelector.getAvailableGoals()) {
         if (goal.getGoal() instanceof HurtByTargetGoal) {
            return true;
         }
      }

      return false;
   }

   private static boolean holdsAGrudge(Mob mob) {
      return mob.getBrain().checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED);
   }

   public static boolean counts(ServerPlayer player, @Nullable Entity victim) {
      Entity target = whole(victim);
      return fightsBack(target) && !fightsFor(player, target);
   }

   public static boolean countsAsKill(ServerPlayer player, @Nullable Entity victim) {
      Entity target = whole(victim);
      return !(target instanceof Player) && counts(player, target);
   }

   public static boolean fightsFor(ServerPlayer player, @Nullable Entity entity) {
      if (entity == null) {
         return false;
      } else if (entity == player) {
         return true;
      } else {
         if (entity instanceof OwnableEntity ownable && ownable.getOwner() == player) {
            return true;
         }

         UUID owner = EntityOwners.ownerOf(entity);
         return owner != null && owner.equals(player.getUUID());
      }
   }

   public static boolean threat(@Nullable Entity attacker, Entity self) {
      return attacker != self && fightsBack(attacker);
   }

   public static boolean threat(DamageSource source, Entity self) {
      return threat(source.getEntity(), self);
   }

   public static boolean defeatedBy(@Nullable LivingEntity foe, ServerPlayer player) {
      if (!countsAsKill(player, foe)) {
         return false;
      } else if (!foe.isDeadOrDying()) {
         return false;
      } else {
         RemovalReason reason = foe.getRemovalReason();
         return reason != null && reason != RemovalReason.KILLED ? false : foe.getKillCredit() == player && foe.getLastHurtByMob() == player;
      }
   }

   public interface Inert {
   }
}
