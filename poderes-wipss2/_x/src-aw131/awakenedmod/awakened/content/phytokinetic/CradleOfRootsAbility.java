package com.awakenedmod.awakened.content.phytokinetic;

import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class CradleOfRootsAbility extends Ability {
   public static final String ID = "cradle_of_roots";
   private static final float COST = 0.0F;
   private static final int UNLOCK = 10;
   private static final double AIM_RANGE = 12.0;
   private static final float HEAL_BURST = 6.0F;
   private static final float SHIELD_HP = 8.0F;
   private static final int SHIELD_TICKS = 120;
   private static final int REGEN_TICKS = 80;
   private static final int REGEN_AMP = 1;
   private static final float CRITICAL_FRACTION = 0.35F;
   private static final double AIM_CONE_DEG = 8.0;
   private static final int WEAVE_TICKS = 12;
   private static final int HOLD_TICKS = 20;
   private static final int SELF_HOLD_TICKS = 80;
   private static final int PULSE_EVERY = 10;
   private static final double PULL_SPEED = 1.0;
   private static final int PULL_RAMP_TICKS = 5;
   private static final double PULL_LIFT = 0.08;
   private static final double ARRIVE_DIST = 2.4;
   private static final int MAX_PULL_TICKS = 40;
   private static final int FALL_PROTECT_TICKS = 60;
   private static final int SELF_INVULN_AMP = 4;
   private static final int SELF_PIN_SLOWNESS_AMP = 250;
   private static final double SELF_PIN_DRIFT_SQR = 0.02;
   private static final int SELF_PROTECT_TICKS = 92;
   private static final int UNRAVEL_SHELL_AT = 1;
   private static final int UNRAVEL_POD_AT = 5;
   private static final int UNRAVEL_TICKS = 10;
   private static final int COCOON_LIFE_CAP = 400;
   private static final double POD_MARGIN = 0.22;
   private static final double SHELL_MARGIN = 0.28;
   private static final double LASH_WIDTH = 0.09;
   private static final int CORD_EVERY = 2;
   private static final int CORD_LIFE = 3;
   private static final int PETAL_PINK = -1529656;
   private static final int PETAL_GOLD = -11718;
   private final Map<UUID, CradleOfRootsAbility.Cradle> cradles = this.activeState();

   public CradleOfRootsAbility() {
      super("cradle_of_roots", 0.0F, 10);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String costLabel() {
      return String.valueOf((int)this.billed(StaminaSystem.baseMax()));
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().healing(6.0).value("shield", AbilityScaling.Unit.RAW, 8.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean usesSneakActivate() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      return this.begin(player, false);
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 3) {
         this.begin(player, true);
      }
   }

   private boolean begin(ServerPlayer player, boolean self) {
      if (this.cradles.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         LivingEntity target = (LivingEntity)(self ? player : AimUtil.aimedLivingSoft(player, 12.0, 8.0, e -> isAlly(player, e)));
         if (target == null) {
            return this.fail(player, "awakened.feedback.no_cradle_target", new Object[0]);
         } else {
            PowerData data = PowerService.data(player);
            float cost = this.billed(StaminaSystem.baseMax());
            if (!StaminaSystem.canAfford(data, cost)) {
               return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
            } else {
               if (!data.infiniteStamina()) {
                  data.setStamina(data.stamina() - cost);
                  data.setLastSpendTime(level.getGameTime());
               }

               PowerService.sync(player);
               this.applyCocoon(player, target);
               CradleOfRootsAbility.Cradle cradle = new CradleOfRootsAbility.Cradle(target.getId(), self);
               this.cradles.put(player.getUUID(), cradle);
               Vec3 feet = player.position();
               PhytoFx.rootBurst(level, level.getRandom(), feet);
               PhytoFx.leafBurst(level, feet.add(0.0, 1.0, 0.0), 12);
               Vfx.ring(level, feet.add(0.0, 0.1, 0.0), 3.0, 9234544, 12);
               Fx.sound(level, feet, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.2F, 0.6F);
               Fx.sound(level, feet, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.7F, 1.2F);
               if (self) {
                  cradle.selfLock = player.position();
                  player.setDeltaMovement(Vec3.ZERO);
                  player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 92, 4, false, false));
                  player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 92, 250, false, false));
                  PowerService.playAnimation(player, "cradle_self");
                  this.beginWeave(level, cradle, player);
               } else {
                  fireLashes(player, level, cradle, target);
               }

               return true;
            }
         }
      }
   }

   private void applyCocoon(ServerPlayer caster, LivingEntity target) {
      boolean clutch = target.getHealth() < target.getMaxHealth() * 0.35F;
      target.heal(MasteryScaling.scaled(6.0F, caster));
      int shieldAmp = Math.max(0, Math.round(MasteryScaling.scaled(8.0F, caster) / 4.0F) - 1);
      target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, shieldAmp, false, false));
      target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, false));
      if (clutch) {
         MasterySystem.award(caster, MasteryReason.SURVIVAL);
         if (target instanceof Player) {
            QuestSystem.feat(caster, "phyto_cradle_save");
         }
      }
   }

   private static boolean isAlly(ServerPlayer caster, LivingEntity e) {
      return e != caster && !(e instanceof Enemy);
   }

   private static void fireLashes(ServerPlayer player, ServerLevel level, CradleOfRootsAbility.Cradle cradle, LivingEntity target) {
      Vec3 to = target.getBoundingBox().getCenter();
      List<Vec3> starts = List.of(
         AimUtil.handPosition(player, false), AimUtil.handPosition(player, true), player.position().add(player.getLookAngle().scale(0.8)).add(0.0, 0.1, 0.0)
      );
      List<List<Vec3>> lashes = new ArrayList<>(starts.size());
      int travel = 1;

      for (Vec3 from : starts) {
         List<Vec3> path = PhytoFx.whipPath(level.getRandom(), from, to);
         travel = Math.max(travel, PhytoFx.growVine(level, level.getRandom(), path, 0.09));
         lashes.add(path);
      }

      cradle.lashes = lashes;
      cradle.travelTicks = travel;
      Fx.sound(level, starts.get(0), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 0.6F);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      CradleOfRootsAbility.Cradle cradle = this.cradles.get(player.getUUID());
      if (cradle != null) {
         ServerLevel level = player.serverLevel();
         LivingEntity target = resolveTarget(player, level, cradle);
         if (target == null) {
            this.snap(player, level, cradle);
         } else {
            cradle.tick++;
            if (cradle.self && cradle.selfLock != null && (cradle.stage == CradleOfRootsAbility.Stage.WEAVE || cradle.stage == CradleOfRootsAbility.Stage.HOLD)
               )
             {
               pinSelf(player, cradle);
            }

            switch (cradle.stage) {
               case LASH:
                  this.tickLash(level, cradle, player, target);
                  break;
               case WEAVE:
                  this.tickWeave(level, cradle, player, target);
                  break;
               case HOLD:
                  this.tickHold(level, cradle, player, target);
                  break;
               case PULL:
                  this.tickPull(level, cradle, player, target);
                  break;
               case UNRAVEL:
                  this.tickUnravel(player, level, cradle, target);
            }
         }
      }
   }

   private static void pinSelf(ServerPlayer player, CradleOfRootsAbility.Cradle cradle) {
      player.setDeltaMovement(Vec3.ZERO);
      player.resetFallDistance();
      if (player.position().distanceToSqr(cradle.selfLock) > 0.02) {
         player.teleportTo(cradle.selfLock.x, cradle.selfLock.y, cradle.selfLock.z);
         player.hurtMarked = true;
      }
   }

   private static LivingEntity resolveTarget(ServerPlayer player, ServerLevel level, CradleOfRootsAbility.Cradle cradle) {
      if (cradle.self) {
         return player;
      } else {
         if (level.getEntity(cradle.targetId) instanceof LivingEntity t && t.isAlive()) {
            return t;
         }

         return null;
      }
   }

   private void tickLash(ServerLevel level, CradleOfRootsAbility.Cradle cradle, ServerPlayer player, LivingEntity target) {
      double t = Math.min(1.0, (double)cradle.tick / (double)cradle.travelTicks);

      for (List<Vec3> path : cradle.lashes) {
         PhytoFx.tipMotes(level, PhytoFx.pointAt(path, t), cradle.tick);
      }

      if (cradle.tick >= cradle.travelTicks) {
         this.beginWeave(level, cradle, target);
      }
   }

   private void beginWeave(ServerLevel level, CradleOfRootsAbility.Cradle cradle, LivingEntity target) {
      cradle.stage = CradleOfRootsAbility.Stage.WEAVE;
      cradle.tick = 0;
      Vec3 feet = target.position();
      EntityDimensions body = target.getDimensions(Pose.STANDING);
      double podW = (double)body.width() + 0.22;
      double podH = (double)body.height() + 0.22;
      double shellW = podW + 0.28;
      double shellH = podH + 0.28;
      cradle.shell = Vfx.blockState(level, feet, Blocks.MANGROVE_ROOTS.defaultBlockState(), shellW, shellH, shellW, 400, target.getYRot(), 10, true);
      cradle.pod = Vfx.blockState(level, feet, Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState(), podW, podH, podW, 400, target.getYRot() + 45.0F, 12, true);
      PhytoFx.leafBurst(level, feet.add(0.0, shellH * 0.5, 0.0), 8);
      Fx.sound(level, feet, SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.1F, 0.6F);
      Fx.sound(level, feet, SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 1.1F, 0.8F);
      Fx.sound(level, feet, SoundEvents.MOSS_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
   }

   private void tickWeave(ServerLevel level, CradleOfRootsAbility.Cradle cradle, ServerPlayer player, LivingEntity target) {
      followCocoon(cradle, target);
      Vec3 feet = target.position();
      if (cradle.tick % 3 == 1) {
         Vfx.effect(VfxEntity.Kind.HELIX, feet)
            .scale((double)target.getBbWidth() * 0.8 + 0.2)
            .secondary(new Vec3((double)target.getBbHeight() * 1.05, 3.5, 0.0))
            .color(4103220, 9234544)
            .lifetime(9)
            .spawn(level);
         Fx.sound(level, feet, SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 0.8F, 0.8F + 0.05F * (float)cradle.tick);
      }

      PhytoFx.fallingPetals(level, feet.add(0.0, (double)target.getBbHeight() * 0.8, 0.0), -1529656, 1);
      Fx.burst(level, PhytoFx.POLLEN_DUST, feet.add(0.0, (double)target.getBbHeight() * 0.6, 0.0), 2, 0.4, 0.02);
      if (cradle.tick >= 12) {
         if (cradle.self) {
            cradle.stage = CradleOfRootsAbility.Stage.HOLD;
            cradle.tick = 0;
         } else {
            beginPull(level, cradle, player, target);
         }
      }
   }

   private void tickHold(ServerLevel level, CradleOfRootsAbility.Cradle cradle, ServerPlayer player, LivingEntity target) {
      followCocoon(cradle, target);
      if (cradle.tick % 10 == 5) {
         pulse(level, cradle, target);
      }

      int hold = cradle.self ? 80 : 20;
      if (cradle.tick >= hold) {
         if (cradle.self) {
            beginUnravel(player, cradle);
         } else {
            beginPull(level, cradle, player, target);
         }
      }
   }

   private static void beginPull(ServerLevel level, CradleOfRootsAbility.Cradle cradle, ServerPlayer player, LivingEntity target) {
      cradle.stage = CradleOfRootsAbility.Stage.PULL;
      cradle.tick = 0;
      PowerService.stopAnimation(player);
      PowerService.playAnimation(player, "cradle_pull");
      Fx.sound(level, target.position(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 0.8F);
   }

   private static void pulse(ServerLevel level, CradleOfRootsAbility.Cradle cradle, LivingEntity target) {
      Vec3 feet = target.position();
      double r = (double)target.getBbWidth() * 0.9 + 0.6;
      Vfx.effect(VfxEntity.Kind.DOME, feet).scale(r).color(9234544, 16765498).lifetime(8).intensity(0.5F).spawn(level);
      Vfx.effect(VfxEntity.Kind.HALO, feet.add(0.0, 0.15, 0.0)).scale(r + 0.4).color(5816906, 16765498).lifetime(10).intensity(0.5F).spawn(level);
      Fx.burst(level, ParticleTypes.HEART, feet.add(0.0, (double)target.getBbHeight() + 0.4, 0.0), 1, 0.25, 0.0);
      Fx.burst(level, PhytoFx.POLLEN_DUST, feet.add(0.0, (double)target.getBbHeight() * 0.6, 0.0), 4, 0.4, 0.03);
      Fx.sound(level, feet, SoundEvents.MOSS_STEP, SoundSource.PLAYERS, 0.9F, 0.55F);
      Fx.sound(level, feet, SoundEvents.SPORE_BLOSSOM_PLACE, SoundSource.PLAYERS, 0.5F, 1.2F);
      Fx.burst(level, PhytoFx.MOSS_CRUMBS, feet.add(0.0, (double)target.getBbHeight() * 0.4, 0.0), 2, 0.3, 0.02);
   }

   private void tickPull(ServerLevel level, CradleOfRootsAbility.Cradle cradle, ServerPlayer player, LivingEntity target) {
      followCocoon(cradle, target);
      Vec3 casterChest = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
      Vec3 cocoon = target.getBoundingBox().getCenter();
      Vec3 toCaster = casterChest.subtract(cocoon);
      double dist = toCaster.length();
      if (!(dist <= 2.4) && cradle.tick < 40) {
         Vec3 dir = toCaster.scale(1.0 / dist);
         double speed = 1.0 * Math.min(1.0, ((double)cradle.tick + 1.0) / 5.0);
         target.setDeltaMovement(dir.x * speed, dir.y * speed + 0.08, dir.z * speed);
         target.hurtMarked = true;
         target.resetFallDistance();
         if (cradle.tick % 2 == 0) {
            Vec3 top = cocoon.add(0.0, (double)target.getBbHeight() * 0.35, 0.0);
            PhytoFx.tautVine(level, PhytoFx.sagPath(AimUtil.handPosition(player, false), top, 0.05), 0.07, 3);
            PhytoFx.tautVine(level, PhytoFx.sagPath(AimUtil.handPosition(player, true), top, 0.05), 0.07, 3);
         }

         if (cradle.tick % 10 == 5) {
            pulse(level, cradle, target);
         }

         Fx.burst(level, PhytoFx.LEAF_DUST, cocoon.subtract(dir.scale(0.8)), 2, 0.2, 0.02);
         if (cradle.tick % 4 == 0) {
            PhytoFx.fallingPetals(level, cocoon, -1529656, 1);
         }

         if (cradle.tick % 5 == 0) {
            Fx.sound(level, cocoon, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 0.5F, 0.8F + (float)Math.min(1.0, (double)cradle.tick / 40.0) * 0.4F);
         }
      } else {
         target.setDeltaMovement(target.getDeltaMovement().scale(0.2).add(0.0, 0.05, 0.0));
         target.hurtMarked = true;
         target.resetFallDistance();
         target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
         beginUnravel(player, cradle);
      }
   }

   private static void beginUnravel(ServerPlayer player, CradleOfRootsAbility.Cradle cradle) {
      PowerService.stopAnimation(player);
      cradle.stage = CradleOfRootsAbility.Stage.UNRAVEL;
      cradle.tick = 0;
   }

   private void tickUnravel(ServerPlayer player, ServerLevel level, CradleOfRootsAbility.Cradle cradle, LivingEntity target) {
      followCocoon(cradle, target);
      Vec3 mid = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
      if (cradle.tick == 1 && cradle.shell != null) {
         cradle.shell.discard();
         cradle.shell = null;
         Fx.burst(level, PhytoFx.WOOD_CRUMBS, mid, 14, 0.45, 0.06);
         Fx.sound(level, mid, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 1.1F, 0.7F);
      }

      if (cradle.tick == 5 && cradle.pod != null) {
         cradle.pod.discard();
         cradle.pod = null;
         PhytoFx.leafBurst(level, mid, 14);
         PhytoFx.fallingPetals(level, mid.add(0.0, 0.4, 0.0), -1529656, 6);
         PhytoFx.fallingPetals(level, mid.add(0.0, 0.4, 0.0), -11718, 4);
         Fx.burst(level, ParticleTypes.HEART, mid.add(0.0, (double)target.getBbHeight() * 0.5 + 0.3, 0.0), 2, 0.3, 0.0);
         Fx.sound(level, mid, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 1.1F, 0.9F);
         Fx.sound(level, mid, SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
         Fx.sound(level, mid, SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.6F, 1.3F);
      }

      if (cradle.tick >= 10) {
         this.cradles.remove(player.getUUID());
      }
   }

   private static void followCocoon(CradleOfRootsAbility.Cradle cradle, LivingEntity target) {
      if (cradle.shell != null) {
         cradle.shell.setPos(target.getX(), target.getY(), target.getZ());
      }

      if (cradle.pod != null) {
         cradle.pod.setPos(target.getX(), target.getY(), target.getZ());
      }
   }

   private void snap(ServerPlayer player, ServerLevel level, CradleOfRootsAbility.Cradle cradle) {
      Vec3 at = cradle.shell != null ? cradle.shell.position().add(0.0, 0.6, 0.0) : player.position().add(player.getLookAngle().scale(2.0));
      discardCocoon(cradle);
      this.cradles.remove(player.getUUID());
      PowerService.stopAnimation(player);
      Fx.burst(level, PhytoFx.WOOD_CRUMBS, at, 10, 0.4, 0.06);
      Fx.burst(level, PhytoFx.BARK_DUST, at, 6, 0.35, 0.05);
      Fx.sound(level, at, SoundEvents.VINE_BREAK, SoundSource.PLAYERS, 1.1F, 0.75F);
      Fx.sound(level, at, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 0.9F, 0.8F);
   }

   private static void discardCocoon(CradleOfRootsAbility.Cradle cradle) {
      if (cradle.shell != null) {
         cradle.shell.discard();
         cradle.shell = null;
      }

      if (cradle.pod != null) {
         cradle.pod.discard();
         cradle.pod = null;
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      CradleOfRootsAbility.Cradle cradle = this.cradles.get(player.getUUID());
      if (cradle != null) {
         discardCocoon(cradle);
         PowerService.stopAnimation(player);
         if (cradle.self) {
            removeOwn(player, MobEffects.DAMAGE_RESISTANCE, 4);
            removeOwn(player, MobEffects.MOVEMENT_SLOWDOWN, 250);
         }
      }

      super.clearPlayer(player);
   }

   private static void removeOwn(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
      MobEffectInstance running = player.getEffect(effect);
      if (running != null && running.getAmplifier() == amplifier) {
         player.removeEffect(effect);
      }
   }

   private static final class Cradle {
      final int targetId;
      final boolean self;
      CradleOfRootsAbility.Stage stage;
      int tick;
      int travelTicks;
      List<List<Vec3>> lashes = List.of();
      BlockFxEntity shell;
      BlockFxEntity pod;
      Vec3 selfLock;

      Cradle(int targetId, boolean self) {
         this.targetId = targetId;
         this.self = self;
         this.stage = self ? CradleOfRootsAbility.Stage.WEAVE : CradleOfRootsAbility.Stage.LASH;
      }
   }

   private static enum Stage {
      LASH,
      WEAVE,
      HOLD,
      PULL,
      UNRAVEL;
   }
}
