package com.devilfruits.core.fruit;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.config.FruitConfig;
import com.devilfruits.core.config.FruitConfigs;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.logia.LogiaProfile;
import com.devilfruits.registry.DFDamageTypes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;

public final class DevilFruit {
   private final ResourceLocation id;
   private final DevilFruitType type;
   @Nullable
   private final Supplier<? extends Item> item;
   private final List<FruitForm> forms;
   private final Map<String, FruitForm> formsByName;
   private final List<Ability> abilities;
   private final EnergyRules energyDefaults;
   private final int lootWeight;
   private final GachaTier gachaTier;
   private final boolean waterWeakness;
   private final boolean reincarnates;
   private final FruitTheme theme;
   private final FruitFx fx;
   @Nullable
   private final Ability ultimate;
   private final ResourceKey<DamageType> damageType;
   private final boolean devOnly;
   private final List<DevilFruit.Setting> settings;
   @Nullable
   private final LogiaProfile logia;
   private final List<DevilFruit.ConfigUpdate> configUpdates;

   private DevilFruit(DevilFruit.Builder b) {
      this.id = b.id;
      this.type = b.type;
      this.item = b.item;
      List<FruitForm> resolved = new ArrayList<>();

      for (FruitForm.Builder form : b.forms) {
         if (resolved.stream().anyMatch(f -> f.name().equals(form.name()))) {
            throw new IllegalStateException(b.id + " declares form " + form.name() + " twice");
         }

         resolved.add(FruitForm.resolve(form, resolved));
      }

      if (resolved.isEmpty()) {
         throw new IllegalStateException(b.id + " has no forms");
      } else {
         this.forms = Collections.unmodifiableList(resolved);
         Map<String, FruitForm> byName = new LinkedHashMap<>();
         Set<Ability> all = new LinkedHashSet<>();

         for (FruitForm form : resolved) {
            form.attach(this);
            byName.put(form.name(), form);

            for (FormAbility entry : form.abilities()) {
               all.add(entry.ability());
            }
         }

         this.formsByName = Collections.unmodifiableMap(byName);
         this.abilities = List.copyOf(all);
         this.energyDefaults = b.energy;
         this.lootWeight = b.lootWeight;
         this.gachaTier = b.gachaTier != null ? b.gachaTier : GachaTier.of(this.type);
         this.waterWeakness = b.waterWeakness;
         this.reincarnates = b.reincarnates;
         this.theme = b.theme;
         this.fx = b.fx;
         this.ultimate = b.ultimate;
         this.damageType = b.damageType;
         this.devOnly = b.devOnly;
         this.settings = List.copyOf(b.settings);
         this.logia = b.logia;
         this.configUpdates = List.copyOf(b.configUpdates);
         if (this.logia != null && this.type != DevilFruitType.LOGIA) {
            throw new IllegalStateException(this.id + ": a Logia profile needs DevilFruitType.LOGIA, not " + this.type);
         } else if (this.ultimate != null && !this.abilities.contains(this.ultimate)) {
            throw new IllegalStateException(this.id + ": the ultimate " + this.ultimate.id() + " is not listed in any form");
         }
      }
   }

   public static DevilFruit.Builder builder(ResourceLocation id, DevilFruitType type) {
      return new DevilFruit.Builder(id, type);
   }

   public ResourceLocation id() {
      return this.id;
   }

   public DevilFruitType type() {
      return this.type;
   }

   public String translationKey() {
      return "fruit." + this.id.getNamespace() + "." + this.id.getPath().replace('/', '.');
   }

   public String key(String suffix) {
      return this.translationKey() + "." + suffix;
   }

   public MutableComponent displayName() {
      return Component.translatable(this.translationKey());
   }

   public MutableComponent message(String name, Object... args) {
      return Component.translatable(this.key("message." + name), args);
   }

   public Optional<Item> item() {
      return this.item == null ? Optional.empty() : Optional.of(this.item.get());
   }

   public boolean hasItem() {
      return this.item != null;
   }

   public List<FruitForm> forms() {
      return this.forms;
   }

   public FruitForm baseForm() {
      return this.forms.get(0);
   }

   @Nullable
   public FruitForm form(String name) {
      return this.formsByName.get(name);
   }

   public FruitForm formOrBase(String name) {
      FruitForm form = this.formsByName.get(name);
      return form != null ? form : this.baseForm();
   }

   public boolean hasForms() {
      return this.forms.size() > 1;
   }

   public List<Ability> abilities() {
      return this.abilities;
   }

   @Nullable
   public Ability ultimate() {
      return this.ultimate;
   }

   public EnergyRules energyDefaults() {
      return this.energyDefaults;
   }

   public int defaultLootWeight() {
      return this.lootWeight;
   }

   public GachaTier defaultGachaTier() {
      return this.gachaTier;
   }

   public boolean waterWeakness() {
      return this.waterWeakness;
   }

   public boolean reincarnates() {
      return this.reincarnates;
   }

   public FruitTheme theme() {
      return this.theme;
   }

   public FruitFx fx() {
      return this.fx;
   }

   public ResourceKey<DamageType> damageType() {
      return this.damageType;
   }

   public List<DevilFruit.Setting> settings() {
      return this.settings;
   }

   public boolean isDevOnly() {
      return this.devOnly;
   }

   @Nullable
   public LogiaProfile logia() {
      return this.logia;
   }

   public boolean isLogia() {
      return this.logia != null;
   }

   public List<DevilFruit.ConfigUpdate> configUpdates() {
      return this.configUpdates;
   }

   public int configRevision() {
      return this.configUpdates.stream().mapToInt(DevilFruit.ConfigUpdate::revision).max().orElse(0);
   }

   public FruitConfig config() {
      return FruitConfigs.fruit(this);
   }

   @Override
   public String toString() {
      return this.id.toString();
   }

   public static final class Builder {
      private final ResourceLocation id;
      private final DevilFruitType type;
      @Nullable
      private Supplier<? extends Item> item;
      private final List<FruitForm.Builder> forms = new ArrayList<>();
      private EnergyRules energy = EnergyRules.DEFAULT;
      private int lootWeight = 10;
      @Nullable
      private GachaTier gachaTier;
      private boolean waterWeakness = true;
      private boolean reincarnates = true;
      private FruitTheme theme = FruitTheme.DEFAULT;
      private FruitFx fx = FruitFx.DEFAULT;
      @Nullable
      private Ability ultimate;
      private ResourceKey<DamageType> damageType = DFDamageTypes.DEVIL_FRUIT;
      private boolean devOnly;
      private final List<DevilFruit.Setting> settings = new ArrayList<>();
      @Nullable
      private LogiaProfile logia;
      private final List<DevilFruit.ConfigUpdate> configUpdates = new ArrayList<>();

      private Builder(ResourceLocation id, DevilFruitType type) {
         this.id = id;
         this.type = type;
      }

      public DevilFruit.Builder item(Supplier<? extends Item> item) {
         this.item = item;
         return this;
      }

      public DevilFruit.Builder form(FruitForm.Builder form) {
         this.forms.add(form);
         return this;
      }

      public DevilFruit.Builder energy(EnergyRules energy) {
         this.energy = energy;
         return this;
      }

      public DevilFruit.Builder lootWeight(int weight) {
         this.lootWeight = weight;
         return this;
      }

      public DevilFruit.Builder gachaTier(GachaTier tier) {
         this.gachaTier = tier;
         return this;
      }

      public DevilFruit.Builder waterWeakness(boolean weak) {
         this.waterWeakness = weak;
         return this;
      }

      public DevilFruit.Builder reincarnates(boolean reincarnates) {
         this.reincarnates = reincarnates;
         return this;
      }

      public DevilFruit.Builder theme(FruitTheme theme) {
         this.theme = theme;
         return this;
      }

      public DevilFruit.Builder fx(FruitFx fx) {
         this.fx = fx;
         return this;
      }

      public DevilFruit.Builder ultimate(Ability ability) {
         this.ultimate = ability;
         return this;
      }

      public DevilFruit.Builder damageType(ResourceKey<DamageType> damageType) {
         this.damageType = damageType;
         return this;
      }

      public DevilFruit.Builder devOnly() {
         this.devOnly = true;
         return this;
      }

      public DevilFruit.Builder logia(LogiaProfile profile) {
         this.logia = profile;
         return this;
      }

      public DevilFruit.Builder logia(LogiaProfile.Builder profile) {
         return this.logia(profile.build());
      }

      public DevilFruit.Builder setting(String name, double def, double min, double max, String comment) {
         this.settings.add(new DevilFruit.Setting(name, def, min, max, false, comment));
         return this;
      }

      public DevilFruit.Builder flag(String name, boolean def, String comment) {
         this.settings.add(new DevilFruit.Setting(name, def ? 1.0 : 0.0, 0.0, 1.0, true, comment));
         return this;
      }

      public DevilFruit.Builder configUpdate(int revision, String... paths) {
         this.configUpdates.add(new DevilFruit.ConfigUpdate(revision, List.of(paths)));
         return this;
      }

      public DevilFruit build() {
         return new DevilFruit(this);
      }
   }

   public static record ConfigUpdate(int revision, List<String> paths) {
   }

   public static record Setting(String name, double defaultValue, double min, double max, boolean flag, String comment) {
   }
}
