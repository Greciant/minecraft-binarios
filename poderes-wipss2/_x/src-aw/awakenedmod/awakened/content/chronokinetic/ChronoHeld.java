package com.awakenedmod.awakened.content.chronokinetic;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;

public record ChronoHeld(int holds) {
   public static final ChronoHeld NONE = new ChronoHeld(0);
   public static final StreamCodec<ByteBuf, ChronoHeld> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(ChronoHeld::new, ChronoHeld::holds);

   static void hold(Entity entity) {
      entity.setData(AwakenedAttachments.CHRONO_HELD.get(), new ChronoHeld(count(entity) + 1));
   }

   static void let(Entity entity) {
      if (!entity.isRemoved()) {
         int left = count(entity) - 1;
         if (left > 0) {
            entity.setData(AwakenedAttachments.CHRONO_HELD.get(), new ChronoHeld(left));
         } else if (entity.getExistingDataOrNull(AwakenedAttachments.CHRONO_HELD.get()) != null) {
            entity.removeData(AwakenedAttachments.CHRONO_HELD.get());
         }
      }
   }

   public static boolean isHeld(Entity entity) {
      return count(entity) > 0;
   }

   private static int count(Entity entity) {
      ChronoHeld held = (ChronoHeld)entity.getExistingDataOrNull(AwakenedAttachments.CHRONO_HELD.get());
      return held == null ? 0 : held.holds;
   }
}
