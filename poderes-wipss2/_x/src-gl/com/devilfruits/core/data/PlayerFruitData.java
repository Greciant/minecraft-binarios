package com.devilfruits.core.data;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.logia.LogiaData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class PlayerFruitData {
   public static final int MAX_MASTERY = 10;
   public static final ResourceLocation FORM_COOLDOWN = DevilFruits.id("form_switch");
   public static final Codec<PlayerFruitData> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               ResourceLocation.CODEC.optionalFieldOf("fruit").forGetter(d -> Optional.ofNullable(d.fruit)),
               Codec.STRING.optionalFieldOf("form", "").forGetter(d -> d.form),
               Codec.INT.optionalFieldOf("mastery", 1).forGetter(d -> d.mastery),
               Codec.FLOAT.optionalFieldOf("mastery_xp", 0.0F).forGetter(d -> d.masteryXp),
               ResourceLocation.CODEC.optionalFieldOf("mastery_fruit").forGetter(d -> Optional.ofNullable(d.masteryFruit)),
               Codec.FLOAT.optionalFieldOf("energy", 0.0F).forGetter(d -> d.energy),
               Codec.unboundedMap(ResourceLocation.CODEC, PlayerFruitData.Cooldown.CODEC).optionalFieldOf("cooldowns", Map.of()).forGetter(d -> d.cooldowns),
               Codec.INT.optionalFieldOf("selected", 0).forGetter(d -> d.selected),
               ResourceLocation.CODEC.listOf().optionalFieldOf("toggles", List.of()).forGetter(d -> List.copyOf(d.toggles)),
               Codec.LONG.optionalFieldOf("last_combat", 0L).forGetter(d -> d.lastCombatTick),
               Codec.BOOL.optionalFieldOf("suppressed", false).forGetter(d -> d.suppressed),
               Codec.BOOL.optionalFieldOf("has_fruit").forGetter(d -> Optional.empty()),
               LogiaData.CODEC.optionalFieldOf("logia").forGetter(d -> d.logia.isDefault() ? Optional.empty() : Optional.of(d.logia))
            )
            .apply(i, PlayerFruitData::decode)
   );
   private static final StreamCodec<ByteBuf, PlayerFruitData> SAVED_STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
   public static final StreamCodec<ByteBuf, PlayerFruitData> STREAM_CODEC = StreamCodec.of((buf, data) -> {
      SAVED_STREAM_CODEC.encode(buf, data);
      ByteBufCodecs.VAR_INT.encode(buf, data.flags);
      data.logia.writeSync(buf);
   }, buf -> {
      PlayerFruitData data = (PlayerFruitData)SAVED_STREAM_CODEC.decode(buf);
      data.flags = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      data.logia.readSync(buf);
      return data;
   });
   @Nullable
   private ResourceLocation fruit;
   private String form = "";
   private int mastery = 1;
   private float masteryXp;
   @Nullable
   private ResourceLocation masteryFruit;
   private float energy;
   private final Map<ResourceLocation, PlayerFruitData.Cooldown> cooldowns = new HashMap<>();
   private int selected;
   private final Set<ResourceLocation> toggles = new LinkedHashSet<>();
   private long lastCombatTick;
   private boolean suppressed;
   private int flags;
   private LogiaData logia = new LogiaData();
   private boolean dirty;
   private boolean urgent;

   private static PlayerFruitData decode(
      Optional<ResourceLocation> fruit,
      String form,
      int mastery,
      float masteryXp,
      Optional<ResourceLocation> masteryFruit,
      float energy,
      Map<ResourceLocation, PlayerFruitData.Cooldown> cooldowns,
      int selected,
      List<ResourceLocation> toggles,
      long lastCombat,
      boolean suppressed,
      Optional<Boolean> legacyHasFruit,
      Optional<LogiaData> logia
   ) {
      PlayerFruitData data = new PlayerFruitData();
      data.fruit = fruit.orElse(null);
      if (data.fruit == null && legacyHasFruit.orElse(false)) {
         data.fruit = LegacyData.singleFruitOwner();
      }

      data.form = form;
      data.mastery = Mth.clamp(mastery, 1, 10);
      data.masteryXp = Math.max(0.0F, masteryXp);
      data.masteryFruit = masteryFruit.orElse(data.fruit);
      data.energy = Math.max(0.0F, energy);
      data.cooldowns.putAll(cooldowns);
      data.selected = selected;
      data.toggles.addAll(toggles);
      data.lastCombatTick = lastCombat;
      data.suppressed = suppressed;
      logia.ifPresent(l -> data.logia = l);
      return data;
   }

   public boolean hasFruit() {
      return this.fruit != null;
   }

   @Nullable
   public ResourceLocation fruitId() {
      return this.fruit;
   }

   @Nullable
   public DevilFruit fruit() {
      return FruitRegistry.get(this.fruit);
   }

   public String formName() {
      return this.form;
   }

   @Nullable
   public FruitForm form() {
      DevilFruit f = this.fruit();
      return f == null ? null : f.formOrBase(this.form);
   }

   public int mastery() {
      return this.mastery;
   }

   public float masteryXp() {
      return this.masteryXp;
   }

   @Nullable
   public ResourceLocation masteryFruit() {
      return this.masteryFruit;
   }

   public float energy() {
      return this.energy;
   }

   public int selected() {
      return this.selected;
   }

   public long lastCombatTick() {
      return this.lastCombatTick;
   }

   public boolean suppressed() {
      return this.suppressed;
   }

   public LogiaData logia() {
      return this.logia;
   }

   public Map<ResourceLocation, PlayerFruitData.Cooldown> cooldowns() {
      return this.cooldowns;
   }

   public int flags() {
      return this.flags;
   }

   public boolean hasFlag(int bit) {
      return (this.flags & bit) != 0;
   }

   public boolean isToggleActive(ResourceLocation ability) {
      return this.toggles.contains(ability);
   }

   public Set<ResourceLocation> activeToggles() {
      return this.toggles;
   }

   public boolean hasActiveToggles() {
      return !this.toggles.isEmpty();
   }

   public boolean isOnCooldown(ResourceLocation key, long gameTime) {
      PlayerFruitData.Cooldown cd = this.cooldowns.get(key);
      return cd != null && cd.readyAt() > gameTime;
   }

   public float cooldownFraction(ResourceLocation key, long gameTime, float partialTick) {
      PlayerFruitData.Cooldown cd = this.cooldowns.get(key);
      if (cd != null && cd.duration() > 0) {
         float remaining = (float)(cd.readyAt() - gameTime) - partialTick;
         return Mth.clamp(remaining / (float)cd.duration(), 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   public int cooldownTicksLeft(ResourceLocation key, long gameTime) {
      PlayerFruitData.Cooldown cd = this.cooldowns.get(key);
      return cd == null ? 0 : (int)Math.max(0L, cd.readyAt() - gameTime);
   }

   public void setFruit(@Nullable ResourceLocation fruit) {
      if (!Objects.equals(this.fruit, fruit)) {
         this.fruit = fruit;
         this.form = "";
         this.selected = 0;
         this.markUrgent();
      }
   }

   public void setForm(String form) {
      if (!this.form.equals(form)) {
         this.form = form;
         this.markUrgent();
      }
   }

   public void setMastery(int mastery) {
      this.mastery = Mth.clamp(mastery, 1, 10);
      this.masteryFruit = this.fruit;
      this.markUrgent();
   }

   public void setMasteryXp(float xp) {
      this.masteryXp = Math.max(0.0F, xp);
      this.markDirty();
   }

   public void setEnergy(float energy) {
      float clamped = Math.max(0.0F, energy);
      if (Math.abs(clamped - this.energy) > 1.0E-4F) {
         this.energy = clamped;
         this.markDirty();
      }
   }

   public void setSelected(int selected) {
      if (this.selected != selected) {
         this.selected = selected;
         this.markDirty();
      }
   }

   public void setToggle(ResourceLocation ability, boolean active) {
      if (active ? this.toggles.add(ability) : this.toggles.remove(ability)) {
         this.markUrgent();
      }
   }

   public void clearToggles() {
      if (!this.toggles.isEmpty()) {
         this.toggles.clear();
         this.markUrgent();
      }
   }

   public boolean setFlags(int flags) {
      if (this.flags == flags) {
         return false;
      } else {
         this.flags = flags;
         return true;
      }
   }

   public void setSuppressed(boolean suppressed) {
      if (this.suppressed != suppressed) {
         this.suppressed = suppressed;
         this.markUrgent();
      }
   }

   public void setLastCombatTick(long tick) {
      this.lastCombatTick = tick;
   }

   public void startCooldown(ResourceLocation key, long gameTime, int ticks) {
      if (ticks <= 0) {
         this.cooldowns.remove(key);
      } else {
         this.cooldowns.put(key, new PlayerFruitData.Cooldown(gameTime + (long)ticks, ticks));
      }

      this.markUrgent();
   }

   public void clearCooldowns() {
      this.cooldowns.clear();
      this.markUrgent();
   }

   public void pruneCooldowns(long gameTime) {
      if (this.cooldowns.values().removeIf(cd -> cd.readyAt() <= gameTime)) {
         this.markDirty();
      }
   }

   public void reset(boolean keepMastery) {
      if (!keepMastery) {
         this.mastery = 1;
         this.masteryXp = 0.0F;
         this.masteryFruit = null;
      }

      this.fruit = null;
      this.form = "";
      this.energy = 0.0F;
      this.cooldowns.clear();
      this.selected = 0;
      this.toggles.clear();
      this.suppressed = false;
      this.flags = 0;
      this.logia.reset();
      this.markUrgent();
   }

   public void markDirty() {
      this.dirty = true;
   }

   public void markUrgent() {
      this.dirty = true;
      this.urgent = true;
   }

   public boolean isDirty() {
      return this.dirty;
   }

   public boolean isUrgent() {
      return this.urgent;
   }

   public void clearDirty() {
      this.dirty = false;
      this.urgent = false;
   }

   public boolean sameState(PlayerFruitData o) {
      return Objects.equals(this.fruit, o.fruit)
         && this.form.equals(o.form)
         && this.mastery == o.mastery
         && this.masteryXp == o.masteryXp
         && Objects.equals(this.masteryFruit, o.masteryFruit)
         && this.energy == o.energy
         && this.cooldowns.equals(o.cooldowns)
         && this.selected == o.selected
         && this.toggles.equals(o.toggles)
         && this.lastCombatTick == o.lastCombatTick
         && this.suppressed == o.suppressed
         && this.logia.state() == o.logia.state()
         && this.logia.limbs() == o.logia.limbs()
         && this.logia.counterTimers().equals(o.logia.counterTimers());
   }

   public static record Cooldown(long readyAt, int duration) {
      public static final Codec<PlayerFruitData.Cooldown> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  Codec.LONG.fieldOf("ready_at").forGetter(PlayerFruitData.Cooldown::readyAt),
                  Codec.INT.fieldOf("duration").forGetter(PlayerFruitData.Cooldown::duration)
               )
               .apply(i, PlayerFruitData.Cooldown::new)
      );
   }
}
