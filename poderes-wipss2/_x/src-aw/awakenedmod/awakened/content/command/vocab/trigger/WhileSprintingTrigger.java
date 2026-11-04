package com.awakenedmod.awakened.content.command.vocab.trigger;

import com.awakenedmod.awakened.content.command.vocab.Trigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class WhileSprintingTrigger implements Trigger {
   @Override
   public String id() {
      return "while_sprinting";
   }

   @Override
   public int unlockLevel() {
      return 1;
   }

   @Override
   public boolean live(ServerPlayer caster) {
      return sprinting(caster);
   }

   @Override
   public boolean liveOnClient(Player caster) {
      return sprinting(caster);
   }

   private static boolean sprinting(Player caster) {
      Entity mount = caster.getVehicle();
      return caster.isSprinting() || mount != null && mount.isSprinting();
   }
}
