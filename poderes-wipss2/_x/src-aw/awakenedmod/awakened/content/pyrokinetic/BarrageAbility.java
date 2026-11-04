package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.caster.PlayerCaster;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class BarrageAbility extends Ability {
   private static final String ID = "barrage";
   public static final String ANIMATION = "pyro_barrage";
   public static final int SHOT_INTERVAL = 6;
   private static final int UNLOCK = 7;
   private static final int SYNC_INTERVAL = 5;
   private final Map<UUID, BarrageEngine.Volley> volleys = this.activeState();
   private final List<Ability> guards;

   public BarrageAbility(List<Ability> guards) {
      super("barrage", 0.0F, 7);
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
   public boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(3.0).value("burning_ground", AbilityScaling.Unit.RAW, 2.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.volleys.containsKey(player.getUUID())) {
         return this.failSilently(player);
      } else {
         for (Ability guard : this.guards) {
            if (guard.ownsBody(player)) {
               return this.fail(player, "awakened.fail.busy", new Object[0]);
            }
         }

         PlayerCaster caster = PlayerCaster.of(player);
         if (!BarrageEngine.hasRoom(caster)) {
            return this.fail(player, "awakened.pyrokinetic.barrage.no_room", new Object[0]);
         } else if (!this.charge(player, 6.0F)) {
            return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            this.volleys.put(player.getUUID(), new BarrageEngine.Volley());
            PowerService.playAnimation(player, "pyro_barrage", true);
            BarrageEngine.open(caster);
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
      BarrageEngine.Volley volley = this.volleys.get(player.getUUID());
      if (volley != null) {
         PowerData data = PowerService.data(player);
         boolean billed = !data.infiniteStamina();
         float drain = this.billed(0.6F);
         if (billed && data.stamina() < drain) {
            PowerService.feedback(player, 0);
            this.end(player);
         } else {
            switch (BarrageEngine.tick(PlayerCaster.of(player), volley)) {
               case END:
                  this.end(player);
               case SKIP:
               default:
                  break;
               case BILL:
                  if (billed) {
                     StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), drain);
                     if (player.tickCount % 5 == 0) {
                        PowerService.sync(player);
                     }
                  }
            }
         }
      }
   }

   private void end(ServerPlayer player) {
      if (this.volleys.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
         BarrageEngine.close(PlayerCaster.of(player));
         PowerService.sync(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      PyroBurningGround.clear(player);
      if (this.volleys.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }
}
