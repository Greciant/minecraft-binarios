package com.devilfruits.core.gametest.fixture;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.DebugZoanAutotest;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.ability.FormSwitchAbility;
import com.devilfruits.core.form.AbilityModifiers;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fx.ServerFx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.bus.api.IEventBus;

public final class DebugZoan implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("debug_zoan");
   public static final DebugZoan.Strike STRIKE = new DebugZoan.Strike();
   public static final DebugZoan.Guard GUARD = new DebugZoan.Guard();
   public static final DebugZoan.Breath BREATH = new DebugZoan.Breath();
   public static final DebugZoan.Flurry FLURRY = new DebugZoan.Flurry();
   public static final FormSwitchAbility TRANSFORM = new FormSwitchAbility(id("transform"), "beast") {
      @Override
      public ResourceLocation iconSprite() {
         return DebugZoan.GENERIC_ICON;
      }
   };
   private static final ResourceLocation GENERIC_ICON = DevilFruits.id("ability/generic");
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("debug_zoan/" + path);
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
      DebugZoanAutotest.register();
   }

   @Override
   public void register(FruitModuleContext ctx) {
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.MYTHICAL_ZOAN)
            .form(FruitForm.builder("base").ability(STRIKE).ability(GUARD).ability(TRANSFORM))
            .form(
               FruitForm.builder("beast")
                  .inherit("base")
                  .override(STRIKE, AbilityModifiers.variant("beast").damage(2.0F).range(1.5F).cooldown(0.5F).build())
                  .ability(BREATH)
                  .ability(FLURRY)
                  .attribute(Attributes.SCALE, 0.5, Operation.ADD_VALUE)
                  .attribute(Attributes.MAX_HEALTH, 10.0, Operation.ADD_VALUE)
                  .attribute(Attributes.STEP_HEIGHT, 0.5, Operation.ADD_VALUE)
                  .attribute(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE)
                  .stats(1, 10, 5.0, 20)
            )
            .form(
               FruitForm.builder("winged")
                  .inherit("base")
                  .remove(GUARD)
                  .flight()
                  .boost(3.0)
                  .passThroughLeaves()
                  .dimensions(1.2F, 1.2F, 1.0F)
                  .stats(3, 20, 4.0, 0)
            )
            .form(
               FruitForm.builder("giant")
                  .inherit("base")
                  .ultimate(STRIKE)
                  .castTime(10)
                  .regen(0.5)
                  .forcePose(Pose.STANDING)
                  .cameraDistance(8.0F)
                  .stats(1, 5, 0.0, 0)
            )
            .energy(new EnergyRules(100, 10, 2.5, 7.0))
            .lootWeight(0)
            .devOnly()
            .build()
      );
   }

   public static final class Breath extends DebugZoan.DebugAbility {
      Breath() {
         super("breath", CastType.HELD, AbilityStats.builder().energy(8).cooldown(60).build());
      }

      @Override
      public int maxChannelTicks() {
         return 30;
      }

      @Override
      public void onChannelStart(AbilityContext ctx) {
         this.log.add("start");
      }

      @Override
      public boolean channelTick(AbilityContext ctx, int ticks) {
         this.log.add("tick");
         return true;
      }

      @Override
      public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
         this.log.add(interrupted ? "interrupted" : "end");
      }
   }

   public abstract static class DebugAbility extends Ability {
      public final List<String> log = new ArrayList<>();

      DebugAbility(String name, CastType type, AbilityStats stats) {
         super(DebugZoan.id(name), type, stats);
      }

      @Override
      public ResourceLocation iconSprite() {
         return DebugZoan.GENERIC_ICON;
      }
   }

   public static final class Flurry extends DebugZoan.DebugAbility {
      Flurry() {
         super("flurry", CastType.COMBO, AbilityStats.builder().energy(3).cooldown(50).build());
      }

      @Override
      public void comboStep(AbilityContext ctx, int step) {
         this.log.add("step" + step);
      }
   }

   public static final class Guard extends DebugZoan.DebugAbility {
      Guard() {
         super("guard", CastType.TOGGLE, AbilityStats.builder().energy(5).cooldown(20).build());
      }

      @Override
      public void onToggle(AbilityContext ctx, boolean active) {
         this.log.add(active ? "on" : "off");
      }

      @Override
      public void toggleTick(AbilityContext ctx) {
         ctx.data().setEnergy(ctx.data().energy() - 0.1F);
      }
   }

   public static final class Strike extends DebugZoan.DebugAbility {
      Strike() {
         super("strike", CastType.INSTANT, AbilityStats.builder().energy(5).cooldown(40).damage(4.0).range(3.0).build());
      }

      @Override
      public void activate(AbilityContext ctx) {
         this.log.add(ctx.variant() + ":" + ctx.damage());
         ServerFx.shake(ctx.level(), ctx.player().position(), 0.2F, 8);
      }
   }
}
