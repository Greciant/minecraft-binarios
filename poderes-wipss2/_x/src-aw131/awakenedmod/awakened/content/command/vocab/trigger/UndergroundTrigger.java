package com.awakenedmod.awakened.content.command.vocab.trigger;

import com.awakenedmod.awakened.content.command.vocab.Trigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class UndergroundTrigger implements Trigger {
   private static final int MAX_FLUID_CLIMB = 64;

   @Override
   public String id() {
      return "underground";
   }

   @Override
   public int unlockLevel() {
      return 2;
   }

   @Override
   public boolean live(ServerPlayer caster) {
      return underground(caster);
   }

   @Override
   public boolean liveOnClient(Player caster) {
      return underground(caster);
   }

   private static boolean underground(Player caster) {
      Level level = caster.level();
      MutableBlockPos pos = BlockPos.containing(caster.getEyePosition()).mutable();

      for (int climbed = 0; climbed < 64 && !level.getFluidState(pos).isEmpty(); climbed++) {
         pos.move(0, 1, 0);
      }

      return !level.canSeeSky(pos);
   }
}
