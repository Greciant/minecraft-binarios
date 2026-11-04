package com.devilfruits.mobs.pirate.entity;

import com.devilfruits.mobs.pirate.PirateClips;
import com.devilfruits.mobs.pirate.PirateConfig;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.goal.PirateMeleeGoal;
import com.devilfruits.mobs.pirate.goal.PirateRetreatGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PirateDeckhand extends PirateFighter {
   public PirateDeckhand(EntityType<? extends PirateDeckhand> type, Level level) {
      super(type, level);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 24.0)
         .add(Attributes.MOVEMENT_SPEED, 0.26)
         .add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.FOLLOW_RANGE, 28.0)
         .add(Attributes.ARMOR, 2.0);
   }

   @Override
   protected void applyStats() {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((Double)PirateConfig.DECKHAND_HEALTH.get());
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((Double)PirateConfig.DECKHAND_DAMAGE.get());
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((Double)PirateConfig.DECKHAND_SPEED.get());
   }

   @Override
   public int rank() {
      return 0;
   }

   @Override
   protected int variantCount() {
      return 4;
   }

   @Override
   protected void addCombatGoals() {
      this.goalSelector.addGoal(1, new PirateRetreatGoal(this));
      this.goalSelector.addGoal(2, new PirateMeleeGoal(this, 1.2, true));
   }

   protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack((ItemLike)Pirates.CUTLASS.get()));
   }

   @Override
   protected void onActionTick(int id, int tick) {
      LivingEntity target = this.getTarget();
      if (id == 1 && tick == PirateClips.HIT[id]) {
         this.swingFx(1.0F);
         this.meleeHit(target, 1.0F, 0.0, 0.3);
      } else if (id == 2 && tick == PirateClips.HIT[id]) {
         this.swingFx(1.15F);
         this.meleeHit(target, 1.15F, 0.2, 0.3);
      } else if (id == 3) {
         if (tick == PirateClips.MOVE[id] && target != null) {
            Vec3 to = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            Vec3 dash = to.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot()) : to.normalize();
            this.setDeltaMovement(dash.scale(0.85).add(0.0, 0.12, 0.0));
            this.hasImpulse = true;
            this.playSound(Pirates.BARGE.get(), 1.0F, 1.0F);
         } else if (tick == PirateClips.HIT[id]) {
            this.meleeHit(target, 0.6F, 1.1, 0.6);
         }
      }
   }

   @Override
   protected void onActionEnd(int id) {
      if (id == 1) {
         LivingEntity target = this.getTarget();
         if (target != null
            && target.isAlive()
            && this.isWithinMeleeAttackRange(target)
            && (double)this.random.nextFloat() < (Double)PirateConfig.DECKHAND_COMBO_CHANCE.get()) {
            this.playAction(2, PirateClips.LENGTH[2]);
         }
      }
   }

   public boolean killedEntity(ServerLevel level, LivingEntity victim) {
      boolean result = super.killedEntity(level, victim);
      if (!this.isActing()) {
         this.playAction(4, PirateClips.LENGTH[4]);
         this.playSound(Pirates.TAUNT.get(), 1.0F, 1.0F);
      }

      return result;
   }

   @Override
   protected SoundEvent ambient() {
      return Pirates.DECKHAND_AMBIENT.get();
   }

   @Override
   protected SoundEvent hurt() {
      return Pirates.DECKHAND_HURT.get();
   }

   @Override
   protected SoundEvent death() {
      return Pirates.DECKHAND_DEATH.get();
   }
}
