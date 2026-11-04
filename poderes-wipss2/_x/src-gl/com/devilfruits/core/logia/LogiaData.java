package com.devilfruits.core.logia;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

public final class LogiaData {
   public static final Codec<LogiaData> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               Codec.STRING.optionalFieldOf("state", "solid").forGetter(d -> d.state.serializedName()),
               Codec.INT.optionalFieldOf("limbs", 0).forGetter(d -> d.limbs),
               Codec.INT.optionalFieldOf("partial_ticks", 0).forGetter(d -> d.partialTicks),
               Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("counters", Map.of()).forGetter(d -> d.counterTimers),
               Codec.BOOL.optionalFieldOf("intangible", true).forGetter(d -> d.intangible)
            )
            .apply(i, LogiaData::decode)
   );
   private LogiaState state = LogiaState.SOLID;
   private int limbs;
   private int partialTicks;
   private int reformTicks;
   private final Map<String, Integer> counterTimers = new HashMap<>();
   private boolean intangible = true;
   private int activeCounters;
   private boolean intangibilitySuppressed;
   private boolean abilitiesBlocked;
   private float abilityPower = 1.0F;
   @Nullable
   private ResourceLocation movementMode;

   private static LogiaData decode(String state, int limbs, int partialTicks, Map<String, Integer> counters, boolean intangible) {
      LogiaData d = new LogiaData();

      for (LogiaState s : LogiaState.values()) {
         if (s.serializedName().equals(state)) {
            d.state = s;
         }
      }

      d.limbs = limbs & 63;
      d.partialTicks = Math.max(0, partialTicks);
      d.counterTimers.putAll(counters);
      d.intangible = intangible;
      return d;
   }

   public LogiaState state() {
      return this.state;
   }

   public int limbs() {
      return this.limbs;
   }

   public int partialTicks() {
      return this.partialTicks;
   }

   public int reformTicks() {
      return this.reformTicks;
   }

   public Map<String, Integer> counterTimers() {
      return this.counterTimers;
   }

   public boolean intangible() {
      return this.intangible;
   }

   public int activeCounters() {
      return this.activeCounters;
   }

   public boolean intangibilitySuppressed() {
      return this.intangibilitySuppressed;
   }

   public boolean abilitiesBlocked() {
      return this.abilitiesBlocked;
   }

   public float abilityPower() {
      return this.abilityPower;
   }

   @Nullable
   public ResourceLocation movementMode() {
      return this.movementMode;
   }

   public boolean isDefault() {
      return this.state == LogiaState.SOLID && this.limbs == 0 && this.counterTimers.isEmpty() && this.intangible;
   }

   void setState(LogiaState state) {
      this.state = state;
   }

   void setLimbs(int limbs) {
      this.limbs = limbs & 63;
   }

   void setPartialTicks(int ticks) {
      this.partialTicks = ticks;
   }

   void setReformTicks(int ticks) {
      this.reformTicks = ticks;
   }

   void setActiveCounters(int mask) {
      this.activeCounters = mask;
   }

   void setIntangibilitySuppressed(boolean on) {
      this.intangibilitySuppressed = on;
   }

   void setAbilitiesBlocked(boolean on) {
      this.abilitiesBlocked = on;
   }

   void setAbilityPower(float power) {
      this.abilityPower = power;
   }

   void setMovementMode(@Nullable ResourceLocation mode) {
      this.movementMode = mode;
   }

   void setIntangible(boolean on) {
      this.intangible = on;
   }

   public void writeSync(ByteBuf buf) {
      ByteBufCodecs.VAR_INT.encode(buf, this.state.ordinal());
      ByteBufCodecs.VAR_INT.encode(buf, this.limbs);
      ByteBufCodecs.VAR_INT.encode(buf, this.activeCounters);
      ByteBufCodecs.VAR_INT.encode(buf, this.flags());
      ByteBufCodecs.FLOAT.encode(buf, this.abilityPower);
      ByteBufCodecs.VAR_INT.encode(buf, LogiaMovement.index(this.movementMode) + 1);
   }

   public void readSync(ByteBuf buf) {
      this.state = LogiaState.byId((Integer)ByteBufCodecs.VAR_INT.decode(buf));
      this.limbs = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      this.activeCounters = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      this.setFlags((Integer)ByteBufCodecs.VAR_INT.decode(buf));
      this.abilityPower = (Float)ByteBufCodecs.FLOAT.decode(buf);
      LogiaMovementMode mode = LogiaMovement.byIndex((Integer)ByteBufCodecs.VAR_INT.decode(buf) - 1);
      this.movementMode = mode == null ? null : mode.id();
   }

   private int flags() {
      return (this.intangibilitySuppressed ? 1 : 0) | (this.abilitiesBlocked ? 2 : 0) | (this.intangible ? 0 : 4);
   }

   private void setFlags(int flags) {
      this.intangibilitySuppressed = (flags & 1) != 0;
      this.abilitiesBlocked = (flags & 2) != 0;
      this.intangible = (flags & 4) == 0;
   }

   public void applyRemote(LogiaState state, int limbs, int activeCounters, int modeIndex) {
      this.state = state;
      this.limbs = limbs & 63;
      this.activeCounters = activeCounters;
      LogiaMovementMode mode = LogiaMovement.byIndex(modeIndex);
      this.movementMode = mode == null ? null : mode.id();
   }

   public void reset() {
      this.state = LogiaState.SOLID;
      this.limbs = 0;
      this.partialTicks = 0;
      this.reformTicks = 0;
      this.counterTimers.clear();
      this.intangible = true;
      this.activeCounters = 0;
      this.intangibilitySuppressed = false;
      this.abilitiesBlocked = false;
      this.abilityPower = 1.0F;
      this.movementMode = null;
   }

   public boolean sameState(LogiaData o) {
      return this.state == o.state
         && this.limbs == o.limbs
         && this.activeCounters == o.activeCounters
         && this.flags() == o.flags()
         && this.abilityPower == o.abilityPower;
   }
}
