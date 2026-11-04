package com.awakenedmod.awakened.content.phytokinetic;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityBalance;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class LifeGraftAbility extends Ability {
   public static final String ID = "life_graft";
   private static final int UNLOCK = 7;
   private static final double AIM_RANGE = 9.0;
   private static final double AIM_CONE_DEG = 8.0;
   private static final double BREAK_RANGE = 12.0;
   private static final int BREAK_GRACE_TICKS = 8;
   private static final float DRAIN_PER_TICK = 0.3F;
   private static final float TRANSFER_HP = 1.0F;
   private static final int TRANSFER_EVERY = 5;
   private static final float MIN_SELF_HP = 1.0F;
   private static final float CRITICAL_FRACTION = 0.3F;
   private static final double TENDRIL_WIDTH = 0.045;
   private static final double TENDRIL_SAG = 0.09;
   private static final int TENDRIL_EVERY = 2;
   private static final int TENDRIL_LIFE = 4;
   private static final double FLOW_STEP = 0.05;
   private static final int MOTES_PER_TICK = 3;
   private static final int PULSE_EVERY = 10;
   private static final int SOUND_EVERY = 14;
   private final Map<UUID, LifeGraftAbility.Graft> grafts = this.activeState();

   public LifeGraftAbility() {
      super("life_graft", 0.0F, 7);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String costLabel() {
      return (int)this.billed(6.0F) + "/s";
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().healing(1.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      return this.begin(player);
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.end(player, false);
   }

   private boolean begin(ServerPlayer player) {
      if (this.grafts.containsKey(player.getUUID())) {
         return true;
      } else {
         ServerLevel level = player.serverLevel();
         LivingEntity target = AimUtil.aimedLivingSoft(player, 9.0, 8.0, e -> true);
         if (target == null) {
            return this.fail(player, "awakened.feedback.no_graft_target", new Object[0]);
         } else {
            PowerData data = PowerService.data(player);
            if (!data.infiniteStamina() && data.stamina() < this.billed(0.3F)) {
               return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
            } else {
               boolean feed = player.isShiftKeyDown();
               this.grafts.put(player.getUUID(), new LifeGraftAbility.Graft(target.getId(), feed));
               PowerService.playAnimation(player, "life_graft");
               Vec3 tap = target.getBoundingBox().getCenter();
               PhytoFx.tautVine(level, PhytoFx.sagPath(AimUtil.handPosition(player), tap, 0.09), 0.045, 4);
               PhytoFx.leafBurst(level, tap, 4);
               Fx.sound(level, player.position(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6F, 1.2F);
               if (feed) {
                  Fx.sound(level, tap, SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
               } else {
                  Fx.sound(level, tap, SoundEvents.SPONGE_ABSORB, SoundSource.PLAYERS, 0.5F, 0.8F);
               }

               return true;
            }
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      LifeGraftAbility.Graft graft = this.grafts.get(player.getUUID());
      if (graft != null) {
         ServerLevel level = player.serverLevel();
         if (level.getEntity(graft.targetId) instanceof LivingEntity target && target.isAlive()) {
            Vec3 tap = target.getBoundingBox().getCenter();
            boolean broken = (double)player.distanceTo(target) > 12.0 || !hasLineOfSight(player, level, tap);
            if (broken) {
               if (++graft.slack > 8) {
                  this.end(player, true);
                  return;
               }
            } else {
               graft.slack = 0;
            }

            PowerData data = PowerService.data(player);
            long gameTime = level.getGameTime();
            StaminaSystem.suppressRegen(data, gameTime);
            if (!data.infiniteStamina()) {
               float drain = this.billed(0.3F);
               if (data.stamina() < drain) {
                  this.end(player, false);
                  return;
               }

               StaminaSystem.drainSustained(data, gameTime, drain);
            }

            graft.feed = player.isShiftKeyDown();
            graft.tick++;
            if (!broken && graft.tick % 5 == 0) {
               this.transfer(player, level, graft, target);
            }

            Vec3 hand = AimUtil.handPosition(player);
            List<Vec3> path = PhytoFx.sagPath(hand, tap, 0.09);
            if (graft.tick % 2 == 0) {
               PhytoFx.tautVine(level, path, 0.045, 4);
            }

            tickFlow(level, graft, path);
            tickEndpoints(level, graft, player, target);
            tickSoundBed(level, graft, path);
            if (player.tickCount % 3 == 0) {
               PowerService.sync(player);
            }

            return;
         }

         this.end(player, true);
      }
   }

   private void transfer(ServerPlayer player, ServerLevel level, LifeGraftAbility.Graft graft, LivingEntity target) {
      float amount = MasteryScaling.scaled(1.0F, player);
      if (graft.feed) {
         float available = (float)Math.max(0.0, (double)(player.getHealth() - 1.0F));
         float missing = target.getMaxHealth() - target.getHealth();
         float moved = Math.min(amount, Math.min(available, missing));
         boolean ally = target instanceof Player;
         if (moved > 0.0F) {
            boolean clutch = target.getHealth() < target.getMaxHealth() * 0.3F;
            player.setHealth(player.getHealth() - moved);
            target.heal(moved);
            if (ally) {
               MasterySystem.award(player, MasteryReason.HIT);
               if (clutch) {
                  MasterySystem.award(player, MasteryReason.SURVIVAL);
               }
            }
         }
      } else {
         float before = target.getHealth();
         float leech = amount * AbilityBalance.damageMultiplier(this.balanceKey());
         if (Dmg.hurtRapid(target, level, Dmg.by(player, DamageTypes.MAGIC), leech)) {
            float drained = Math.max(0.0F, before - target.getHealth());
            if (drained > 0.0F) {
               player.heal(drained);
            }

            MasterySystem.award(player, MasteryReason.HIT, target);
         }
      }
   }

   private static void tickFlow(ServerLevel level, LifeGraftAbility.Graft graft, List<Vec3> path) {
      graft.flowPhase = (graft.flowPhase + 0.05) % 1.0;

      for (int i = 0; i < 3; i++) {
         double f = (graft.flowPhase + (double)i / 3.0) % 1.0;
         double at = graft.feed ? f : 1.0 - f;
         double ahead = graft.feed ? Math.min(1.0, f + 0.06) : Math.max(0.0, 1.0 - f - 0.06);
         Vec3 pos = PhytoFx.pointAt(path, at);
         Vec3 dir = PhytoFx.pointAt(path, ahead).subtract(pos);
         Vec3 vel = dir.lengthSqr() < 1.0E-6 ? Vec3.ZERO : dir.normalize().scale(0.2);
         ParticleOptions mote = graft.feed ? (i % 2 == 0 ? PhytoFx.LEAF_DUST : PhytoFx.POLLEN_DUST) : PhytoFx.LIFE_DUST;
         level.sendParticles(mote, pos.x, pos.y, pos.z, 0, vel.x, vel.y, vel.z, 1.0);
      }
   }

   private static void tickEndpoints(ServerLevel level, LifeGraftAbility.Graft graft, ServerPlayer player, LivingEntity target) {
      if (graft.tick % 10 == 0) {
         Vec3 casterChest = player.position().add(0.0, (double)player.getBbHeight() * 0.6, 0.0);
         Vec3 targetChest = target.getBoundingBox().getCenter();
         Vec3 receiver = graft.feed ? targetChest : casterChest;
         Vec3 source = graft.feed ? casterChest : targetChest;
         Fx.burst(level, graft.feed ? PhytoFx.LEAF_DUST : PhytoFx.LIFE_DUST, receiver, 4, 0.25, 0.02);
         if (graft.tick % 20 == 0) {
            Fx.burst(level, ParticleTypes.HEART, receiver.add(0.0, 0.5, 0.0), 1, 0.2, 0.0);
         }

         Fx.burst(level, graft.feed ? PhytoFx.LEAF_DUST : PhytoFx.BARK_DUST, source.add(0.0, -0.2, 0.0), 3, 0.25, 0.02);
      }
   }

   private static void tickSoundBed(ServerLevel level, LifeGraftAbility.Graft graft, List<Vec3> path) {
      if (graft.tick % 14 == 0) {
         Vec3 mid = PhytoFx.pointAt(path, 0.5);
         if (graft.feed) {
            Fx.sound(level, mid, SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 0.5F, 1.5F);
            if (graft.tick % 28 == 0) {
               Fx.sound(level, mid, SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.35F, 1.6F);
            }
         } else {
            Fx.sound(level, mid, SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.4F, 0.7F);
         }
      }
   }

   private static boolean hasLineOfSight(ServerPlayer player, ServerLevel level, Vec3 tap) {
      return level.clip(new ClipContext(player.getEyePosition(), tap, Block.COLLIDER, Fluid.NONE, player)).getType() == Type.MISS;
   }

   private void end(ServerPlayer player, boolean snapped) {
      LifeGraftAbility.Graft graft = this.grafts.remove(player.getUUID());
      if (graft != null) {
         ServerLevel level;
         Vec3 hand;
         Vec3 var10000;
         label29: {
            PowerService.stopAnimation(player);
            level = player.serverLevel();
            hand = AimUtil.handPosition(player);
            if (level.getEntity(graft.targetId) instanceof LivingEntity target && target.isAlive()) {
               var10000 = target.getBoundingBox().getCenter();
               break label29;
            }

            var10000 = hand.add(player.getLookAngle().scale(2.0));
         }

         Vec3 tap = var10000;
         List<Vec3> path = PhytoFx.sagPath(hand, tap, 0.09);

         for (int i = 0; i <= 6; i++) {
            Fx.burst(level, PhytoFx.LEAF_DUST, PhytoFx.pointAt(path, (double)i / 6.0), 1, 0.1, 0.02);
         }

         Vec3 mid = PhytoFx.pointAt(path, 0.5);
         if (snapped) {
            Fx.burst(level, PhytoFx.BARK_DUST, mid, 6, 0.3, 0.05);
            Fx.sound(level, mid, SoundEvents.VINE_BREAK, SoundSource.PLAYERS, 1.1F, 0.75F);
         } else {
            Fx.sound(level, mid, SoundEvents.VINE_BREAK, SoundSource.PLAYERS, 0.7F, 1.15F);
            Fx.sound(level, mid, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 0.6F, 1.0F);
         }

         PowerService.sync(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.grafts.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }

   private static final class Graft {
      final int targetId;
      boolean feed;
      int tick;
      int slack;
      double flowPhase;

      Graft(int targetId, boolean feed) {
         this.targetId = targetId;
         this.feed = feed;
      }
   }
}
