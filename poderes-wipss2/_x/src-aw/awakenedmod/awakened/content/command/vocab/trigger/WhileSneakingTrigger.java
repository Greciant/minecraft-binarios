package com.awakenedmod.awakened.content.command.vocab.trigger;

import com.awakenedmod.awakened.content.command.vocab.Posture;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class WhileSneakingTrigger implements Trigger {
   @Override
   public String id() {
      return "while_sneaking";
   }

   @Override
   public int unlockLevel() {
      return 0;
   }

   @Override
   public boolean live(ServerPlayer caster) {
      return caster.isShiftKeyDown();
   }

   @Override
   public boolean liveOnClient(Player caster) {
      return caster.isShiftKeyDown();
   }

   @Override
   public Set<Posture> postures() {
      return Set.of(Posture.SNEAKING);
   }
}
