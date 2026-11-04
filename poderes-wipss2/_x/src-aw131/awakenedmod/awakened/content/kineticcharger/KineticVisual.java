package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record KineticVisual(boolean stomping) {
   public static final KineticVisual NONE = new KineticVisual(false);
   public static final int STYLE_CHARGED = 1;
   public static final StreamCodec<ByteBuf, KineticVisual> STREAM_CODEC = ByteBufCodecs.BOOL.map(KineticVisual::new, KineticVisual::stomping);

   public static KineticVisual of(Entity entity) {
      return (KineticVisual)entity.getData(AwakenedAttachments.KINETIC_VISUAL);
   }

   static void setStomping(ServerPlayer player, boolean stomping) {
      if (of(player).stomping() != stomping) {
         player.setData(AwakenedAttachments.KINETIC_VISUAL, new KineticVisual(stomping));
      }
   }
}
