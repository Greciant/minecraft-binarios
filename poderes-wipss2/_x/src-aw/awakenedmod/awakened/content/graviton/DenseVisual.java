package com.awakenedmod.awakened.content.graviton;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;

public record DenseVisual(boolean active, int charge, int hits, int stored, boolean low) {
   public static final DenseVisual NONE = new DenseVisual(false, 0, 0, 0, false);
   public static final StreamCodec<ByteBuf, DenseVisual> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      DenseVisual::active,
      ByteBufCodecs.VAR_INT,
      DenseVisual::charge,
      ByteBufCodecs.VAR_INT,
      DenseVisual::hits,
      ByteBufCodecs.VAR_INT,
      DenseVisual::stored,
      ByteBufCodecs.BOOL,
      DenseVisual::low,
      DenseVisual::new
   );
   public static final AttachmentType<DenseVisual> TYPE = AttachmentType.builder(() -> NONE).sync(STREAM_CODEC).build();
   private static final int HIT_WRAP = 16384;

   public float chargeFraction() {
      return (float)this.charge / 255.0F;
   }

   public DenseVisual withStore(float fraction, float store, boolean lowStamina) {
      int c = Mth.clamp(Math.round(Mth.clamp(fraction, 0.0F, 1.0F) * 255.0F), 0, 255);
      return new DenseVisual(this.active, c, this.hits, (int)Math.max(0.0, Math.ceil((double)(store - 0.001F))), lowStamina);
   }

   public DenseVisual hit() {
      return new DenseVisual(this.active, this.charge, (this.hits + 1) % 16384, this.stored, this.low);
   }

   public static DenseVisual of(Player player) {
      return (DenseVisual)player.getData(TYPE);
   }

   public static void set(Player player, DenseVisual next) {
      if (!next.equals(player.getData(TYPE))) {
         player.setData(TYPE, next);
      }
   }
}
