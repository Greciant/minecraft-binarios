package com.awakenedmod.awakened.content.command.vocab.trigger;

import com.awakenedmod.awakened.content.command.vocab.Trigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class BelowHalfHealthTrigger implements Trigger {
   @Override
   public String id() {
      return "below_half_health";
   }

   @Override
   public int unlockLevel() {
      return 7;
   }

   @Override
   public boolean live(ServerPlayer caster) {
      return caster.getHealth() < caster.getMaxHealth() * 0.5F;
   }

   @Override
   public boolean liveOnClient(Player caster) {
      return caster.getHealth() < caster.getMaxHealth() * 0.5F;
   }
}
