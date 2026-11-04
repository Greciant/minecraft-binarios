package com.awakenedmod.awakened.content.gamma;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class ProvokeAbility extends Ability {
   private static final float COST = 12.0F;
   private static final float PROVOKE_CAP_BASE = 0.35F;
   private static final float PROVOKE_CAP_MAX = 1.0F;
   private static final int PROVOKE_CAP_MASTERY_LEVEL = 10;
   private static final float RAGE_PER_BEAT = 0.09F;
   private static final int BEAT_TICKS = 4;
   private static final float DRAIN_PER_TICK = 2.85F;
   private final Map<UUID, ProvokeAbility.Provoking> channeling = this.activeState();

   public ProvokeAbility() {
      super("gamma_provoke", 12.0F, 0);
   }

   @Override
   public String costLabel() {
      return (int)this.billed(57.0F) + "/s";
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      UUID id = player.getUUID();
      if (this.channeling.containsKey(id)) {
         return false;
      } else {
         PowerData data = PowerService.data(player);
         if (Rage.rage(data) >= provokeCap(data)) {
            return this.fail(player, "ability.awakened.gamma_provoke.capped", new Object[0]);
         } else {
            this.channeling.put(id, new ProvokeAbility.Provoking());
            PowerService.playAnimation(player, "gamma_provoke");
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      ProvokeAbility.Provoking p = this.channeling.get(player.getUUID());
      if (p != null) {
         PowerData data = PowerService.data(player);
         float cap = provokeCap(data);
         if (Rage.rage(data) >= cap) {
            this.finish(player);
         } else {
            if (!data.infiniteStamina()) {
               float remaining = StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), this.billed(2.85F));
               PowerService.sync(player);
               if (remaining <= 0.0F) {
                  this.finish(player);
                  return;
               }
            }

            if (p.ticks++ % 4 == 0) {
               Rage.provoke(player, data, 0.09F, cap);
               GammaFx.provokePound(player.serverLevel(), player, Rage.rage(data) / cap);
            }
         }
      }
   }

   private static float provokeCap(PowerData data) {
      int level = Math.max(0, data.masteryLevel());
      float t = Math.min(1.0F, (float)level / 10.0F);
      return 0.35F + 0.65F * t;
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.finish(player);
   }

   private void finish(ServerPlayer player) {
      if (this.channeling.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
         PowerService.playAnimation(player, "gamma_provoke_roar");
         GammaFx.provoke(player.serverLevel(), player, Rage.rage(PowerService.data(player)));
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.channeling.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
      }

      Rage.clear(player);
      GammaBeats.clear(player.getUUID());
      super.clearPlayer(player);
   }

   private static final class Provoking {
      private int ticks;
   }
}
