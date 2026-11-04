package com.awakenedmod.awakened.core.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SpiderVisual(boolean gauntlets, boolean gliding, int tether, boolean hanging, boolean leftArm, boolean diving, double ax, double ay, double az) {
   public static final int TETHER_NONE = 0;
   public static final int TETHER_SWING = 1;
   public static final int TETHER_ZIP = 2;
   public static final SpiderVisual NONE = new SpiderVisual(false, false, 0, false, false, false, 0.0, 0.0, 0.0);
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderVisual> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BYTE,
      SpiderVisual::packedFlags,
      ByteBufCodecs.DOUBLE,
      SpiderVisual::ax,
      ByteBufCodecs.DOUBLE,
      SpiderVisual::ay,
      ByteBufCodecs.DOUBLE,
      SpiderVisual::az,
      SpiderVisual::fromWire
   );

   private byte packedFlags() {
      return (byte)(
         (this.gauntlets ? 1 : 0) | (this.gliding ? 2 : 0) | (this.hanging ? 4 : 0) | (this.leftArm ? 8 : 0) | (this.tether & 3) << 4 | (this.diving ? 64 : 0)
      );
   }

   private static SpiderVisual fromWire(byte flags, double ax, double ay, double az) {
      return new SpiderVisual((flags & 1) != 0, (flags & 2) != 0, flags >> 4 & 3, (flags & 4) != 0, (flags & 8) != 0, (flags & 64) != 0, ax, ay, az);
   }

   public SpiderVisual withGauntlets(boolean g) {
      return new SpiderVisual(g, this.gliding, this.tether, this.hanging, this.leftArm, this.diving, this.ax, this.ay, this.az);
   }

   public SpiderVisual withGliding(boolean g) {
      return new SpiderVisual(this.gauntlets, g, this.tether, this.hanging, this.leftArm, this.diving, this.ax, this.ay, this.az);
   }

   public SpiderVisual withTether(int kind, boolean hang, boolean left, double x, double y, double z) {
      return new SpiderVisual(this.gauntlets, this.gliding, kind, hang, left, this.diving, x, y, z);
   }

   public SpiderVisual withHanging(boolean hang) {
      return new SpiderVisual(this.gauntlets, this.gliding, this.tether, hang, this.leftArm, this.diving, this.ax, this.ay, this.az);
   }

   public SpiderVisual withDiving(boolean d) {
      return new SpiderVisual(this.gauntlets, this.gliding, this.tether, this.hanging, this.leftArm, d, this.ax, this.ay, this.az);
   }
}
