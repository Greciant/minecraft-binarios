package com.awakenedmod.awakened.content.command.vocab.trigger;

import com.awakenedmod.awakened.content.command.vocab.Trigger;
import net.minecraft.server.level.ServerPlayer;

public final class AlwaysTrigger implements Trigger {
   @Override
   public String id() {
      return "always";
   }

   @Override
   public int unlockLevel() {
      return 0;
   }

   @Override
   public boolean live(ServerPlayer caster) {
      return true;
   }
}
