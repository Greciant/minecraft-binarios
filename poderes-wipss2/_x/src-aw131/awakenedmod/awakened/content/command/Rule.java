package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import com.awakenedmod.awakened.content.command.vocab.Vocabulary;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Rule(String targetId, String effectId, int level, int radius, int seconds, String triggerId) {
   public static final int INFINITE = -1;
   public static final int INSTANT_SECONDS = 1;
   public static final int DEFAULT_RADIUS = 4;
   public static final Rule EMPTY = new Rule("", "", 1, 0, 0, "always");
   public static final MapCodec<Rule> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Codec.STRING.optionalFieldOf("target", "").forGetter(Rule::targetId),
               Codec.STRING.optionalFieldOf("effect", "").forGetter(Rule::effectId),
               Codec.INT.optionalFieldOf("level", 1).forGetter(Rule::level),
               Codec.INT.optionalFieldOf("seconds", 0).forGetter(Rule::seconds),
               Codec.STRING.optionalFieldOf("trigger", "always").forGetter(Rule::triggerId),
               Codec.INT.optionalFieldOf("radius").forGetter(rule -> Optional.of(rule.radius()))
            )
            .apply(instance, Rule::fromSave)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, Rule> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      Rule::targetId,
      ByteBufCodecs.STRING_UTF8,
      Rule::effectId,
      ByteBufCodecs.INT,
      Rule::level,
      ByteBufCodecs.INT,
      Rule::radius,
      ByteBufCodecs.INT,
      Rule::seconds,
      ByteBufCodecs.STRING_UTF8,
      Rule::triggerId,
      Rule::new
   );

   public Rule(String targetId, String effectId, int level, int seconds, String triggerId) {
      this(targetId, effectId, level, radiusTarget(targetId) ? 4 : 0, seconds, triggerId);
   }

   public Rule(String targetId, String effectId, int level, int radius, int seconds, String triggerId) {
      if (effectId != null && !effectId.isEmpty()) {
         Effect effect = Vocabulary.effect(effectId);
         Target about = targetId != null && !targetId.isEmpty() ? Vocabulary.target(targetId) : null;
         if (effect != null && effect.instant() && (about == null || !about.repeatsInstantWords())) {
            seconds = 1;
         }
      }

      this.targetId = targetId;
      this.effectId = effectId;
      this.level = level;
      this.radius = radius;
      this.seconds = seconds;
      this.triggerId = triggerId;
   }

   static Rule fromSave(String targetId, String effectId, int level, int seconds, String triggerId, Optional<Integer> radius) {
      if (radius.isPresent()) {
         return new Rule(targetId, effectId, level, radius.get(), seconds, triggerId);
      } else {
         return radiusTarget(targetId)
            ? new Rule(targetId, effectId, 1, level, seconds, triggerId)
            : new Rule(targetId, effectId, level, 0, seconds, triggerId);
      }
   }

   private static boolean radiusTarget(String targetId) {
      Target target = targetId != null && !targetId.isEmpty() ? Vocabulary.target(targetId) : null;
      return target != null && target.usesRadius();
   }

   public Rule written() {
      return new Rule(
         this.targetId,
         this.effectId,
         this.levelled() ? Math.max(1, this.level) : 1,
         this.radial() ? Math.max(1, this.radius) : 0,
         this.seconds,
         this.triggerId
      );
   }

   public Rule withTarget(String id) {
      return new Rule(id, this.effectId, this.level, this.radius, this.seconds, this.triggerId);
   }

   public Rule withEffect(String id) {
      return new Rule(this.targetId, id, this.level, this.radius, this.seconds, this.triggerId);
   }

   public Rule withTrigger(String id) {
      return new Rule(this.targetId, this.effectId, this.level, this.radius, this.seconds, id);
   }

   public Rule withLevel(int value) {
      return new Rule(this.targetId, this.effectId, value, this.radius, this.seconds, this.triggerId);
   }

   public Rule withRadius(int value) {
      return new Rule(this.targetId, this.effectId, this.level, value, this.seconds, this.triggerId);
   }

   public Rule withSeconds(int value) {
      return new Rule(this.targetId, this.effectId, this.level, this.radius, value, this.triggerId);
   }

   public boolean instant() {
      Effect effect = this.effect();
      if (effect != null && effect.instant()) {
         Target target = this.target();
         return target == null || !target.repeatsInstantWords();
      } else {
         return false;
      }
   }

   public boolean numbered() {
      return this.levelled() || this.radial();
   }

   public boolean levelled() {
      Effect effect = this.effect();
      return this.target() != null && effect != null && effect.hasLevel();
   }

   public boolean radial() {
      Target target = this.target();
      return target != null && this.effect() != null && target.usesRadius();
   }

   public boolean allowsInfinite() {
      Target target = this.target();
      return !this.instant() && (target == null || target.allowsInfinite());
   }

   public boolean isEmpty() {
      return this.targetId.isEmpty() || this.effectId.isEmpty();
   }

   public boolean infinite() {
      return this.seconds == -1;
   }

   public int durationTicks() {
      return this.infinite() ? -1 : Math.max(0, this.seconds) * 20;
   }

   @Nullable
   public Target target() {
      return Vocabulary.target(this.targetId);
   }

   @Nullable
   public Effect effect() {
      return Vocabulary.effect(this.effectId);
   }

   @Nullable
   public Trigger trigger() {
      return Vocabulary.trigger(this.triggerId);
   }

   public boolean resolves() {
      return !this.isEmpty() && this.target() != null && this.effect() != null && this.trigger() != null;
   }

   public Component describe() {
      return this.describe(-1);
   }

   public Component describe(int secondsLeft) {
      Target target = this.target();
      Effect effect = this.effect();
      Trigger trigger = this.trigger();
      if (target != null && effect != null && trigger != null) {
         MutableComponent line = Component.empty().append(target.displayName()).append(" ").append(effect.displayName());
         if (effect.hasLevel()) {
            line.append(" ").append(effect.numberText(this.level));
         }

         if (target.usesRadius()) {
            line.append(" ").append(Component.translatable("awakened.command.rule.within", new Object[]{this.radius}));
         }

         if (!this.instant()) {
            line.append(" ")
               .append(
                  this.infinite()
                     ? Component.translatable(target.kind().lands() ? "awakened.command.rule.forever_one" : "awakened.command.rule.forever")
                     : Component.translatable("awakened.command.rule.for", new Object[]{secondsLeft >= 0 ? secondsLeft : this.seconds})
               );
         }

         if (!"always".equals(trigger.id())) {
            line.append(" ").append(trigger.displayName());
         }

         return line;
      } else {
         return Component.translatable("awakened.command.rule.empty");
      }
   }
}
