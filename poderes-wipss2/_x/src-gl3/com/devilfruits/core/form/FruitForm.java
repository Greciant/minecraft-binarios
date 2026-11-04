package com.devilfruits.core.form;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.config.FormConfig;
import com.devilfruits.core.config.FruitConfigs;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.level.Level;

public final class FruitForm {
   private final String name;
   private final List<FormAbility> abilities;
   private final List<FormAttribute> attributes;
   @Nullable
   private final EntityDimensions dimensions;
   private final boolean flight;
   @Nullable
   private final ResourceLocation renderer;
   @Nullable
   private final ResourceLocation enterAnimation;
   @Nullable
   private final ResourceLocation exitAnimation;
   @Nullable
   private final ResourceLocation enterFx;
   @Nullable
   private final ResourceLocation exitFx;
   private final FormStats defaults;
   private final Set<ResourceKey<Level>> allowedDimensions;
   @Nullable
   private final FormCondition condition;
   @Nullable
   private final Ability ultimate;
   @Nullable
   private final FormAbility primaryAttack;
   @Nullable
   private final Pose forcedPose;
   private final boolean passThroughLeaves;
   private final boolean fireImmune;
   private final float chargedJumpHeight;
   @Nullable
   private final ResourceLocation chargedJumpFx;
   private final boolean boostable;
   private final int timeLimitSeconds;
   private final boolean freeAbilities;
   private final float cameraDistance;
   @Nullable
   private final ResourceLocation windupAnimation;
   private DevilFruit fruit;

   private FruitForm(FruitForm.Builder b, List<FormAbility> abilities) {
      this.name = b.name;
      this.abilities = Collections.unmodifiableList(abilities);
      this.attributes = List.copyOf(b.attributes);
      this.dimensions = b.dimensions;
      this.flight = b.flight;
      this.renderer = b.renderer;
      this.enterAnimation = b.enterAnimation;
      this.exitAnimation = b.exitAnimation;
      this.enterFx = b.enterFx;
      this.exitFx = b.exitFx;
      this.defaults = b.stats.withCastTicks(b.castTicks).withRegenMultiplier(b.regenMultiplier).withBoostDrain(b.boostDrain);
      this.allowedDimensions = Set.copyOf(b.allowedDimensions);
      this.condition = b.condition;
      this.ultimate = b.ultimate;
      this.primaryAttack = b.primaryAttack == null ? null : new FormAbility(b.primaryAttack);
      this.forcedPose = b.forcedPose;
      this.passThroughLeaves = b.passThroughLeaves;
      this.fireImmune = b.fireImmune;
      this.chargedJumpHeight = b.chargedJumpHeight;
      this.chargedJumpFx = b.chargedJumpFx;
      this.boostable = b.boostable;
      this.timeLimitSeconds = b.timeLimitSeconds;
      this.freeAbilities = b.freeAbilities;
      this.cameraDistance = b.cameraDistance;
      this.windupAnimation = b.windupAnimation;
      if (this.ultimate != null && abilities.stream().noneMatch(e -> e.ability() == this.ultimate)) {
         throw new IllegalStateException("Form " + this.name + ": its ultimate " + this.ultimate.id() + " is not in its ability list");
      }
   }

   public static FruitForm.Builder builder(String name) {
      return new FruitForm.Builder(name);
   }

   public String name() {
      return this.name;
   }

   public DevilFruit fruit() {
      return this.fruit;
   }

   public boolean isBase() {
      return this.fruit.baseForm() == this;
   }

   public boolean isHome(PlayerFruitData data) {
      return this == this.fruit.home(data);
   }

   public int index() {
      return this.fruit.forms().indexOf(this);
   }

   public String translationKey() {
      return "form." + this.fruit.id().getNamespace() + "." + this.fruit.id().getPath().replace('/', '.') + "." + this.name;
   }

   public Component displayName() {
      return Component.translatable(this.translationKey());
   }

   public ResourceLocation id() {
      return ResourceLocation.fromNamespaceAndPath(this.fruit.id().getNamespace(), this.fruit.id().getPath() + "/form/" + this.name);
   }

   public ResourceLocation iconSprite() {
      return ResourceLocation.fromNamespaceAndPath(this.fruit.id().getNamespace(), "form/" + this.fruit.id().getPath() + "/" + this.name);
   }

   public FormConfig config() {
      return FruitConfigs.form(this);
   }

   public List<FormAbility> abilities() {
      return this.abilities;
   }

   @Nullable
   public FormAbility find(ResourceLocation abilityId) {
      for (FormAbility entry : this.abilities) {
         if (entry.ability().id().equals(abilityId)) {
            return entry;
         }
      }

      return this.primaryAttack != null && this.primaryAttack.ability().id().equals(abilityId) ? this.primaryAttack : null;
   }

   @Nullable
   public FormAbility primaryAttack() {
      return this.primaryAttack;
   }

   @Nullable
   public Ability ultimate() {
      if (this.ultimate != null) {
         return this.ultimate;
      } else {
         Ability fruitUltimate = this.fruit.ultimate();
         return fruitUltimate != null && this.find(fruitUltimate.id()) != null ? fruitUltimate : null;
      }
   }

   public int indexOf(ResourceLocation abilityId) {
      for (int i = 0; i < this.abilities.size(); i++) {
         if (this.abilities.get(i).ability().id().equals(abilityId)) {
            return i;
         }
      }

      return -1;
   }

   public List<FormAttribute> attributes() {
      return this.attributes;
   }

   @Nullable
   public EntityDimensions dimensions() {
      return this.dimensions;
   }

   public boolean canFly() {
      return this.flight;
   }

   @Nullable
   public ResourceLocation renderer() {
      return this.renderer;
   }

   @Nullable
   public ResourceLocation enterAnimation() {
      return this.enterAnimation;
   }

   @Nullable
   public ResourceLocation exitAnimation() {
      return this.exitAnimation;
   }

   @Nullable
   public ResourceLocation enterFx() {
      return this.enterFx;
   }

   @Nullable
   public ResourceLocation exitFx() {
      return this.exitFx;
   }

   public FormStats defaults() {
      return this.defaults;
   }

   public Set<ResourceKey<Level>> allowedDimensions() {
      return this.allowedDimensions;
   }

   public boolean allowedIn(ResourceKey<Level> dimension) {
      return this.allowedDimensions.isEmpty() || this.allowedDimensions.contains(dimension);
   }

   @Nullable
   public FormCondition condition() {
      return this.condition;
   }

   @Nullable
   public Pose forcedPose() {
      return this.forcedPose;
   }

   public boolean passesThroughLeaves() {
      return this.passThroughLeaves;
   }

   public boolean fireImmune() {
      return this.fireImmune;
   }

   public float chargedJumpHeight() {
      return this.chargedJumpHeight;
   }

   @Nullable
   public ResourceLocation chargedJumpFx() {
      return this.chargedJumpFx;
   }

   public boolean boostable() {
      return this.boostable;
   }

   public int timeLimitSeconds() {
      return this.timeLimitSeconds;
   }

   public boolean freeAbilities() {
      return this.freeAbilities;
   }

   public float cameraDistance() {
      return this.cameraDistance;
   }

   @Nullable
   public ResourceLocation windupAnimation() {
      return this.windupAnimation;
   }

   @Override
   public String toString() {
      return this.fruit == null ? this.name : this.fruit.id() + "#" + this.name;
   }

   public static FruitForm resolve(FruitForm.Builder builder, List<FruitForm> resolved) {
      List<FormAbility> list = new ArrayList<>();
      if (builder.inherit != null) {
         FruitForm parent = resolved.stream()
            .filter(f -> f.name.equals(builder.inherit))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Form " + builder.name + " inherits from unknown (or later) form " + builder.inherit));
         list.addAll(parent.abilities);
      }

      for (FruitForm.Op op : builder.ops) {
         int index = -1;

         for (int i = 0; i < list.size(); i++) {
            if (list.get(i).ability() == op.ability) {
               index = i;
               break;
            }
         }

         switch (op.kind) {
            case ADD:
               if (index >= 0) {
                  throw new IllegalStateException("Form " + builder.name + " lists " + op.ability.id() + " twice (use override for variants)");
               }

               list.add(new FormAbility(op.ability, op.modifiers));
               break;
            case OVERRIDE:
               if (index < 0) {
                  throw new IllegalStateException("Form " + builder.name + " overrides " + op.ability.id() + " which it does not inherit");
               }

               list.set(index, new FormAbility(op.ability, op.modifiers));
               break;
            case REMOVE:
               if (index >= 0) {
                  list.remove(index);
               }
         }
      }

      return new FruitForm(builder, list);
   }

   public void attach(DevilFruit fruit) {
      if (this.fruit != null) {
         throw new IllegalStateException("Form " + this.name + " already belongs to " + this.fruit.id());
      } else {
         this.fruit = fruit;
      }
   }

   public static final class Builder {
      private final String name;
      @Nullable
      private String inherit;
      private final List<FruitForm.Op> ops = new ArrayList<>();
      private final List<FormAttribute> attributes = new ArrayList<>();
      @Nullable
      private EntityDimensions dimensions;
      private boolean flight;
      @Nullable
      private ResourceLocation renderer;
      @Nullable
      private ResourceLocation enterAnimation;
      @Nullable
      private ResourceLocation exitAnimation;
      @Nullable
      private ResourceLocation enterFx;
      @Nullable
      private ResourceLocation exitFx;
      private FormStats stats = FormStats.BASE;
      private final List<ResourceKey<Level>> allowedDimensions = new ArrayList<>();
      @Nullable
      private FormCondition condition;
      @Nullable
      private Ability ultimate;
      @Nullable
      private Ability primaryAttack;
      @Nullable
      private Pose forcedPose;
      private boolean passThroughLeaves;
      private boolean fireImmune;
      private float chargedJumpHeight;
      @Nullable
      private ResourceLocation chargedJumpFx;
      private boolean boostable;
      private int timeLimitSeconds;
      private boolean freeAbilities;
      private double boostDrain;
      private int castTicks;
      private double regenMultiplier = 1.0;
      private float cameraDistance;
      @Nullable
      private ResourceLocation windupAnimation;

      private Builder(String name) {
         if (!name.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Form names must be [a-z0-9_]+: " + name);
         } else {
            this.name = name;
         }
      }

      public String name() {
         return this.name;
      }

      public FruitForm.Builder inherit(String formName) {
         this.inherit = formName;
         return this;
      }

      public FruitForm.Builder ability(Ability ability) {
         return this.ability(ability, AbilityModifiers.NONE);
      }

      public FruitForm.Builder ability(Ability ability, AbilityModifiers modifiers) {
         this.ops.add(new FruitForm.Op(FruitForm.OpKind.ADD, ability, modifiers));
         return this;
      }

      public FruitForm.Builder override(Ability ability, AbilityModifiers modifiers) {
         this.ops.add(new FruitForm.Op(FruitForm.OpKind.OVERRIDE, ability, modifiers));
         return this;
      }

      public FruitForm.Builder remove(Ability ability) {
         this.ops.add(new FruitForm.Op(FruitForm.OpKind.REMOVE, ability, AbilityModifiers.NONE));
         return this;
      }

      public FruitForm.Builder attribute(Holder<Attribute> attribute, double amount, Operation operation) {
         this.attributes.add(new FormAttribute(attribute, amount, operation));
         return this;
      }

      public FruitForm.Builder dimensions(float width, float height, float eyeHeight) {
         this.dimensions = EntityDimensions.scalable(width, height).withEyeHeight(eyeHeight);
         return this;
      }

      public FruitForm.Builder flight() {
         this.flight = true;
         return this;
      }

      public FruitForm.Builder renderer(ResourceLocation renderer) {
         this.renderer = renderer;
         return this;
      }

      public FruitForm.Builder enterAnimation(ResourceLocation animation) {
         this.enterAnimation = animation;
         return this;
      }

      public FruitForm.Builder exitAnimation(ResourceLocation animation) {
         this.exitAnimation = animation;
         return this;
      }

      public FruitForm.Builder enterFx(ResourceLocation fx) {
         this.enterFx = fx;
         return this;
      }

      public FruitForm.Builder exitFx(ResourceLocation fx) {
         this.exitFx = fx;
         return this;
      }

      public FruitForm.Builder stats(FormStats stats) {
         this.stats = stats;
         this.castTicks = stats.castTicks();
         this.regenMultiplier = stats.regenMultiplier();
         this.boostDrain = stats.boostDrainPerSecond();
         return this;
      }

      public FruitForm.Builder stats(int requiredMastery, int enterEnergyCost, double drainPerSecond, int switchCooldownTicks) {
         return this.stats(
            new FormStats(requiredMastery, enterEnergyCost, drainPerSecond, switchCooldownTicks, this.castTicks, this.regenMultiplier, this.boostDrain)
         );
      }

      public FruitForm.Builder allowIn(ResourceKey<Level> dimension) {
         this.allowedDimensions.add(dimension);
         return this;
      }

      public FruitForm.Builder condition(FormCondition condition) {
         this.condition = condition;
         return this;
      }

      public FruitForm.Builder castTime(int ticks) {
         this.castTicks = ticks;
         return this;
      }

      public FruitForm.Builder windupAnimation(ResourceLocation animation) {
         this.windupAnimation = animation;
         return this;
      }

      public FruitForm.Builder regen(double multiplier) {
         this.regenMultiplier = multiplier;
         return this;
      }

      public FruitForm.Builder boost(double drainPerSecond) {
         this.boostable = true;
         this.boostDrain = drainPerSecond;
         return this;
      }

      public FruitForm.Builder timeLimit(int seconds) {
         this.timeLimitSeconds = seconds;
         return this;
      }

      public FruitForm.Builder freeAbilities() {
         this.freeAbilities = true;
         return this;
      }

      public FruitForm.Builder ultimate(Ability ability) {
         this.ultimate = ability;
         return this;
      }

      public FruitForm.Builder primaryAttack(Ability ability) {
         this.primaryAttack = ability;
         return this;
      }

      public FruitForm.Builder forcePose(Pose pose) {
         this.forcedPose = pose;
         return this;
      }

      public FruitForm.Builder passThroughLeaves() {
         this.passThroughLeaves = true;
         return this;
      }

      public FruitForm.Builder fireImmune() {
         this.fireImmune = true;
         return this;
      }

      public FruitForm.Builder chargedJump(float maxHeight, ResourceLocation fx) {
         this.chargedJumpHeight = maxHeight;
         this.chargedJumpFx = fx;
         return this;
      }

      public FruitForm.Builder cameraDistance(float blocks) {
         this.cameraDistance = blocks;
         return this;
      }
   }

   private static record Op(FruitForm.OpKind kind, Ability ability, AbilityModifiers modifiers) {
   }

   private static enum OpKind {
      ADD,
      OVERRIDE,
      REMOVE;
   }
}
