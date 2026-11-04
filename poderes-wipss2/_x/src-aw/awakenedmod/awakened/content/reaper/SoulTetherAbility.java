package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class SoulTetherAbility extends Ability {
   private static final float COST = 20.0F;
   private static final int UNLOCK = 5;
   private static final double ACQUIRE_RANGE = 12.0;
   private static final double BREAK_RANGE = 12.0;
   private static final int DURATION_TICKS = 100;
   private static final float DRAIN_PER_SECOND = 2.0F;
   private static final int DRAIN_INTERVAL = 10;
   private static final float LIFESTEAL = 0.6F;
   private static final float DRAIN_STAMINA_PER_TICK = 0.8F;
   private static final float DRAIN_PHASE_STEP = 0.12F;
   private final Map<UUID, SoulTetherAbility.Tether> tethers = this.activeState();

   public SoulTetherAbility() {
      super("soul_tether", 20.0F, 5);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.tethers.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         LivingEntity target = AimUtil.aimedLivingEntity(player, 12.0, e -> e != player && Harm.allowed(player, e));
         if (target == null) {
            return this.fail(player, "awakened.reaper.tether_no_target", new Object[0]);
         } else if (!Harm.controls(player, target)) {
            return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
         } else {
            this.tethers.put(player.getUUID(), new SoulTetherAbility.Tether(target));
            TickFreeze.freeze(target);
            ReaperFx.tetherSnap(player.serverLevel(), targetCenter(target), true);
            MasterySystem.award(player, MasteryReason.HIT, target);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      SoulTetherAbility.Tether t = this.tethers.get(player.getUUID());
      if (t != null) {
         ServerLevel level = player.serverLevel();
         LivingEntity target = t.target;
         if (target.isAlive() && !target.isRemoved() && !(player.distanceToSqr(target) > 144.0) && hasLineOfSight(player, target) && ++t.ticks <= 100) {
            PowerData data = PowerService.data(player);
            long gameTime = level.getGameTime();
            StaminaSystem.suppressRegen(data, gameTime);
            if (!data.infiniteStamina()) {
               float drain = this.billed(0.8F);
               if (data.stamina() < drain) {
                  this.end(player, t);
                  return;
               }

               StaminaSystem.drainSustained(data, gameTime, drain);
               if (player.tickCount % 3 == 0) {
                  PowerService.sync(player);
               }
            }

            Vec3 hand = AimUtil.handPosition(player);
            Vec3 center = targetCenter(target);
            ReaperFx.tetherBeam(level, hand, center);
            t.phase = (t.phase + 0.12F) % 1.0F;
            ReaperFx.tetherDrain(level, center, hand, t.phase);
            if (++t.drainTimer >= 10) {
               t.drainTimer = 0;
               long souls = ReaperAttachments.souls(player).souls();
               float perPulse = 1.0F;
               float bite = MasteryScaling.scaledDamage(perPulse, player, this.balanceKey(), (float)ReaperScaling.offense(souls) - 1.0F);
               boolean aliveBefore = target.isAlive();
               float before = target.getHealth();
               Dmg.hurt(target, level, Dmg.by(player, DamageTypes.MAGIC), bite);
               float drained = Math.min(Math.max(0.0F, before - target.getHealth()), bite);
               if (drained > 0.0F) {
                  player.heal(0.6F * drained);
               }

               if (aliveBefore && !target.isAlive()) {
                  MasterySystem.award(player, MasteryReason.KILL);
                  if (Foes.countsAsKill(player, target)) {
                     QuestSystem.feat(player, "reaper_drain");
                  }
               }
            }

            PowerService.playAnimation(player, "soul_tether");
         } else {
            this.end(player, t);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      SoulTetherAbility.Tether t = this.tethers.get(player.getUUID());
      if (t != null) {
         this.end(player, t);
      }
   }

   private void end(ServerPlayer player, SoulTetherAbility.Tether t) {
      if (this.tethers.remove(player.getUUID()) != null) {
         if (t.target != null) {
            TickFreeze.release(t.target);
         }

         ServerLevel var4 = player.serverLevel();
         if (var4 instanceof ServerLevel && t.target != null) {
            ReaperFx.tetherSnap(var4, targetCenter(t.target), false);
         }

         PowerService.stopAnimation(player);
      }
   }

   private static Vec3 targetCenter(LivingEntity target) {
      return target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
   }

   private static boolean hasLineOfSight(ServerPlayer player, LivingEntity target) {
      Vec3 from = player.getEyePosition();
      Vec3 to = targetCenter(target);
      ClipContext ctx = new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, player);
      return player.serverLevel().clip(ctx).getType() != Type.BLOCK;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      SoulTetherAbility.Tether t = this.tethers.get(player.getUUID());
      if (t != null) {
         this.end(player, t);
      }

      super.clearPlayer(player);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "soul_tether";
   }

   private static final class Tether {
      private final LivingEntity target;
      private int ticks;
      private int drainTimer;
      private float phase;

      private Tether(LivingEntity target) {
         this.target = target;
      }
   }
}
