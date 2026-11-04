package com.devilfruits.mobs.pirate.entity;

import com.devilfruits.mobs.pirate.PirateClips;
import com.devilfruits.mobs.pirate.PirateConfig;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.goal.GunnerGoal;
import com.devilfruits.mobs.pirate.item.ReloadCues;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PirateGunner extends PirateFighter {
   public PirateGunner(EntityType<? extends PirateGunner> type, Level level) {
      super(type, level);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ATTACK_DAMAGE, 2.0)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.ARMOR, 1.0);
   }

   @Override
   protected void applyStats() {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((Double)PirateConfig.GUNNER_HEALTH.get());
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((Double)PirateConfig.GUNNER_SPEED.get());
   }

   @Override
   public int rank() {
      return 1;
   }

   @Override
   protected int variantCount() {
      return 4;
   }

   public boolean hasMusket() {
      return this.getMainHandItem().is((Item)Pirates.MUSKET.get());
   }

   @Override
   protected void addCombatGoals() {
      this.goalSelector.addGoal(2, new GunnerGoal(this));
   }

   protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
      boolean musket = random.nextDouble() < (Double)PirateConfig.GUNNER_MUSKET_CHANCE.get();
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(musket ? (ItemLike)Pirates.MUSKET.get() : (ItemLike)Pirates.FLINTLOCK_PISTOL.get()));
      this.setFlag(8, true);
   }

   @Override
   protected void onActionTick(int id, int tick) {
      LivingEntity target = this.getTarget();
      if ((id == 5 || id == 7) && tick == PirateClips.HIT[id]) {
         this.setFlag(8, false);
         if (target != null && target.isAlive()) {
            boolean musket = id == 7;
            float damage = ((Double)(musket ? PirateConfig.GUNNER_MUSKET_DAMAGE : PirateConfig.GUNNER_PISTOL_DAMAGE).get()).floatValue();
            float spread = ((Double)PirateConfig.GUNNER_INACCURACY.get()).floatValue()
               * (musket ? 0.5F : 1.0F)
               * (float)(4 - this.level().getDifficulty().getId())
               / 2.0F;
            this.shootAt(target, damage, spread, musket, false);
         }
      } else if (id == 6 || id == 8) {
         ReloadCues.sound(this, id, tick, this.getSoundSource(), true);
         if (tick == PirateClips.HIT[id]) {
            this.setFlag(8, true);
         }
      } else if (id == 9 && tick == PirateClips.MOVE[id]) {
         Vec3 away = target != null
            ? this.position().subtract(target.position()).multiply(1.0, 0.0, 1.0)
            : Vec3.directionFromRotation(0.0F, this.getYRot()).scale(-1.0);
         Vec3 hop = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(0.55);
         this.setDeltaMovement(hop.add(0.0, 0.32, 0.0));
         this.hasImpulse = true;
      }
   }

   @Override
   protected SoundEvent ambient() {
      return Pirates.GUNNER_AMBIENT.get();
   }

   @Override
   protected SoundEvent hurt() {
      return Pirates.GUNNER_HURT.get();
   }

   @Override
   protected SoundEvent death() {
      return Pirates.GUNNER_DEATH.get();
   }
}
