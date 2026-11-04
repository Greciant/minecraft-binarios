package com.awakenedmod.awakened.content.command.vocab.trigger;

import com.awakenedmod.awakened.content.command.vocab.Posture;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class WhileFallingTrigger implements Trigger {
   private static final double FALLING_DY = -0.1;

   @Override
   public String id() {
      return "while_falling";
   }

   @Override
   public int unlockLevel() {
      return 3;
   }

   @Override
   public boolean live(ServerPlayer caster) {
      return falling(caster, caster.getKnownMovement().y);
   }

   @Override
   public boolean liveOnClient(Player caster) {
      return falling(caster, caster.getRootVehicle().getDeltaMovement().y);
   }

   private static boolean falling(Player caster, double dy) {
      return !caster.getRootVehicle().onGround() && dy < -0.1;
   }

   @Override
   public Set<Posture> postures() {
      return Set.of(Posture.AIRBORNE, Posture.FALLING);
   }
}
