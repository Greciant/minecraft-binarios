package com.devilfruits.fruits.gomu_gomu;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.gear.GearDefinition;
import com.devilfruits.core.gear.GearRegistry;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.gomu_gomu.client.GomuClient;
import com.devilfruits.fruits.gomu_gomu.datagen.GomuGomuData;
import com.devilfruits.fruits.gomu_gomu.gametest.GomuGameTests;
import com.devilfruits.fruits.gomu_gomu.rubber.BulletCatch;
import com.devilfruits.registry.DFAttachments;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

public final class GomuGomu implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("gomu_gomu");
   public static final String BASE = "base";
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("gomu_gomu/" + path);
   }

   public static DevilFruit fruit() {
      return fruit;
   }

   public static boolean isUser(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) && ID.equals(DFAttachments.data(player).fruitId());
   }

   public static boolean canUse(Player player) {
      return isUser(player) && !player.isSpectator() && !Weakness.isSuppressed(player);
   }

   public static double setting(String name) {
      return fruit.config().setting(name);
   }

   public static boolean flag(String name) {
      return fruit.config().flag(name);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   @Override
   public void register(FruitModuleContext ctx) {
      GomuItems.init();
      GomuSounds.init();
      GomuTriggers.init();
      ctx.modBus().addListener(GomuNetwork::register);
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.PARAMECIA)
            .item(GomuItems.FRUIT)
            .form(
               FruitForm.builder("base")
                  .ability(GomuAbilities.PISTOL)
                  .ability(GomuAbilities.BAZOOKA)
                  .ability(GomuAbilities.ROCKET)
                  .ability(GomuAbilities.AXE)
                  .ability(GomuAbilities.RIFLE)
                  .ability(GomuAbilities.GATLING)
            )
            .energy(new EnergyRules(140, 10, 3.0, 8.0))
            .gachaTier(GachaTier.LEGENDARY)
            .ultimate(GomuAbilities.GATLING)
            .lootWeight(6)
            .theme(FruitTheme.sprites("").colors(15194367, 16773598, 12098559, 16765803, 16773598, 16165064))
            .flag(
               "bluntImmunity",
               true,
               "Blunt blows do nothing: fists, clubs, the mace, shovels and sticks, unarmed blunt mobs, falling blocks, cramming, flying into walls."
            )
            .flag("fallImmunity", true, "No fall damage from any height (a rubbery landing instead).")
            .flag("bulletImmunity", true, "Bullets (#devilfruits:bullets and bulletEntities) are caught in a dent and shot back.")
            .flag("lightningImmunity", true, "Lightning and #devilfruits:electric damage do nothing (rubber insulates).")
            .setting("bulletHoldTicks", 20.0, 1.0, 200.0, "Ticks a caught bullet stays in its dent before it is shot back.")
            .setting("returnedBulletSpeed", 0.8, 0.0, 3.0, "Speed of a returned bullet, as a share of its incoming speed.")
            .setting("returnedBulletDamageMultiplier", 1.0, 0.0, 10.0, "Factor on a returned bullet's own damage (its \"Damage\" / \"damage\" tag).")
            .setting("rubberLandingHeight", 4.0, 0.0, 256.0, "Fall distance (blocks) from which the landing plays the rubbery squash and rebound.")
            .list("bulletEntities", List.of(), "Entity ids of other mods' bullets, caught like #devilfruits:bullets (e.g. \"somemod:bullet\").")
            .configUpdate(
               1,
               "abilities.pistol.cooldownTicks",
               "abilities.bazooka.cooldownTicks",
               "abilities.rifle.cooldownTicks",
               "abilities.axe.cooldownTicks",
               "abilities.rocket.cooldownTicks",
               "abilities.rocket.pullAccel",
               "abilities.rocket.maxPullSpeed",
               "abilities.rocket.arrivalHop",
               "abilities.rocket.arrivalCarry",
               "abilities.axe.axeCraterRadius"
            )
            .build()
      );
      GearRegistry.register(
         ID,
         GearDefinition.builder("gear_1", 1).build(),
         GearDefinition.builder("gear_2", 2).placeholder().build(),
         GearDefinition.builder("gear_3", 3).placeholder().build(),
         GearDefinition.builder("gear_4", 4)
            .placeholder()
            .sub(GearDefinition.builder("bounce_man", 0).placeholder())
            .sub(GearDefinition.builder("snake_man", 1).placeholder())
            .build(),
         GearDefinition.builder("gear_5", 5).placeholder().build()
      );
      GomuEvents.register(ctx.gameBus());
      GomuSableTest.register();
      FruitRules.onPowerRemoved((player, f) -> {
         if (f == fruit) {
            BulletCatch.release(player);
         }
      });
   }

   @Override
   public void registerClient(IEventBus modBus) {
      GomuClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new GomuGomuData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(GomuGameTests.class);
   }
}
