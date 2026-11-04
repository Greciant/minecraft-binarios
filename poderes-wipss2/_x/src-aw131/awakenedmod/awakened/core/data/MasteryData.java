package com.awakenedmod.awakened.core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class MasteryData {
   public static final Codec<MasteryData> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               Codec.INT.optionalFieldOf("level", 0).forGetter(MasteryData::level),
               Codec.DOUBLE.optionalFieldOf("progress", 0.0).forGetter(MasteryData::progress),
               Codec.INT.optionalFieldOf("peak", 0).forGetter(MasteryData::peak)
            )
            .apply(instance, MasteryData::new)
   );
   private int level;
   private double progress;
   private int peak;

   public MasteryData() {
      this(0, 0.0);
   }

   public MasteryData(int level, double progress) {
      this(level, progress, level);
   }

   private MasteryData(int level, double progress, int peak) {
      this.level = level;
      this.progress = progress;
      this.peak = Math.max(peak, level);
   }

   public int peak() {
      return Math.max(this.peak, this.level);
   }

   public int level() {
      return this.level;
   }

   public double progress() {
      return this.progress;
   }

   public void addProgress(double amount) {
      this.progress += amount;
   }

   public void levelUp(double carryProgress) {
      this.level++;
      this.progress = Math.max(0.0, carryProgress);
      this.peak = Math.max(this.peak, this.level);
   }

   public void reset() {
      this.level = 0;
      this.progress = 0.0;
      this.peak = 0;
   }

   public void set(int level, double progress) {
      this.level = Math.max(0, level);
      this.progress = Math.max(0.0, progress);
      this.peak = Math.max(this.peak, this.level);
   }
}
