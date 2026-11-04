package com.devilfruits.fruits.suke_suke;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.physics.PhysicsCommand;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.suke_suke.clear.Clear;
import com.devilfruits.fruits.suke_suke.clear.Cloak;
import com.devilfruits.fruits.suke_suke.clear.ShipCloak;
import com.devilfruits.fruits.suke_suke.client.SukeClient;
import com.devilfruits.fruits.suke_suke.datagen.SukeSukeData;
import com.devilfruits.fruits.suke_suke.gametest.SukeGameTests;
import com.devilfruits.registry.DFAttachments;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

public final class SukeSuke implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("suke_suke");
   public static final String BASE = "base";
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("suke_suke/" + path);
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
      SukeItems.init();
      SukeSounds.init();
      SukeTriggers.init();
      Clear.init(ctx.modBus());
      ctx.modBus().addListener(SukeNetwork::register);
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.PARAMECIA)
            .item(SukeItems.FRUIT)
            .form(
               FruitForm.builder("base")
                  .ability(SukeAbilities.INVISIBILITY)
                  .ability(SukeAbilities.HIDDEN_WEAPON)
                  .ability(SukeAbilities.GROUP_INVISIBILITY)
                  .ability(SukeAbilities.SHIP_INVISIBILITY)
            )
            .energy(new EnergyRules(150, 10, 3.0, 8.0))
            .gachaTier(GachaTier.COMMON)
            .lootWeight(6)
            .theme(FruitTheme.sprites("").colors(12578784, 14546376, 8378568, 16777215, 14546376, 12578784))
            .ultimate(SukeAbilities.SHIP_INVISIBILITY)
            .setting("soloSecondsMastery1", 60.0, 5.0, 3600.0, "Seconds a full energy bar keeps the user alone invisible at mastery 1.")
            .setting("soloSecondsMastery10", 300.0, 5.0, 3600.0, "Seconds a full energy bar keeps the user alone invisible at mastery 10 (linear between).")
            .setting("mobVisibility", 0.07, 0.0, 1.0, "How visible a clear entity is to mobs (vanilla's visibility factor: 1 = fully visible).")
            .setting("muffleSteps", 0.5, 0.0, 1.0, "Volume of a clear entity's footsteps (1 = unchanged).")
            .setting(
               "groupRadius",
               3.0,
               1.0,
               16.0,
               "Shared Invisibility takes the players within this many blocks of the user; a member who steps farther shows again at once."
            )
            .setting("shipSecondsMastery8", 30.0, 1.0, 600.0, "Seconds a ship stays invisible at mastery 8.")
            .setting("shipSecondsMastery10", 60.0, 1.0, 600.0, "Seconds a ship stays invisible at mastery 10 (linear between).")
            .setting("shipReach", 16.0, 1.0, 64.0, "Blocks to a ship under the crosshair (when the user does not stand on one).")
            .setting("shipSoundFactor", 0.35, 0.0, 1.0, "Volume of the sounds of a hidden ship (its creaks, its machines), for those who cannot see it.")
            .flag("hideNameTag", true, "Hide the name tag of a clear entity from those who cannot see it.")
            .flag("breakOnAttack", false, "Attacking ends the attacker's invisibility.")
            .flag("spectatorsSee", true, "Spectators see clear entities (the vanilla translucent view).")
            .build()
      );
      SukeEvents.register(ctx.gameBus());
      FruitRules.onPowerRemoved((player, f) -> {
         if (f == fruit) {
            Cloak.end(player);
            Clear.hideItems(player, false);
         }
      });
      PhysicsCommand.registerTest("ship_invisible", ShipCloak::commandTest);
   }

   @Override
   public void registerClient(IEventBus modBus) {
      SukeClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new SukeSukeData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(SukeGameTests.class);
   }
}
