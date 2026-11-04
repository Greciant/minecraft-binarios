package com.awakenedmod.awakened.core.data;

import com.awakenedmod.awakened.core.skill.Stat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

public final class SkillData {
   public static final MapCodec<SkillData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Codec.INT.optionalFieldOf("level", 1).forGetter(d -> d.level),
               Codec.DOUBLE.optionalFieldOf("xp", 0.0).forGetter(d -> d.xp),
               Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("allocations", Map.of()).forGetter(d -> d.allocations)
            )
            .apply(instance, SkillData::new)
   );
   private int level;
   private double xp;
   private final Map<String, Integer> allocations;

   public SkillData() {
      this(1, 0.0, Map.of());
   }

   private SkillData(int level, double xp, Map<String, Integer> allocations) {
      this.level = Math.max(1, level);
      this.xp = Math.max(0.0, xp);
      this.allocations = new HashMap<>(allocations);
   }

   public int level() {
      return this.level;
   }

   public double xp() {
      return this.xp;
   }

   public void addXp(double amount) {
      this.xp += amount;
   }

   public void levelUp(double carryXp) {
      this.level++;
      this.xp = Math.max(0.0, carryXp);
   }

   public int points(Stat stat) {
      return this.allocations.getOrDefault(stat.id(), 0);
   }

   public float bonus(Stat stat) {
      return (float)this.points(stat) * stat.perPoint();
   }

   public void setPoints(Stat stat, int value) {
      if (value <= 0) {
         this.allocations.remove(stat.id());
      } else {
         this.allocations.put(stat.id(), value);
      }
   }

   public int totalSpent() {
      int sum = 0;

      for (int v : this.allocations.values()) {
         sum += Math.max(0, v);
      }

      return sum;
   }

   public void clearAllocations() {
      this.allocations.clear();
   }

   public void set(int level, double xp) {
      this.level = Math.max(1, level);
      this.xp = Math.max(0.0, xp);
   }

   public void reset() {
      this.level = 1;
      this.xp = 0.0;
      this.allocations.clear();
   }
}
