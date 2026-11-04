package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class RuinGripAbility extends RuinAbility {
   public static final String ID = "ruin_grip";
   private static final String CLIP_HOLD = "ruin_grip_hold";
   private static final String CLIP_RELEASE = "ruin_grip_release";
   public static final ResourceLocation ROOT_ID = AwId.of("ruin_grip_root");
   private final Map<UUID, RuinGripAbility.Hold> holds = this.activeState();
   private final Map<UUID, Map<Integer, Long>> spent = this.playerState();
   static final AbilityScaling.Entry STACKS = new AbilityScaling.Entry(
      "ruin_grip_stacks", AbilityScaling.Unit.COUNT, AbilityScaling.Kind.PER_POINT, 2.0, 4, 0.1
   );
   static final AbilityScaling.Entry LENGTH = new AbilityScaling.Entry("duration", AbilityScaling.Unit.SECONDS, AbilityScaling.Kind.PER_POINT, 4.0, 6, 0.2);

   public RuinGripAbility() {
      super("ruin_grip", 6.0F, 0);
   }

   @Override
   public String costLabel() {
      return holdCostLabel(this.billed(6.0F), this.billed(0.5F));
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().add(STACKS).add(LENGTH).build();
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "ruin_grip";
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (busy(player)) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         LivingEntity target = AimUtil.aimedLivingEntity(player, 3.5, e -> takeable(player, e));
         if (target == null) {
            return this.fail(player, "awakened.ruin.grip_no_target", new Object[0]);
         } else if (!Harm.controls(player, target)) {
            return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
         } else if (this.stillSpent(player, target)) {
            return this.fail(player, "awakened.ruin.grip_spent", new Object[0]);
         } else {
            this.start(player, target);
            return true;
         }
      }
   }

   static boolean grabbable(ServerPlayer player, LivingEntity e) {
      return takeable(player, e) && Harm.controls(player, e);
   }

   private static boolean takeable(ServerPlayer player, LivingEntity e) {
      return e.isAlive() && RuinStacks.accepts(e, player) && !Foes.fightsFor(player, e) && player.hasLineOfSight(e);
   }

   private void start(ServerPlayer player, LivingEntity target) {
      boolean freeze = !(target instanceof Player);
      if (freeze) {
         TickFreeze.freeze(target);
      }

      this.holds.put(player.getUUID(), new RuinGripAbility.Hold(target, freeze, poweredCount(STACKS, player), (int)Math.round(powered(LENGTH, player) * 20.0)));
      root(player, true);
      hold(player, target);
      RuinFx.gripReach(player);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      RuinGripAbility.Hold h = this.holds.get(player.getUUID());
      if (h != null) {
         LivingEntity target = h.target;
         if (!target.isAlive() || target.isRemoved() || target.level() != player.serverLevel() || !RuinStacks.accepts(target, player)) {
            this.end(player, h);
         } else if (h.ticks >= 4 && (double)player.distanceTo(target) > 5.0) {
            this.end(player, h);
            this.fail(player, "awakened.ruin.grip_broken", new Object[0]);
         } else if (!this.drainTick(player, 0.5F, h.ticks)) {
            this.end(player, h);
            ranDry(player);
         } else {
            hold(player, target);
            lift(target, h);
            ServerLevel level = player.serverLevel();
            Vec3 grip = RuinStacks.touchPoint(player, target);
            h.ticks++;
            if (h.ticks == 6) {
               PowerService.playAnimation(player, "ruin_grip_hold", true);
            }

            int sinceFirst = h.ticks - 5;
            if (sinceFirst == 0) {
               RuinFx.gripClose(level, grip);
            }

            if (sinceFirst >= 0 && sinceFirst % 10 == 0 && RuinStacks.add(target, h.stacks, player, grip)) {
               RuinFx.gripStack(level, target, grip, RuinStacks.stacks(target));
            }

            RuinFx.gripTick(level, player, target, grip, h.ticks, h.maxTicks, closed(h));
            if (h.ticks >= h.maxTicks) {
               this.end(player, h);
            }
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      RuinGripAbility.Hold h = this.holds.get(player.getUUID());
      if (h != null) {
         this.end(player, h);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      RuinGripAbility.Hold h = this.holds.remove(player.getUUID());
      if (h != null) {
         letGo(h);
         root(player, false);
         RuinFx.gripSilence(player, h.target);
         PowerService.stopAnimation(player);
         PowerService.playAnimation(player, "ruin_grip_release");
      }

      super.clearPlayer(player);
   }

   private void end(ServerPlayer player, RuinGripAbility.Hold h) {
      if (this.holds.remove(player.getUUID()) != null) {
         letGo(h);
         root(player, false);
         this.lockOut(player, h);
         RuinFx.gripEnd(player.serverLevel(), player, h.target, RuinStacks.touchPoint(player, h.target), closed(h));
         PowerService.stopAnimation(player);
         PowerService.playAnimation(player, "ruin_grip_release");
         PowerService.sync(player);
      }
   }

   private void lockOut(ServerPlayer player, RuinGripAbility.Hold h) {
      if (closed(h)) {
         long until = player.serverLevel().getGameTime() + Math.round((double)h.ticks * 1.0);
         this.spent.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(h.target.getId(), until);
      }
   }

   private boolean stillSpent(ServerPlayer player, LivingEntity target) {
      Map<Integer, Long> mine = this.spent.get(player.getUUID());
      if (mine == null) {
         return false;
      } else {
         long now = player.serverLevel().getGameTime();
         mine.values().removeIf(until -> until <= now);
         if (mine.isEmpty()) {
            this.spent.remove(player.getUUID());
         }

         return mine.containsKey(target.getId());
      }
   }

   private static boolean closed(RuinGripAbility.Hold h) {
      return h.ticks >= 5;
   }

   private static void root(ServerPlayer player, boolean on) {
      double amount = on ? -1.0 : 0.0;
      Attrs.set(player, Attributes.MOVEMENT_SPEED, ROOT_ID, amount, Operation.ADD_MULTIPLIED_TOTAL);
      Attrs.set(player, Attributes.JUMP_STRENGTH, ROOT_ID, amount, Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static void letGo(RuinGripAbility.Hold h) {
      if (h.frozen) {
         TickFreeze.release(h.target);
      }
   }

   private static void lift(LivingEntity target, RuinGripAbility.Hold h) {
      if (h.frozen) {
         double t = Math.min(1.0, (double)(h.ticks + 1) / 5.0);
         double y = h.base.y + 0.5 * t * t * (3.0 - 2.0 * t);
         double dy = y - target.getY();
         if (!(Math.abs(dy) < 0.001) && target.level().noCollision(target, target.getBoundingBox().move(0.0, dy, 0.0))) {
            target.setPos(h.base.x, y, h.base.z);
         }
      }
   }

   private static void hold(ServerPlayer player, LivingEntity target) {
      if (target instanceof Player) {
         lockPlayer(player, target);
         Harm.setMotion(player, target, 0.0, Math.min(0.0, target.getDeltaMovement().y), 0.0);
      }
   }

   static void lockPlayer(ServerPlayer holder, LivingEntity target) {
      Harm.effect(holder, target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 6, false, false));
      Harm.effect(holder, target, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6, 2, false, false));
   }

   private static final class Hold {
      final LivingEntity target;
      final boolean frozen;
      final Vec3 base;
      final int stacks;
      final int maxTicks;
      int ticks;

      Hold(LivingEntity target, boolean frozen, int stacks, int maxTicks) {
         this.target = target;
         this.frozen = frozen;
         this.base = target.position();
         this.stacks = stacks;
         this.maxTicks = maxTicks;
      }
   }
}
