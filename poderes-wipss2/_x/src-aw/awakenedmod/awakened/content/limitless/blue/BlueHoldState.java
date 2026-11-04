package com.awakenedmod.awakened.content.limitless.blue;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;

public record BlueHoldState(boolean live, double x, double y, double z, int started) {
   public static final BlueHoldState NONE = new BlueHoldState(false, 0.0, 0.0, 0.0, 0);
   public static final StreamCodec<ByteBuf, BlueHoldState> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      BlueHoldState::live,
      ByteBufCodecs.DOUBLE,
      BlueHoldState::x,
      ByteBufCodecs.DOUBLE,
      BlueHoldState::y,
      ByteBufCodecs.DOUBLE,
      BlueHoldState::z,
      ByteBufCodecs.VAR_INT,
      BlueHoldState::started,
      BlueHoldState::new
   );
   public static final AttachmentType<BlueHoldState> TYPE = AttachmentType.builder(() -> NONE).sync(STREAM_CODEC).build();

   public Vec3 target() {
      return new Vec3(this.x, this.y, this.z);
   }

   public BlueHoldState pulling(Vec3 target) {
      return new BlueHoldState(true, target.x, target.y, target.z, this.started + 1 & 65535);
   }

   public BlueHoldState ended() {
      return new BlueHoldState(false, this.x, this.y, this.z, this.started);
   }

   public static BlueHoldState of(Player player) {
      return (BlueHoldState)player.getData(TYPE);
   }

   public static void set(Player player, BlueHoldState next) {
      if (!next.equals(player.getData(TYPE))) {
         player.setData(TYPE, next);
      }
   }
}
