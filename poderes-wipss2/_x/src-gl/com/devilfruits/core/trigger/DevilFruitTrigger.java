package com.devilfruits.core.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger.SimpleInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class DevilFruitTrigger extends SimpleCriterionTrigger<DevilFruitTrigger.TriggerInstance> {
   public Codec<DevilFruitTrigger.TriggerInstance> codec() {
      return DevilFruitTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer player, ResourceLocation fruit) {
      this.trigger(player, fruit, "");
   }

   public void trigger(ServerPlayer player, ResourceLocation fruit, String form) {
      this.trigger(player, instance -> instance.matches(fruit, form));
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceLocation> fruit, Optional<String> form)
      implements SimpleInstance {
      public static final Codec<DevilFruitTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(DevilFruitTrigger.TriggerInstance::player),
                  ResourceLocation.CODEC.optionalFieldOf("fruit").forGetter(DevilFruitTrigger.TriggerInstance::fruit),
                  Codec.STRING.optionalFieldOf("form").forGetter(DevilFruitTrigger.TriggerInstance::form)
               )
               .apply(i, DevilFruitTrigger.TriggerInstance::new)
      );

      boolean matches(ResourceLocation fruit, String form) {
         return this.fruit.<Boolean>map(fruit::equals).orElse(true) && this.form.map(form::equals).orElse(true);
      }

      public static Criterion<DevilFruitTrigger.TriggerInstance> of(DevilFruitTrigger trigger, ResourceLocation fruit) {
         return trigger.createCriterion(new DevilFruitTrigger.TriggerInstance(Optional.empty(), Optional.of(fruit), Optional.empty()));
      }

      public static Criterion<DevilFruitTrigger.TriggerInstance> of(DevilFruitTrigger trigger, ResourceLocation fruit, String form) {
         return trigger.createCriterion(new DevilFruitTrigger.TriggerInstance(Optional.empty(), Optional.of(fruit), Optional.of(form)));
      }
   }
}
