package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.core.Headroom;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.server.level.ServerPlayer;

public final class SizeChangeAbility extends Ability {
   public static final float BURST_COST = 3.0F;
   public static final String K_LAST_SHIFT = "scaler_last_shift";
   public static final String K_SHIFT_AIR = "scaler_shift_air";
   public static final String K_SHIFT_DIR = "scaler_shift_dir";

   public SizeChangeAbility() {
      super("size_change", 3.0F, 0);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      return this.shift(player, 1, false);
   }

   @Override
   public boolean usesSneakActivate() {
      return true;
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 3) {
         this.shift(player, -1, true);
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   private boolean shift(ServerPlayer player, int dir, boolean selfCharge) {
      PowerData data = PowerService.data(player);
      SizeStage from = Scaling.stage(data);
      SizeStage to = from.shifted(dir);
      if (to == from) {
         return this.fail(player, dir > 0 ? "awakened.scaler.at_top" : "awakened.scaler.at_bottom", new Object[0]);
      } else if (to.unlockDistance() > Scaling.maxUnlockedDistance(data)) {
         return this.fail(player, "awakened.scaler.locked", new Object[0]);
      } else if (dir > 0 && !Headroom.fits(player, (double)to.scale())) {
         return this.fail(player, "awakened.scaler.no_room", new Object[0]);
      } else {
         if (selfCharge) {
            float cost = this.billed(3.0F);
            if (!StaminaSystem.canAfford(data, cost)) {
               return this.fail(player, "awakened.scaler.tired", new Object[0]);
            }

            if (!data.infiniteStamina()) {
               data.setStamina(data.stamina() - cost);
               data.setLastSpendTime(player.serverLevel().getGameTime());
            }
         }

         Scaling.setStage(data, to);
         data.setVar("scaler_last_shift", (float)player.serverLevel().getGameTime());
         data.setVar("scaler_shift_air", player.onGround() ? 0.0F : 1.0F);
         data.setVar("scaler_shift_dir", (float)dir);
         ScalerFx.shift(player, dir > 0, (double)Math.max(from.scale(), to.scale()));
         PowerService.sync(player);
         return true;
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      Scaling.clear(player);
      super.clearPlayer(player);
   }
}
