package com.awakenedmod.awakened.content.limitless.purple;

import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;

public record PurpleCast(LimitlessCast cast, int pull, int tier, boolean over, int beat, int pullLen) {
   public static final PurpleCast NONE = new PurpleCast(LimitlessCast.NONE, Integer.MAX_VALUE, 0, false, 10, 6);
   public static final StreamCodec<ByteBuf, PurpleCast> STREAM_CODEC = StreamCodec.composite(
      LimitlessCast.STREAM_CODEC,
      PurpleCast::cast,
      ByteBufCodecs.INT,
      PurpleCast::pull,
      ByteBufCodecs.VAR_INT,
      PurpleCast::tier,
      ByteBufCodecs.BOOL,
      PurpleCast::over,
      ByteBufCodecs.VAR_INT,
      PurpleCast::beat,
      ByteBufCodecs.VAR_INT,
      PurpleCast::pullLen,
      PurpleCast::new
   );
   public static final AttachmentType<PurpleCast> TYPE = AttachmentType.builder(() -> NONE).sync(STREAM_CODEC).build();

   public static PurpleCast started(int now, boolean over, int beat, int pullLen) {
      return new PurpleCast(LimitlessCast.started(now), Integer.MAX_VALUE, 0, over, Math.max(1, beat), Math.max(1, pullLen));
   }

   public PurpleCast pulling(int now, PurpleStrength strength) {
      return this.live() && !this.pulling() ? new PurpleCast(this.cast, now, strength.tier(), this.over, this.beat, this.pullLen) : this;
   }

   public PurpleCast ended(int now, boolean fired) {
      return new PurpleCast(this.cast.ended(now, fired), this.pull, this.tier, this.over, this.beat, this.pullLen);
   }

   public int start() {
      return this.cast.start();
   }

   public boolean live() {
      return this.cast.live();
   }

   public boolean pulling() {
      return this.pull != Integer.MAX_VALUE;
   }

   public PurpleStrength strength() {
      return PurpleStrength.of(this.tier);
   }

   public float beats(double t) {
      if (!this.over) {
         return 0.0F;
      } else {
         double held = Math.min(t, this.pulling() ? (double)this.pull : t) - (double)this.start() - 50.0;
         return (float)Math.max(0.0, Math.min(4.0, held / (double)this.beat));
      }
   }

   public boolean full(double t) {
      return this.beats(t) >= 4.0F;
   }

   public float pullIn(double t) {
      return this.pulling() ? (float)Math.max(0.0, Math.min(1.0, (t - (double)this.pull) / (double)this.pullLen)) : 0.0F;
   }

   public int launchAt() {
      return this.pull + this.pullLen;
   }

   public static PurpleCast of(Player player) {
      return (PurpleCast)player.getData(TYPE);
   }

   public static void set(Player player, PurpleCast next) {
      if (!next.equals(player.getData(TYPE))) {
         player.setData(TYPE, next);
      }
   }
}
