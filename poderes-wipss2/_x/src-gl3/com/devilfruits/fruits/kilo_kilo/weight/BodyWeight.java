package com.devilfruits.fruits.kilo_kilo.weight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class BodyWeight {
   public static final int NORMAL = 0;
   public static final int LIGHT = 1;
   public static final int HEAVY = 2;
   public static final Codec<BodyWeight> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               Codec.DOUBLE.optionalFieldOf("kg", 70.0).forGetter(d -> d.kg),
               Codec.DOUBLE.optionalFieldOf("target", 70.0).forGetter(d -> d.target),
               Codec.INT.optionalFieldOf("mode", 0).forGetter(d -> d.mode),
               Codec.DOUBLE.optionalFieldOf("rate", 0.0).forGetter(d -> d.rate)
            )
            .apply(i, BodyWeight::new)
   );
   public static final StreamCodec<ByteBuf, BodyWeight> STREAM_CODEC = StreamCodec.of((buf, d) -> {
      buf.writeDouble(d.kg);
      buf.writeDouble(d.target);
      buf.writeByte(d.mode);
      buf.writeDouble(d.rate);
      buf.writeBoolean(d.plunging);
   }, buf -> {
      BodyWeight d = new BodyWeight(buf.readDouble(), buf.readDouble(), buf.readByte(), buf.readDouble());
      d.plunging = buf.readBoolean();
      return d;
   });
   private double kg;
   private double target;
   private int mode;
   private double rate;
   private boolean plunging;

   public BodyWeight() {
      this(70.0, 70.0, 0, 0.0);
   }

   private BodyWeight(double kg, double target, int mode, double rate) {
      this.kg = WeightCurves.clampKg(kg);
      this.target = WeightCurves.clampKg(target);
      this.mode = mode;
      this.rate = rate;
   }

   public double kg() {
      return this.kg;
   }

   public double target() {
      return this.target;
   }

   public int mode() {
      return this.mode;
   }

   public double rate() {
      return this.rate;
   }

   public boolean plunging() {
      return this.plunging;
   }

   public double scale() {
      return WeightCurves.scale(this.kg);
   }

   public boolean normal() {
      return this.mode == 0 && Math.abs(this.kg - 70.0) < 1.0E-6 && Math.abs(this.target - 70.0) < 1.0E-6;
   }

   public boolean ramping() {
      return Math.abs(Math.log(this.kg) - Math.log(this.target)) > 1.0E-9;
   }

   public void setKg(double kg) {
      this.kg = WeightCurves.clampKg(kg);
   }

   public void aim(int mode, double target, double rate) {
      this.mode = mode;
      this.target = WeightCurves.clampKg(target);
      this.rate = rate;
   }

   public void setRate(double rate) {
      this.rate = rate;
   }

   public void setPlunging(boolean plunging) {
      this.plunging = plunging;
   }

   public void reset() {
      this.kg = 70.0;
      this.target = 70.0;
      this.mode = 0;
      this.rate = 0.0;
      this.plunging = false;
   }

   public boolean advance(double defaultRate) {
      if (!this.ramping()) {
         return false;
      } else {
         this.kg = WeightCurves.step(this.kg, this.target, this.rate > 0.0 ? this.rate : defaultRate);
         return !this.ramping();
      }
   }

   public void copyFrom(BodyWeight o) {
      this.kg = o.kg;
      this.target = o.target;
      this.mode = o.mode;
      this.rate = o.rate;
      this.plunging = o.plunging;
   }
}
