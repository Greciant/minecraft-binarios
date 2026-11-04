package com.awakenedmod.awakened.content.entity.archevoker;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.entity.ai.GatedGoal;
import com.awakenedmod.awakened.content.entity.boss.AwakenedBossEntity;
import com.awakenedmod.awakened.content.entity.boss.BossDefenses;
import com.awakenedmod.awakened.content.entity.boss.MoveScheduler;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.relic.RelicDrops;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ArchEvokerEntity extends AwakenedBossEntity implements ArchEvokerActor {
   private static final BossDefenses.Profile DEFENSE_PROFILE = new BossDefenses.Profile(40.0, 30.0F, 4.0, 100);
   private static final int GAP_P1 = 26;
   private static final int GAP_P2 = 18;
   private static final int GAP_P3 = 12;
   private static final double RETREAT_BAND = 7.0;
   private static final double APPROACH_BAND = 15.0;
   private static final double ORBIT_RADIUS = 11.0;
   private static final double PREFERRED_DIST = 11.0;
   private final MoveScheduler<ArchEvokerEntity> scheduler = new MoveScheduler<>(this);
   private int castCooldown = 26;
   int vexplodeShotsLeft;
   int vexplodeInterval = 6;
   private static final float PHASE2_HP_FRAC = 0.75F;
   private boolean phase2Started;
   private static final double HOME_REACH = 11.0;
   private static final double HOME_REACH_Y = 4.0;
   @Nullable
   private Vec3 home;

   public boolean withinHome(Vec3 spot) {
      Vec3 h = this.home != null ? this.home : this.position();
      return Math.abs(spot.x - h.x) <= 11.0 && Math.abs(spot.z - h.z) <= 11.0 && Math.abs(spot.y - h.y) <= 4.0;
   }

   public boolean inPhase2() {
      return this.getHealth() <= this.getMaxHealth() * 0.75F;
   }

   public ArchEvokerEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level, Component.translatable("entity.awakened.archevoker"), BossBarColor.PURPLE, BossBarOverlay.NOTCHED_10);
      this.xpReward = 70;
      this.setPersistenceRequired();
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 525.0)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.ARMOR_TOUGHNESS, 2.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ATTACK_DAMAGE, 8.0)
         .add(Attributes.FOLLOW_RANGE, 48.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected BossDefenses.Profile defenseProfile() {
      return DEFENSE_PROFILE;
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(5, new GatedGoal(new WaterAvoidingRandomStrollGoal(this, 0.7), () -> this.getTarget() == null));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
      this.goalSelector.addGoal(7, new GatedGoal(new RandomLookAroundGoal(this), () -> this.getTarget() == null));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.home != null) {
         tag.putDouble("HomeX", this.home.x);
         tag.putDouble("HomeY", this.home.y);
         tag.putDouble("HomeZ", this.home.z);
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.home = tag.contains("HomeX") ? new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ")) : null;
   }

   private int neutralGap() {
      return switch (this.escalationPhase()) {
         case 1 -> 26;
         case 2 -> 18;
         default -> 12;
      };
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      if (this.home == null) {
         this.home = this.position();
      }

      if (this.deathTime <= 0) {
         if (!this.phase2Started && this.inPhase2()) {
            this.phase2Started = true;
            this.enterPhase2(level);
         }

         LivingEntity target = this.getTarget();
         if (this.scheduler.isRunning()) {
            this.scheduler.advance(level, target);
            if (!this.scheduler.isRunning()) {
               this.castCooldown = this.neutralGap();
            }
         } else if (target == null) {
            this.scheduler.stopCircling();
         } else {
            if (this.castCooldown > 0) {
               this.castCooldown--;
            }

            if (this.castCooldown <= 0) {
               ArchEvokerMove move = this.scheduler.pickMove(level, target, ArchEvokerMoves.KIT, this.onGround());
               if (move != null) {
                  this.scheduler.stopCircling();
                  this.scheduler.start(level, move, target);
                  return;
               }
            }

            double dist = (double)this.distanceTo(target);
            if (dist > 15.0) {
               this.scheduler.stopCircling();
               this.kiteToward(target);
            } else if (dist < 7.0) {
               this.scheduler.stopCircling();
               this.getNavigation().stop();
               this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            } else {
               this.scheduler.tickCircling(target, true, 7.0, 19.0, 11.0, 0.9);
            }
         }
      }
   }

   private void kiteToward(LivingEntity target) {
      Vec3 toward = target.position().subtract(this.position());
      Vec3 dir = toward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : toward.normalize();
      Vec3 dest = target.position().subtract(dir.scale(11.0));
      this.getNavigation().moveTo(dest.x, dest.y, dest.z, 0.95);
      this.getLookControl().setLookAt(target, 30.0F, 30.0F);
   }

   private void enterPhase2(ServerLevel level) {
      Fx.burst(level, ParticleTypes.WITCH, this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0), 60, 1.4, 0.2);
      Fx.sound(level, this.position(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.6F, 0.7F);
      this.scheduler.interrupt();
      this.castCooldown = 30;
   }

   @Override
   protected void onEscalate(ServerLevel level, int newTier) {
      if (newTier >= 3) {
         Fx.burst(level, ParticleTypes.WITCH, this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0), 55, 1.3, 0.25);
         Fx.sound(level, this.position(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.6F, 0.5F);
         this.bossEvent.setColor(BossBarColor.RED);
         this.bossEvent.setDarkenScreen(true);
         this.scheduler.interrupt();
         this.castCooldown = 25;
      }
   }

   @Override
   protected void onDeathSequenceStart() {
      this.scheduler.interrupt();
      this.bossEvent.setDarkenScreen(false);
   }

   @Override
   protected int deathDuration() {
      return 50;
   }

   @Override
   protected void deathBeat(ServerLevel level, int t) {
      if (t == 1) {
         Fx.sound(level, this.position(), SoundEvents.EVOKER_DEATH, SoundSource.HOSTILE, 1.4F, 0.8F);
      }

      if (t == 25) {
         Fx.burst(level, ParticleTypes.WITCH, this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0), 50, 1.2, 0.25);
      }
   }

   @Override
   protected void dropCinematicLoot(ServerLevel level) {
      Vec3 at = this.position().add(0.0, 1.0, 0.0);
      bless(this.spawnAtFeet(level, new ItemStack((ItemLike)AwakenedItems.ARCHEVOKER_STAFF.get())));
      bless(RelicDrops.dropSignature(level, at, "fangbinder_sigil"));
      bless(this.spawnAtFeet(level, new ItemStack(Items.TOTEM_OF_UNDYING, 2)));
      bless(this.spawnAtFeet(level, new ItemStack(Items.ECHO_SHARD, 2 + this.getRandom().nextInt(3))));
   }

   @Override
   public double bonusMastery() {
      return 200.0;
   }

   @Override
   public double bonusXp() {
      return 650.0;
   }

   @Override
   public void onCreditedKill(ServerPlayer player) {
      AwakenedAttachments.progress(player).bossDefeated();
      PowerService.syncProgress(player);
      QuestSystem.onBossDefeated(player);
   }
}
