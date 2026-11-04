package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record TideHookVisual(int target, boolean wet) {
   public static final TideHookVisual NONE = new TideHookVisual(-1, false);
   public static final StreamCodec<ByteBuf, TideHookVisual> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, TideHookVisual::target, ByteBufCodecs.BOOL, TideHookVisual::wet, TideHookVisual::new
   );

   public static TideHookVisual of(Entity entity) {
      return (TideHookVisual)entity.getData(AwakenedAttachments.TIDE_HOOK_VISUAL);
   }

   public boolean gripping() {
      return this.target >= 0;
   }

   static void grip(ServerPlayer player, Entity target, boolean wet) {
      player.setData(AwakenedAttachments.TIDE_HOOK_VISUAL, new TideHookVisual(target.getId(), wet));
   }

   static void clear(ServerPlayer player) {
      if (of(player).gripping()) {
         player.setData(AwakenedAttachments.TIDE_HOOK_VISUAL, NONE);
      }
   }
}
