package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class FullReleaseAbility extends Ability {
   public static final String ID = "full_release";
   private static final int UNLOCK = 9;
   private static final int JUMP_TICKS = 10;
   private static final String STEP_CLIP = "full_release_step";
   private final Map<UUID, FullReleaseAbility.Press> presses = this.playerState();

   public FullReleaseAbility() {
      super("full_release", 0.0F, 9);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      this.presses.put(player.getUUID(), new FullReleaseAbility.Press(player.isShiftKeyDown()));
      return true;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      FullReleaseAbility.Press press = this.presses.get(player.getUUID());
      if (press != null && !press.spent) {
         if (++press.ticks >= 10) {
            press.spent = true;
            this.move(player, press.down, true);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      FullReleaseAbility.Press press = this.presses.remove(player.getUUID());
      if (press != null && !press.spent) {
         this.move(player, press.down, false);
      }
   }

   private void move(ServerPlayer player, boolean down, boolean jump) {
      PowerData data = PowerService.data(player);
      int level = FullReleaseState.level(data);
      if (down) {
         if (level <= 0) {
            this.failSilently(player);
         } else {
            FullReleaseState.set(player, data, jump ? 0 : level - 1);
            PowerService.playAnimation(player, "full_release_step");
         }
      } else if (level >= 10) {
         this.fail(player, "awakened.kinetic_charger.release_full", new Object[0]);
      } else if (!FullReleaseState.set(player, data, jump ? 10 : level + 1)) {
         this.fail(player, "awakened.kinetic_charger.no_charge", new Object[0]);
      } else {
         PowerService.playAnimation(player, "full_release_step");
      }
   }

   @Override
   public String trayLabel(int resourceTier) {
      return resourceTier * 10 + "%";
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      FullReleaseState.clear(player);
      super.clearPlayer(player);
   }

   private static final class Press {
      private final boolean down;
      private int ticks;
      private boolean spent;

      private Press(boolean down) {
         this.down = down;
      }
   }
}
