package com.awakenedmod.awakened.content.limitless.domain;

import com.awakenedmod.awakened.content.limitless.LimitlessPower;
import com.awakenedmod.awakened.core.AbilityGate;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.PowerResource;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class InfiniteVoidAbility extends Ability {
   public static final String ID = "limitless_void";
   private static final String HAND_SIGN = "limitless_hand_sign";
   private static final AbilityScaling.Entry RADIUS = new AbilityScaling.Entry(
      "radius", AbilityScaling.Unit.BLOCKS, AbilityScaling.Kind.MULTIPLIER, 12.0, 16, 0.0
   );

   public InfiniteVoidAbility() {
      super("limitless_void", 50.0F, 10);
      AbilityGate.register(p -> VoidControl.isHeld(p) && !VoidBody.of(p).caster());
      AbilityGate.registerPressLock(VoidControl::isHeld);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return VoidControl.isUp(player) ? 0.0F : 50.0F;
   }

   @Override
   public String costLabel() {
      return Math.round(this.billed(50.0F)) + " + " + Math.round(this.billed(30.0F)) + "/s";
   }

   @Override
   public float staminaCost(Player viewer) {
      return litOnClient(viewer) ? 0.0F : 50.0F;
   }

   @Override
   public String costLabel(Player viewer) {
      return litOnClient(viewer) ? "0" : this.costLabel();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (VoidControl.isUp(player)) {
         if (VoidControl.inIntro(player)) {
            return this.failSilently(player);
         } else {
            VoidControl.endEarly(player.getUUID());
            return true;
         }
      } else if (LimitlessPower.castingOther(player, this)) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         double radius = RADIUS.after(PowerService.data(player).masteryLevel(), MasteryScaling.abilityPower(player));
         if (VoidControl.wouldOverlap(player, radius)) {
            return this.fail(player, "awakened.limitless.void_overlap", new Object[0]);
         } else {
            VoidControl.open(player, radius);
            PowerService.playAnimation(player, "limitless_hand_sign");
            return true;
         }
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean ignoresSealWhileActive() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().add(RADIUS).build();
   }

   @Override
   public void serverTick(ServerPlayer player) {
      if (VoidControl.isUp(player) && !VoidControl.inIntro(player)) {
         PowerResource resource = this.resource(player);
         float perSecond = 30.0F + 10.0F * (float)VoidControl.sinceIntro(player) / 20.0F;
         float drain = StaminaSystem.effectiveCost(perSecond / 20.0F, this.balanceKey());
         if (!resource.canAfford(player, drain)) {
            PowerService.feedback(player, 0);
            VoidControl.endEarly(player.getUUID());
         } else {
            resource.sustain(player, drain);
            if (player.tickCount % 5 == 0) {
               PowerService.sync(player);
            }
         }
      }
   }

   @Override
   public boolean isActive(ServerPlayer player) {
      return VoidControl.isUp(player);
   }

   @Override
   public boolean occupiesHands(ServerPlayer player) {
      return false;
   }

   @Override
   public boolean ownsBody(ServerPlayer player) {
      return false;
   }

   public static boolean litOnClient(Player viewer) {
      VoidBody body = VoidBody.of(viewer);
      return body.caster() && body.inVoid();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      VoidControl.endEarly(player.getUUID());
      super.clearPlayer(player);
   }
}
