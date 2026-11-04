package com.awakenedmod.awakened.content.limitless.rct;

import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import com.awakenedmod.awakened.content.limitless.LimitlessFx;
import com.awakenedmod.awakened.content.limitless.LimitlessPower;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.PowerResource;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

public final class RctAbility extends Ability {
   public static final String ID = "limitless_rct";
   public static final String ANIMATION = "limitless_rct";
   private final Map<UUID, RctAbility.Heal> heals = this.activeState();
   private final Map<UUID, RecentHarm> harm = this.playerState();

   public RctAbility() {
      super("limitless_rct", 0.0F, 7);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String costLabel() {
      return (int)this.billed(8.0F) + "/s";
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().value("healing_per_second", AbilityScaling.Unit.RAW, 2.0).build();
   }

   public void noteHarm(ServerPlayer player, float amount) {
      this.harm.computeIfAbsent(player.getUUID(), k -> new RecentHarm(200)).add(player.serverLevel().getGameTime(), amount);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.heals.containsKey(player.getUUID())) {
         return this.failSilently(player);
      } else if (LimitlessPower.castingOther(player, this)) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         PowerResource resource = this.resource(player);
         if (!resource.canAfford(player, this.drain())) {
            resource.refuse(player);
            return this.failSilently(player);
         } else {
            this.begin(player);
            return true;
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.end(player);
   }

   private void begin(ServerPlayer player) {
      int now = (int)player.serverLevel().getGameTime();
      this.heals.put(player.getUUID(), new RctAbility.Heal(now));
      player.clearFire();
      player.removeEffect(MobEffects.POISON);
      player.removeEffect(MobEffects.WITHER);
      player.setSprinting(false);
      LimitlessCast.set(player, LimitlessCast.RCT, LimitlessCast.started(now));
      PowerService.playAnimation(player, "limitless_rct", true);
      LimitlessFx.rctStarted(player.serverLevel(), player);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      RctAbility.Heal heal = this.heals.get(player.getUUID());
      if (heal != null) {
         PowerResource resource = this.resource(player);
         float drain = this.drain();
         if (!resource.canAfford(player, drain)) {
            PowerService.feedback(player, 0);
            this.end(player);
         } else {
            resource.sustain(player, drain);
            if (player.isSprinting()) {
               player.setSprinting(false);
            }

            int ticks = (int)player.serverLevel().getGameTime() - heal.start();
            if (ticks > 0 && ticks % 5 == 0) {
               this.pulse(player);
            }

            if (ticks % 5 == 0) {
               PowerService.sync(player);
            }

            LimitlessFx.rctHum(player);
         }
      }
   }

   private void pulse(ServerPlayer player) {
      float before = player.getHealth();
      if (!(before >= player.getMaxHealth())) {
         player.heal(MasteryScaling.scaled(0.5F, player));
         float healed = player.getHealth() - before;
         RecentHarm pool = this.harm.get(player.getUUID());
         if (pool != null && !(healed <= 0.0F)) {
            long now = player.serverLevel().getGameTime();
            if (pool.consume(now, healed) > 0.0F) {
               MasterySystem.award(player, MasteryReason.SURVIVAL);
            }

            if (pool.isEmpty(now)) {
               this.harm.remove(player.getUUID());
            }
         }
      }
   }

   private void end(ServerPlayer player) {
      if (this.heals.remove(player.getUUID()) != null) {
         PowerService.stopHeldAnimation(player, "limitless_rct");
         int now = (int)player.serverLevel().getGameTime();
         LimitlessCast.end(player, LimitlessCast.RCT, now, false);
         LimitlessFx.rctEnded(player);
         PowerService.sync(player);
      }
   }

   private float drain() {
      return StaminaSystem.effectiveCost(0.4F, this.balanceKey());
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.end(player);
      super.clearPlayer(player);
   }

   private static record Heal(int start) {
   }
}
