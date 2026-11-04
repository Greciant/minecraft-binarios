package com.awakenedmod.awakened.core.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

public record VelocityVisual(
   boolean overdrive,
   int gear,
   int chosen,
   int earnedGear,
   int unlocked,
   boolean sprinting,
   boolean phasing,
   boolean timeless,
   boolean tunnel,
   boolean diving,
   boolean transit,
   boolean forcing,
   boolean billing,
   boolean coyote,
   int charge,
   float speedMult,
   int colorId,
   int fieldRadiusHalves,
   boolean fieldClosing,
   boolean trail
) {
   public static final float NO_SPEED_MULT = 1.0F;
   public static final VelocityVisual NONE = new VelocityVisual(
      false, 0, 0, 0, 0, false, false, false, false, false, false, false, false, false, 0, 1.0F, DyeColor.RED.getId(), 0, false, true
   );
   public static final int CHARGE_FULL = 100;
   public static final int MAX_FIELD_RADIUS_HALVES = 127;
   private static final int F_OVERDRIVE = 1;
   private static final int F_SPRINTING = 2;
   private static final int F_PHASING = 4;
   private static final int F_TIMELESS = 8;
   private static final int F_TUNNEL = 16;
   private static final int F_FORCING = 32;
   private static final int F_COYOTE = 64;
   private static final int F_BILLING = 128;
   private static final int COLOR_SHIFT = 8;
   private static final int COLOR_MASK = 15;
   private static final int FIELD_RADIUS_SHIFT = 12;
   private static final int FIELD_RADIUS_MASK = 127;
   private static final int F_FIELD_CLOSING = 524288;
   private static final int F_DIVING = 1048576;
   private static final int F_TRANSIT = 2097152;
   private static final int UNLOCKED_SHIFT = 22;
   private static final int UNLOCKED_MASK = 7;
   private static final int F_TRAIL = 33554432;
   public static final StreamCodec<RegistryFriendlyByteBuf, VelocityVisual> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      VelocityVisual::flags,
      ByteBufCodecs.VAR_INT,
      VelocityVisual::gear,
      ByteBufCodecs.VAR_INT,
      VelocityVisual::chosen,
      ByteBufCodecs.VAR_INT,
      VelocityVisual::earnedGear,
      ByteBufCodecs.VAR_INT,
      VelocityVisual::charge,
      ByteBufCodecs.FLOAT,
      VelocityVisual::speedMult,
      VelocityVisual::unpack
   );

   private int flags() {
      return (this.overdrive ? 1 : 0)
         | (this.sprinting ? 2 : 0)
         | (this.phasing ? 4 : 0)
         | (this.timeless ? 8 : 0)
         | (this.tunnel ? 16 : 0)
         | (this.forcing ? 32 : 0)
         | (this.coyote ? 64 : 0)
         | (this.billing ? 128 : 0)
         | (this.colorId & 15) << 8
         | (this.fieldRadiusHalves & 127) << 12
         | (this.fieldClosing ? 524288 : 0)
         | (this.diving ? 1048576 : 0)
         | (this.transit ? 2097152 : 0)
         | (this.unlocked & 7) << 22
         | (this.trail ? 33554432 : 0);
   }

   private static VelocityVisual unpack(int flags, int gear, int chosen, int earnedGear, int charge, float speedMult) {
      return new VelocityVisual(
         (flags & 1) != 0,
         gear,
         chosen,
         earnedGear,
         flags >> 22 & 7,
         (flags & 2) != 0,
         (flags & 4) != 0,
         (flags & 8) != 0,
         (flags & 16) != 0,
         (flags & 1048576) != 0,
         (flags & 2097152) != 0,
         (flags & 32) != 0,
         (flags & 128) != 0,
         (flags & 64) != 0,
         charge,
         speedMult,
         flags >> 8 & 15,
         flags >> 12 & 127,
         (flags & 524288) != 0,
         (flags & 33554432) != 0
      );
   }

   public double fieldRadius() {
      return (double)this.fieldRadiusHalves / 2.0;
   }

   public DyeColor color() {
      return DyeColor.byId(this.colorId);
   }

   public boolean running() {
      return this.overdrive && this.sprinting && !this.tunnel;
   }

   public boolean tunnelling() {
      return this.tunnel || this.diving;
   }

   public boolean charging() {
      return this.charge > 0;
   }

   public float chargeFraction() {
      return Math.max(0.0F, Math.min(1.0F, (float)this.charge / 100.0F));
   }
}
