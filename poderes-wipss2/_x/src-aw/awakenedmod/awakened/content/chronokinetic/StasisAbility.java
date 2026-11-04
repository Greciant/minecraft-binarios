package com.awakenedmod.awakened.content.chronokinetic;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.skill.Stat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class StasisAbility extends Ability {
   private static final float COST = 35.0F;
   private static final int UNLOCK = 5;
   private static final double AIM_RANGE = 24.0;
   private static final int DURATION_TICKS = 52;
   private static final int MAX_DURATION_TICKS = 100;
   private static final int BASE_TARGETS = 1;
   private final Map<UUID, List<StasisAbility.Held>> held = this.activeState();
   private final Map<UUID, Long> regenLock = this.activeState();

   public StasisAbility() {
      super("stasis", 35.0F, 5);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().durationTicks(52, 100).targets(1, 0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      LivingEntity target = AimUtil.aimedLivingEntity(player, 24.0, e -> Harm.allowed(player, e));
      if (target == null) {
         return this.fail(player, "awakened.chronokinetic.stasis_no_target", new Object[0]);
      } else if (!Harm.controls(player, target)) {
         return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
      } else if (TickFreeze.isFrozen(target)) {
         return this.fail(player, "awakened.chronokinetic.stasis_already", new Object[0]);
      } else {
         List<StasisAbility.Held> list = this.held.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
         if (list.size() >= maxTargets(player)) {
            return this.fail(player, "awakened.chronokinetic.stasis_full", new Object[0]);
         } else {
            ServerLevel level = player.serverLevel();
            ChronoSupport.freeze(target);
            int ticks = MasteryScaling.scaledTicks(52, 100, player);
            list.add(new StasisAbility.Held(target, target.position(), ticks));
            ChronoFx.stasisLock(level, target, ticks);
            this.castFeedback(level, player, target);
            MasterySystem.award(player, MasteryReason.HIT, target);
            return true;
         }
      }
   }

   private static int maxTargets(ServerPlayer player) {
      int bonus = Math.round(MasteryScaling.abilityPower(player) / Stat.ABILITY_POWER.perPoint());
      return 1 + Math.max(0, bonus);
   }

   private void castFeedback(ServerLevel level, ServerPlayer player, LivingEntity target) {
      player.displayClientMessage(Component.translatable("awakened.chronokinetic.suspended").withStyle(ChatFormatting.GOLD), true);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      Long lockedUntil = this.regenLock.get(player.getUUID());
      if (lockedUntil != null && player.serverLevel().getGameTime() >= lockedUntil) {
         this.regenLock.remove(player.getUUID());
      }

      List<StasisAbility.Held> list = this.held.get(player.getUUID());
      if (list != null && !list.isEmpty()) {
         Iterator<StasisAbility.Held> it = list.iterator();

         while (it.hasNext()) {
            StasisAbility.Held h = it.next();
            if (h.target != null && h.target.isAlive()) {
               ChronoSupport.hold(h.target, h.anchor);
               if (--h.ticksLeft <= 0) {
                  this.release(player.serverLevel(), h.target);
                  this.lockRegenAfter(player, h);
                  it.remove();
               }
            } else {
               if (h.target != null) {
                  if (Foes.defeatedBy(h.target, player)) {
                     QuestSystem.feat(player, "chrono_stasis_kill");
                  }

                  ChronoSupport.release(h.target);
                  ChronoFx.stasisRelease(player.serverLevel(), h.target);
                  this.lockRegenAfter(player, h);
               }

               it.remove();
            }
         }

         if (list.isEmpty()) {
            this.held.remove(player.getUUID());
         }
      }
   }

   private void lockRegenAfter(ServerPlayer player, StasisAbility.Held h) {
      if (h.target instanceof Player) {
         long until = player.serverLevel().getGameTime() + (long)(h.duration - Math.max(0, h.ticksLeft));
         this.regenLock.merge(player.getUUID(), until, Math::max);
      }
   }

   private void release(ServerLevel level, LivingEntity target) {
      ChronoSupport.release(target);
      ChronoFx.stasisRelease(level, target);
   }

   @Override
   public boolean occupiesHands(ServerPlayer player) {
      return false;
   }

   @Override
   public boolean ownsBody(ServerPlayer player) {
      return false;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      List<StasisAbility.Held> list = this.held.remove(player.getUUID());
      if (list != null) {
         for (StasisAbility.Held h : list) {
            if (h.target != null) {
               ChronoSupport.release(h.target);
               if (h.target.level() instanceof ServerLevel level) {
                  ChronoFx.stasisRelease(level, h.target);
               }
            }
         }
      }

      super.clearPlayer(player);
   }

   private static final class Held {
      private final LivingEntity target;
      private final Vec3 anchor;
      private final int duration;
      private int ticksLeft;

      private Held(LivingEntity target, Vec3 anchor, int ticksLeft) {
         this.target = target;
         this.anchor = anchor;
         this.duration = ticksLeft;
         this.ticksLeft = ticksLeft;
      }
   }
}
