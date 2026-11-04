package com.awakenedmod.awakened.core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class ProgressData {
   public static final MapCodec<ProgressData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Codec.INT.optionalFieldOf("timesAwakened", 0).forGetter(d -> d.timesAwakened),
               Codec.INT.optionalFieldOf("highestMastery", 0).forGetter(d -> d.highestMastery),
               Codec.INT.optionalFieldOf("enemiesDefeated", 0).forGetter(d -> d.enemiesDefeated),
               Codec.INT.optionalFieldOf("bossesDefeated", 0).forGetter(d -> d.bossesDefeated)
            )
            .apply(instance, ProgressData::new)
   );
   private int timesAwakened;
   private int highestMastery;
   private int enemiesDefeated;
   private int bossesDefeated;

   public ProgressData() {
      this(0, 0, 0, 0);
   }

   private ProgressData(int timesAwakened, int highestMastery, int enemiesDefeated, int bossesDefeated) {
      this.timesAwakened = timesAwakened;
      this.highestMastery = highestMastery;
      this.enemiesDefeated = enemiesDefeated;
      this.bossesDefeated = bossesDefeated;
   }

   public int timesAwakened() {
      return this.timesAwakened;
   }

   public int highestMastery() {
      return this.highestMastery;
   }

   public int enemiesDefeated() {
      return this.enemiesDefeated;
   }

   public int bossesDefeated() {
      return this.bossesDefeated;
   }

   public void awakened() {
      this.timesAwakened++;
   }

   public void recordMastery(int level) {
      if (level > this.highestMastery) {
         this.highestMastery = level;
      }
   }

   public void enemyDefeated() {
      this.enemiesDefeated++;
   }

   public void bossDefeated() {
      this.bossesDefeated++;
   }
}
