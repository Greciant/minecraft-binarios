package com.awakenedmod.awakened.content.graviton;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;

public record HeavyHandVisual(boolean holding, int target, int grip, boolean up, int snaps, int snapKind) {
   public static final int SNAP_YANK = 0;
   public static final int SNAP_SHOVE = 1;
   public static final int SNAP_CRUSH = 2;
   public static final int SNAP_LAUNCH = 3;
   public static final int SNAP_LOST = 4;
   public static final HeavyHandVisual NONE = new HeavyHandVisual(false, -1, 0, false, 0, 4);
   public static final StreamCodec<ByteBuf, HeavyHandVisual> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      HeavyHandVisual::holding,
      ByteBufCodecs.VAR_INT,
      HeavyHandVisual::target,
      ByteBufCodecs.VAR_INT,
      HeavyHandVisual::grip,
      ByteBufCodecs.BOOL,
      HeavyHandVisual::up,
      ByteBufCodecs.VAR_INT,
      HeavyHandVisual::snaps,
      ByteBufCodecs.VAR_INT,
      HeavyHandVisual::snapKind,
      HeavyHandVisual::new
   );
   public static final AttachmentType<HeavyHandVisual> TYPE = AttachmentType.builder(() -> NONE).sync(STREAM_CODEC).build();
   private static final int SNAP_WRAP = 16384;

   public float gripFraction() {
      return (float)this.grip / 255.0F;
   }

   public HeavyHandVisual holding(int targetId, float f, boolean lift) {
      int g = Mth.clamp(Math.round(Mth.clamp(f, 0.0F, 1.0F) * 255.0F), 0, 255);
      return new HeavyHandVisual(true, targetId, g, lift, this.snaps, this.snapKind);
   }

   public HeavyHandVisual snapped(int kind) {
      return new HeavyHandVisual(false, this.target, this.grip, this.up, (this.snaps + 1) % 16384, kind);
   }

   public static HeavyHandVisual of(Player player) {
      return (HeavyHandVisual)player.getData(TYPE);
   }

   public static void set(Player player, HeavyHandVisual next) {
      if (!next.equals(player.getData(TYPE))) {
         player.setData(TYPE, next);
      }
   }
}
