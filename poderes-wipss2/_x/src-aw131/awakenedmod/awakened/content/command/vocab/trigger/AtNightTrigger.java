package com.awakenedmod.awakened.content.command.vocab.trigger;

import com.awakenedmod.awakened.content.command.CommandSupport;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class AtNightTrigger implements Trigger {
   @Override
   public String id() {
      return "at_night";
   }

   @Override
   public int unlockLevel() {
      return 6;
   }

   @Override
   public boolean live(ServerPlayer caster) {
      return CommandSupport.night(caster.level());
   }

   @Override
   public boolean liveOnClient(Player caster) {
      return CommandSupport.night(caster.level());
   }
}
