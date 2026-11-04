package com.awakenedmod.awakened.content.limitless;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;

public record LimitlessCast(int start, int end, int state) {
   public static final int IDLE = 0;
   public static final int LIVE = 1;
   public static final int FIRED = 2;
   public static final int CANCELLED = 3;
   public static final int OPEN = Integer.MAX_VALUE;
   public static final LimitlessCast NONE = new LimitlessCast(0, 0, 0);
   public static final StreamCodec<ByteBuf, LimitlessCast> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, LimitlessCast::start, ByteBufCodecs.INT, LimitlessCast::end, ByteBufCodecs.VAR_INT, LimitlessCast::state, LimitlessCast::new
   );
   public static final AttachmentType<LimitlessCast> RED = AttachmentType.builder(() -> NONE).sync(STREAM_CODEC).build();
   public static final AttachmentType<LimitlessCast> RCT = AttachmentType.builder(() -> NONE).sync(STREAM_CODEC).build();

   public static LimitlessCast started(int now) {
      return new LimitlessCast(now, Integer.MAX_VALUE, 1);
   }

   public LimitlessCast ended(int now, boolean fired) {
      return this.state == 1 ? new LimitlessCast(this.start, now, fired ? 2 : 3) : this;
   }

   public boolean live() {
      return this.state == 1;
   }

   public static long seed(int casterId, int start) {
      return (long)casterId * 31L + (long)start;
   }

   public static LimitlessCast of(Player player, AttachmentType<LimitlessCast> type) {
      return (LimitlessCast)player.getData(type);
   }

   public static void end(Player player, AttachmentType<LimitlessCast> type, int now, boolean fired) {
      set(player, type, of(player, type).ended(now, fired));
   }

   public static void set(Player player, AttachmentType<LimitlessCast> type, LimitlessCast next) {
      if (!next.equals(player.getData(type))) {
         player.setData(type, next);
      }
   }
}
