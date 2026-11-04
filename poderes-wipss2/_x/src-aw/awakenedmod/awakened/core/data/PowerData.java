package com.awakenedmod.awakened.core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

public final class PowerData {
   public static final String NONE = "";
   public static final MapCodec<PowerData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Codec.STRING.optionalFieldOf("powerId", "").forGetter(d -> d.powerId),
               Codec.BOOL.optionalFieldOf("awakened", false).forGetter(d -> d.awakened),
               Codec.FLOAT.optionalFieldOf("stamina", 0.0F).forGetter(d -> d.stamina),
               Codec.INT.optionalFieldOf("activeSlot", 0).forGetter(d -> d.activeSlot),
               Codec.LONG.optionalFieldOf("lastSpendTime", 0L).forGetter(d -> d.lastSpendTime),
               Codec.LONG.optionalFieldOf("lastCatalystUse", 0L).forGetter(d -> d.lastCatalystUse),
               Codec.BOOL.optionalFieldOf("infiniteStamina", false).forGetter(d -> d.infiniteStamina),
               Codec.unboundedMap(Codec.STRING, MasteryData.CODEC).optionalFieldOf("mastery", Map.of()).forGetter(d -> d.mastery),
               Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("vars", Map.of()).forGetter(d -> d.vars),
               Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("strVars", Map.of()).forGetter(d -> d.strVars),
               Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.FLOAT)).optionalFieldOf("banked", Map.of()).forGetter(d -> d.banked)
            )
            .apply(instance, PowerData::new)
   );
   private String powerId;
   private boolean awakened;
   private float stamina;
   private int activeSlot;
   private long lastSpendTime;
   private long lastCatalystUse;
   private boolean infiniteStamina;
   private final Map<String, MasteryData> mastery;
   private final Map<String, Float> vars;
   private final Map<String, String> strVars;
   private final Map<String, Map<String, Float>> banked;
   private float syncedStamina = Float.NaN;
   private float syncedLocked = Float.NaN;

   public PowerData() {
      this("", false, 0.0F, 0, 0L, 0L, false, Map.of(), Map.of(), Map.of(), Map.of());
   }

   private PowerData(
      String powerId,
      boolean awakened,
      float stamina,
      int activeSlot,
      long lastSpendTime,
      long lastCatalystUse,
      boolean infiniteStamina,
      Map<String, MasteryData> mastery,
      Map<String, Float> vars,
      Map<String, String> strVars,
      Map<String, Map<String, Float>> banked
   ) {
      this.powerId = powerId;
      this.awakened = awakened;
      this.stamina = stamina;
      this.activeSlot = activeSlot;
      this.lastSpendTime = lastSpendTime;
      this.lastCatalystUse = lastCatalystUse;
      this.infiniteStamina = infiniteStamina;
      this.mastery = new HashMap<>(mastery);
      this.vars = new HashMap<>(vars);
      this.strVars = new HashMap<>(strVars);
      this.banked = new HashMap<>();
      banked.forEach((power, values) -> this.banked.put(power, new HashMap<>((Map<? extends String, ? extends Float>)values)));
   }

   public String powerId() {
      return this.powerId;
   }

   public boolean hasPower() {
      return !this.powerId.equals("");
   }

   public boolean awakened() {
      return this.awakened;
   }

   public float stamina() {
      return this.stamina;
   }

   public int activeSlot() {
      return this.activeSlot;
   }

   public long lastSpendTime() {
      return this.lastSpendTime;
   }

   public boolean infiniteStamina() {
      return this.infiniteStamina;
   }

   public void setStamina(float stamina) {
      this.stamina = stamina;
   }

   public boolean staminaSyncStale() {
      return !(Math.abs(this.stamina - this.syncedStamina) < 0.001F);
   }

   public void markStaminaSynced() {
      this.syncedStamina = this.stamina;
   }

   public boolean lockedSyncStale(float locked) {
      return !(Math.abs(locked - this.syncedLocked) < 0.001F);
   }

   public void markLockedSynced(float locked) {
      this.syncedLocked = locked;
   }

   public void setActiveSlot(int activeSlot) {
      this.activeSlot = activeSlot;
   }

   public void setLastSpendTime(long time) {
      this.lastSpendTime = time;
   }

   public long lastCatalystUse() {
      return this.lastCatalystUse;
   }

   public void setLastCatalystUse(long time) {
      this.lastCatalystUse = time;
   }

   public void setInfiniteStamina(boolean infiniteStamina) {
      this.infiniteStamina = infiniteStamina;
   }

   public void assignPower(String powerId, boolean keepMastery) {
      this.powerId = powerId;
      this.awakened = true;
      this.activeSlot = 0;
      this.vars.clear();
      this.strVars.clear();
      MasteryData entry = this.mastery.computeIfAbsent(powerId, k -> new MasteryData());
      if (!keepMastery) {
         entry.reset();
         this.banked.remove(powerId);
      }
   }

   public boolean migratePowerId(String from, String to) {
      return this.migratePowerId(from, to, true);
   }

   public boolean migratePowerId(String from, String to, boolean carryMastery) {
      if (!this.powerId.equals(from)) {
         return false;
      } else {
         this.powerId = to;
         if (carryMastery) {
            this.moveRecords(from, to);
         }

         return true;
      }
   }

   public void moveRecords(String from, String to) {
      Map<String, Float> keep = this.banked.remove(from);
      if (keep != null) {
         this.banked.computeIfAbsent(to, k -> new HashMap<>()).putAll(keep);
      }

      MasteryData earned = this.mastery.remove(from);
      if (earned != null) {
         this.mastery.merge(to, earned, PowerData::furtherAlong);
      }
   }

   private static MasteryData furtherAlong(MasteryData a, MasteryData b) {
      if (a.level() != b.level()) {
         return a.level() > b.level() ? a : b;
      } else {
         return a.progress() >= b.progress() ? a : b;
      }
   }

   public void clearPower() {
      this.powerId = "";
      this.awakened = false;
      this.activeSlot = 0;
      this.stamina = 0.0F;
      this.vars.clear();
      this.strVars.clear();
   }

   public float var(String key, float fallback) {
      return this.vars.getOrDefault(key, fallback);
   }

   public void setVar(String key, float value) {
      this.vars.put(key, value);
   }

   public boolean hasVar(String key) {
      return this.vars.containsKey(key);
   }

   public void removeVar(String key) {
      this.vars.remove(key);
   }

   public void removeVars(String prefix) {
      this.vars.keySet().removeIf(key -> key.startsWith(prefix));
   }

   public float bankedVar(String key, float fallback) {
      Map<String, Float> values = this.banked.get(this.powerId);
      return values == null ? fallback : values.getOrDefault(key, fallback);
   }

   public void setBankedVar(String key, float value) {
      this.banked.computeIfAbsent(this.powerId, k -> new HashMap<>()).put(key, value);
   }

   public String strVar(String key, String fallback) {
      return this.strVars.getOrDefault(key, fallback);
   }

   public void setStrVar(String key, String value) {
      this.strVars.put(key, value);
   }

   public MasteryData currentMastery() {
      return this.mastery.computeIfAbsent(this.powerId, k -> new MasteryData());
   }

   public int masteryLevel() {
      return this.hasPower() ? this.currentMastery().level() : 0;
   }

   public int masteryLevelOf(String id) {
      MasteryData entry = this.mastery.get(id);
      return entry != null ? entry.level() : 0;
   }
}
