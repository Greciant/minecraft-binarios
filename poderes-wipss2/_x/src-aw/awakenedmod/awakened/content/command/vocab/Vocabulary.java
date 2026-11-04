package com.awakenedmod.awakened.content.command.vocab;

import com.awakenedmod.awakened.content.command.vocab.effect.BleedsEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.BreathesWaterEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.BurnsEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.CannotHurtMeEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.CannotMoveEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.DigsFasterEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.ExplodesEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.FeelsMyPainEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.GathersEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.GlowsEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.HurtsWhenItMovesEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.IsFragileEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.IsLitUpEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.IsPulledEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.IsPushedEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.IsStrongerEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.IsUnseenEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.IsWeakerEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.JumpsHigherEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.LandsSoftlyEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.MovesAtEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.MovesWhereILookEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.ReachesFurtherEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.RegeneratesEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.ReturnsHereEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.SeesInTheDarkEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.SwapsWithMeEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.WalksOnFluidsEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.WeighsATonEffect;
import com.awakenedmod.awakened.content.command.vocab.effect.WeighsNothingEffect;
import com.awakenedmod.awakened.content.command.vocab.target.MeTarget;
import com.awakenedmod.awakened.content.command.vocab.target.MobsNearMeTarget;
import com.awakenedmod.awakened.content.command.vocab.target.WhatHitsMeTarget;
import com.awakenedmod.awakened.content.command.vocab.target.WhatIHitTarget;
import com.awakenedmod.awakened.content.command.vocab.target.WhatILookAtTarget;
import com.awakenedmod.awakened.content.command.vocab.target.WhatIRideTarget;
import com.awakenedmod.awakened.content.command.vocab.target.WhatTargetsMeTarget;
import com.awakenedmod.awakened.content.command.vocab.trigger.AfterIKillTrigger;
import com.awakenedmod.awakened.content.command.vocab.trigger.AlwaysTrigger;
import com.awakenedmod.awakened.content.command.vocab.trigger.AtNightTrigger;
import com.awakenedmod.awakened.content.command.vocab.trigger.BelowHalfHealthTrigger;
import com.awakenedmod.awakened.content.command.vocab.trigger.JustAfterIHitTrigger;
import com.awakenedmod.awakened.content.command.vocab.trigger.UndergroundTrigger;
import com.awakenedmod.awakened.content.command.vocab.trigger.WhileAirborneTrigger;
import com.awakenedmod.awakened.content.command.vocab.trigger.WhileFallingTrigger;
import com.awakenedmod.awakened.content.command.vocab.trigger.WhileSneakingTrigger;
import com.awakenedmod.awakened.content.command.vocab.trigger.WhileSprintingTrigger;
import com.awakenedmod.awakened.content.command.vocab.trigger.WhileStandingStillTrigger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public final class Vocabulary {
   private static final Map<String, Target> TARGETS = new LinkedHashMap<>();
   private static final Map<String, Effect> EFFECTS = new LinkedHashMap<>();
   private static final Map<String, Trigger> TRIGGERS = new LinkedHashMap<>();

   private Vocabulary() {
   }

   private static void target(Target word) {
      put(TARGETS, word.id(), word);
   }

   private static void effect(Effect word) {
      put(EFFECTS, word.id(), word);
   }

   private static void trigger(Trigger word) {
      put(TRIGGERS, word.id(), word);
   }

   private static <T> void put(Map<String, T> book, String id, T word) {
      if (book.put(id, word) != null) {
         throw new IllegalStateException("Duplicate Command word id '" + id + "'");
      }
   }

   @Nullable
   public static Target target(String id) {
      return TARGETS.get(id);
   }

   @Nullable
   public static Effect effect(String id) {
      return EFFECTS.get(id);
   }

   @Nullable
   public static Trigger trigger(String id) {
      return id != null && !id.isEmpty() ? TRIGGERS.get(id) : TRIGGERS.get("always");
   }

   public static Collection<Target> targets() {
      return Collections.unmodifiableCollection(TARGETS.values());
   }

   public static Collection<Effect> effects() {
      return Collections.unmodifiableCollection(EFFECTS.values());
   }

   public static Collection<Trigger> triggers() {
      return Collections.unmodifiableCollection(TRIGGERS.values());
   }

   public static List<Word> all() {
      List<Word> words = new ArrayList<>(TARGETS.size() + EFFECTS.size() + TRIGGERS.size());
      words.addAll(TARGETS.values());
      words.addAll(EFFECTS.values());
      words.addAll(TRIGGERS.values());
      return words;
   }

   static {
      target(new MeTarget());
      target(new WhatIRideTarget());
      target(new WhatIHitTarget());
      target(new WhatILookAtTarget());
      target(new WhatHitsMeTarget());
      target(new WhatTargetsMeTarget());
      target(new MobsNearMeTarget());
      effect(new SeesInTheDarkEffect());
      effect(new BreathesWaterEffect());
      effect(new LandsSoftlyEffect());
      effect(new IsLitUpEffect());
      effect(new JumpsHigherEffect());
      effect(new DigsFasterEffect());
      effect(new BurnsEffect());
      effect(new GlowsEffect());
      effect(new GathersEffect());
      effect(new MovesAtEffect());
      effect(new IsStrongerEffect());
      effect(new ReachesFurtherEffect());
      effect(new SwapsWithMeEffect());
      effect(new WeighsNothingEffect());
      effect(new CannotMoveEffect());
      effect(new IsWeakerEffect());
      effect(new WalksOnFluidsEffect());
      effect(new RegeneratesEffect());
      effect(new IsPushedEffect());
      effect(new IsPulledEffect());
      effect(new WeighsATonEffect());
      effect(new BleedsEffect());
      effect(new HurtsWhenItMovesEffect());
      effect(new IsFragileEffect());
      effect(new ExplodesEffect());
      effect(new IsUnseenEffect());
      effect(new ReturnsHereEffect());
      effect(new MovesWhereILookEffect());
      effect(new FeelsMyPainEffect());
      effect(new CannotHurtMeEffect());
      trigger(new AlwaysTrigger());
      trigger(new WhileSneakingTrigger());
      trigger(new WhileAirborneTrigger());
      trigger(new WhileSprintingTrigger());
      trigger(new UndergroundTrigger());
      trigger(new WhileFallingTrigger());
      trigger(new WhileStandingStillTrigger());
      trigger(new JustAfterIHitTrigger());
      trigger(new AtNightTrigger());
      trigger(new AfterIKillTrigger());
      trigger(new BelowHalfHealthTrigger());
   }
}
