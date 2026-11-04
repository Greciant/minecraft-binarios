package com.awakenedmod.awakened.content.command.vocab.trigger;

import com.awakenedmod.awakened.content.command.vocab.Posture;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class WhileAirborneTrigger implements Trigger {
   @Override
   public String id() {
      return "while_airborne";
   }

   @Override
   public int unlockLevel() {
      return 1;
   }

   @Override
   public boolean live(ServerPlayer caster) {
      return airborne(caster);
   }

   @Override
   public boolean liveOnClient(Player caster) {
      return airborne(caster);
   }

   private static boolean airborne(Player caster) {
      return !caster.getRootVehicle().onGround();
   }

   @Override
   public Set<Posture> postures() {
      return Set.of(Posture.AIRBORNE);
   }
}
