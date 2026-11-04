package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.ProneFlight;
import com.awakenedmod.awakened.core.caster.PlayerCaster;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class FireJetsAbility extends Ability {
   public static final String ANIMATION = "fire_jets";
   public static final String TAP_ANIMATION = "fire_jets_tap";
   public static final String LAND_ANIMATION = "fire_jets_land";
   public static final int RAMP_TICKS = 15;
   public static final double MAX_SPEED = 1.3;
   private static final int UNLOCK = 5;
   private static final int RETAP_TICKS = 10;
   private static final int TAP_BODY_TICKS = 14;
   private static final int LAND_BODY_TICKS = 9;
   private static final int SYNC_INTERVAL = 5;
   private static final double MASTERY_STEP = 24.0;
   private static final String K_JET_DIST = "pyroJetDist";
   private final Map<UUID, FireJetsEngine.Jet> jets = this.activeState();
   private final Map<UUID, Long> lastPress = this.playerState();
   private final Map<UUID, Long> lastLanding = this.playerState();
   private final Map<UUID, Long> retapWarned = this.playerState();
   private final List<Ability> guards;

   public FireJetsAbility(List<Ability> guards) {
      super("fire_jets", 0.0F, 5);
      this.guards = guards;
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String costLabel() {
      return (int)this.billed(12.0F) + "/s";
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean ownsBody(ServerPlayer player) {
      if (this.isActive(player)) {
         return true;
      } else {
         long now = player.serverLevel().getGameTime();
         return within(this.lastPress.get(player.getUUID()), now, 14) || within(this.lastLanding.get(player.getUUID()), now, 9);
      }
   }

   private static boolean within(Long stamp, long now, int window) {
      return stamp != null && now < stamp + (long)window;
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return false;
   }

   @Override
   public boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.NONE;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.jets.containsKey(player.getUUID())) {
         return this.failSilently(player);
      } else {
         for (Ability guard : this.guards) {
            if (guard.ownsBody(player)) {
               return this.fail(player, "awakened.fail.busy", new Object[0]);
            }
         }

         long now = player.serverLevel().getGameTime();
         Long last = this.lastPress.get(player.getUUID());
         if (last != null && now - last < 10L) {
            if (last.equals(this.retapWarned.get(player.getUUID()))) {
               return this.failSilently(player);
            } else {
               this.retapWarned.put(player.getUUID(), last);
               return this.fail(player, "awakened.fail.busy", new Object[0]);
            }
         } else if (!this.charge(player, 10.0F)) {
            return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            this.jets.put(player.getUUID(), FireJetsEngine.burst(PlayerCaster.of(player), player.getLookAngle()));
            this.lastPress.put(player.getUUID(), now);
            PowerService.playAnimation(player, "fire_jets_tap");
            PowerService.sync(player);
            return true;
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.end(player);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      FireJetsEngine.Jet jet = this.jets.get(player.getUUID());
      if (jet != null) {
         ServerLevel level = player.serverLevel();
         PowerData data = PowerService.data(player);
         int t = FireJetsEngine.advance(jet);
         if (t == 6) {
            PowerService.playAnimation(player, "fire_jets", true);
         }

         if (t >= 6 && !data.infiniteStamina()) {
            float drain = this.billed(0.6F);
            if (data.stamina() < drain) {
               PowerService.feedback(player, 0);
               this.end(player);
               return;
            }

            StaminaSystem.drainSustained(data, level.getGameTime(), drain);
            if (player.tickCount % 5 == 0) {
               PowerService.sync(player);
            }
         }

         boolean cruiseFlipped = FireJetsEngine.thrust(PlayerCaster.of(player), jet, t, steering(player));
         float acc = data.var("pyroJetDist", 0.0F) + (float)Math.min(jet.moved(), jet.commanded());
         if ((double)acc >= 24.0) {
            acc -= 24.0F;
            MasterySystem.awardUse(player);
         }

         data.setVar("pyroJetDist", acc);
         if (cruiseFlipped) {
            ProneFlight.set(player, jet.cruising());
         }
      }
   }

   private static FireJetsEngine.Steering steering(ServerPlayer player) {
      final Vec3 look = player.getLookAngle();
      final double strafe = 0.0;
      final double lift = 0.0;
      return new FireJetsEngine.Steering() {
         @Override
         public Vec3 direction() {
            return look;
         }

         @Override
         public double strafe() {
            return strafe;
         }

         @Override
         public double lift() {
            return lift;
         }
      };
   }

   private void end(ServerPlayer player) {
      FireJetsEngine.Jet jet = this.jets.remove(player.getUUID());
      if (jet != null) {
         ProneFlight.set(player, false);
         if (jet.engaged()) {
            PowerService.playAnimation(player, "fire_jets_land");
            this.lastLanding.put(player.getUUID(), player.serverLevel().getGameTime());
            FireJetsEngine.land(PlayerCaster.of(player));
         }

         PowerService.sync(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.jets.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
      }

      ProneFlight.set(player, false);
      super.clearPlayer(player);
   }
}
