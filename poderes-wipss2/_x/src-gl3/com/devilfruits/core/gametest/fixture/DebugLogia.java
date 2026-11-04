package com.devilfruits.core.gametest.fixture;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.DebugLogiaAutotest;
import com.devilfruits.client.logia.LogiaThemes;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.logia.CounterTrigger;
import com.devilfruits.core.logia.DamageCategory;
import com.devilfruits.core.logia.HitRule;
import com.devilfruits.core.logia.LogiaCounter;
import com.devilfruits.core.logia.LogiaEnvironment;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaMovement;
import com.devilfruits.core.logia.LogiaMovementMode;
import com.devilfruits.core.logia.LogiaProfile;
import com.devilfruits.core.util.Targeting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.IEventBus;

public final class DebugLogia implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("debug_logia");
   public static final ResourceLocation ELEMENT = DevilFruits.id("test_element");
   private static final ResourceLocation GENERIC_ICON = DevilFruits.id("ability/generic");
   public static final DebugLogia.Pulse PULSE = new DebugLogia.Pulse();
   public static final DebugLogia.Drift DRIFT = new DebugLogia.Drift();
   public static final DebugLogia.DriftMode DRIFT_MODE = new DebugLogia.DriftMode();
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("debug_logia/" + path);
   }

   public static DevilFruit fruit() {
      return fruit;
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   @Override
   public void registerClient(IEventBus modBus) {
      LogiaThemes.registerDebug(ELEMENT);
      DebugLogiaAutotest.register();
   }

   @Override
   public void register(FruitModuleContext ctx) {
      LogiaMovement.register(DRIFT_MODE);
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.LOGIA)
            .form(FruitForm.builder("base").ability(PULSE).ability(DRIFT))
            .logia(
               LogiaProfile.builder(ELEMENT)
                  .rule(DamageCategory.FIRE, HitRule.CONNECT)
                  .counter(
                     LogiaCounter.builder("wet")
                        .trigger(CounterTrigger.Kind.IN_WATER)
                        .trigger(CounterTrigger.Kind.RAIN)
                        .trigger(CounterTrigger.Kind.SPLASH_WATER)
                        .trigger(CounterTrigger.Kind.WATER_BUCKET)
                        .action(LogiaCounter.Action.SUPPRESS_INTANGIBILITY, LogiaCounter.Action.BLOCK_ABILITIES, LogiaCounter.Action.FORCE_REFORM)
                        .slow(0.2F)
                        .linger(60)
                        .icon(DevilFruits.id("logia/wet"))
                  )
                  .counter(
                     LogiaCounter.builder("seastone")
                        .trigger(CounterTrigger.Kind.SEASTONE_TOUCH)
                        .trigger(CounterTrigger.seastoneNear(2.0))
                        .action(LogiaCounter.Action.SUPPRESS_INTANGIBILITY, LogiaCounter.Action.FORCE_REFORM)
                        .linger(10)
                        .icon(DevilFruits.id("logia/seastone"))
                  )
                  .environment(new LogiaEnvironment(BlockTags.WOOL, null, 2.0, null, 0.5, true))
                  .elementalBody(true, 10.0F)
            )
            .energy(new EnergyRules(100, 10, 2.5, 7.0))
            .lootWeight(0)
            .devOnly()
            .build()
      );
   }

   public abstract static class DebugAbility extends Ability {
      DebugAbility(String name, CastType type, AbilityStats stats) {
         super(DebugLogia.id(name), type, stats);
      }

      @Override
      public ResourceLocation iconSprite() {
         return DebugLogia.GENERIC_ICON;
      }
   }

   public static final class Drift extends DebugLogia.DebugAbility {
      Drift() {
         super("drift", CastType.INSTANT, AbilityStats.builder().energy(5).cooldown(20).damage(0.0).range(8.0).build());
      }

      @Override
      public void activate(AbilityContext ctx) {
         LogiaMovement.start(ctx.player(), DebugLogia.DRIFT_MODE);
      }
   }

   public static final class DriftMode implements LogiaMovementMode {
      @Override
      public ResourceLocation id() {
         return DebugLogia.id("drift");
      }

      @Override
      public boolean tick(ServerPlayer player, LogiaMovement.Session session) {
         Targeting.moveSelf(player, player.getLookAngle().scale(0.5));
         return session.ticks < 20;
      }

      @Override
      public int maxTicks() {
         return 40;
      }
   }

   public static final class Pulse extends DebugLogia.DebugAbility {
      Pulse() {
         super("pulse", CastType.INSTANT, AbilityStats.builder().energy(2).cooldown(10).damage(2.0).range(3.0).build());
      }

      @Override
      public void activate(AbilityContext ctx) {
         LogiaManager.partial(ctx.player(), 12, 10);
      }
   }
}
