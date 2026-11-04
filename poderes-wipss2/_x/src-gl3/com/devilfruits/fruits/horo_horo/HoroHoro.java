package com.devilfruits.fruits.horo_horo;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.horo_horo.ability.SpecialHollowAbility;
import com.devilfruits.fruits.horo_horo.client.HoroClient;
import com.devilfruits.fruits.horo_horo.datagen.HoroHoroData;
import com.devilfruits.fruits.horo_horo.gametest.HoroGameTests;
import com.devilfruits.fruits.horo_horo.ghost.GhostForm;
import com.devilfruits.fruits.horo_horo.negative.Negative;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

public final class HoroHoro implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("horo_horo");
   public static final String BASE = "base";
   public static final String GHOST = "ghost";
   public static final ResourceKey<DamageType> HOLLOW_DAMAGE = DFDamageTypes.key("horo_horo/hollow");
   public static final ResourceKey<DamageType> BLAST_DAMAGE = DFDamageTypes.key("horo_horo/blast");
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("horo_horo/" + path);
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

   public static boolean isGhost(Player player) {
      return isUser(player) && "ghost".equals(DFAttachments.data(player).formName());
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
      HoroItems.init();
      HoroEntities.init();
      HoroSounds.init();
      HoroTriggers.init();
      Negative.init(ctx.modBus());
      GhostForm.init();
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.PARAMECIA)
            .item(HoroItems.FRUIT)
            .form(
               FruitForm.builder("base")
                  .ability(HoroAbilities.NEGATIVE_HOLLOW)
                  .ability(HoroAbilities.MINI_HOLLOW)
                  .ability(HoroAbilities.GHOST_FORM)
                  .ability(HoroAbilities.SPECIAL_HOLLOW)
            )
            .form(
               FruitForm.builder("ghost")
                  .ability(HoroAbilities.NEGATIVE_HOLLOW)
                  .ability(HoroAbilities.MINI_HOLLOW)
                  .ability(HoroAbilities.GHOST_FORM)
                  .ability(HoroAbilities.SPECIAL_HOLLOW)
                  .flight()
                  .stats(1, 0, 0.0, 0)
                  .enterFx(HoroFx.QUIET)
                  .exitFx(HoroFx.QUIET)
                  .condition((player, data) -> GhostForm.active(player) ? null : Component.translatable("message.devilfruits.horo_horo.not_a_ghost"))
            )
            .energy(new EnergyRules(150, 10, 3.0, 8.0))
            .lootWeight(6)
            .theme(FruitTheme.sprites("horo_horo").colors(16758741, 16775163, 16740264, 13219583, 16775163, 16758741))
            .ultimate(HoroAbilities.SPECIAL_HOLLOW)
            .damageType(HOLLOW_DAMAGE)
            .flag("blocksAbilities", true, "A creature in Negative cannot cast Devil Fruit abilities.")
            .setting("bossNegativeFactor", 0.4, 0.0, 1.0, "Bosses stay in Negative (and swallowed by Special Hollow) this share of the time.")
            .setting("ghostLeash", 64.0, 8.0, 256.0, "Blocks the ghost may go from its body; beyond, a soft pull draws it back.")
            .setting("recallHoldSeconds", 3.0, 0.5, 10.0, "Seconds the Ghost Form key is held to recall the ghost to its body.")
            .setting("possessReach", 4.0, 1.0, 16.0, "Blocks from its body the ghost may be to right-click it and return.")
            .build()
      );
      GhostForm.register(ctx.gameBus());
      Negative.register(ctx.gameBus());
      HoroEvents.register(ctx.gameBus());
      AbilityManager.addCastBlocker(Negative::castBlocked);
      AbilityManager.addCastBlocker(SpecialHollowAbility::castBlocked);
      FruitRules.onPowerRemoved((player, f) -> {
         if (f == fruit) {
            GhostForm.onPowerRemoved(player);
         }
      });
   }

   @Override
   public void registerClient(IEventBus modBus) {
      HoroClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new HoroHoroData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(HoroGameTests.class);
   }
}
