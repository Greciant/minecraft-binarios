package com.devilfruits.fruits.supa_supa;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitFx;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.physics.PhysicsCommand;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.supa_supa.client.SupaClient;
import com.devilfruits.fruits.supa_supa.datagen.SupaSupaData;
import com.devilfruits.fruits.supa_supa.gametest.SupaGameTests;
import com.devilfruits.fruits.supa_supa.gametest.SupaSlashGameTests;
import com.devilfruits.fruits.supa_supa.slash.SlashCommandTest;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

public final class SupaSupa implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("supa_supa");
   public static final String BASE = "base";
   public static final ResourceKey<DamageType> BLADE_DAMAGE = DFDamageTypes.key("supa_supa/blade");
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("supa_supa/" + path);
   }

   public static DevilFruit fruit() {
      return fruit;
   }

   public static boolean isUser(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) && ID.equals(DFAttachments.data(player).fruitId());
   }

   @Nullable
   public static AbilityContext context(ServerPlayer player, Ability ability) {
      PlayerFruitData data = DFAttachments.data(player);
      FormAbility entry = AbilityManager.entryFor(data, ability);
      return entry == null ? null : new AbilityContext(entry, player, data, 1.0F);
   }

   public static boolean active(Player player) {
      return isUser(player) && !player.isSpectator() && !Weakness.isSuppressed(player);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   @Override
   public void register(FruitModuleContext ctx) {
      SupaItems.init();
      SupaSounds.init();
      SupaTriggers.init();
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.PARAMECIA)
            .item(SupaItems.FRUIT)
            .form(
               FruitForm.builder("base")
                  .ability(SupaAbilities.SPAR_CLAW)
                  .ability(SupaAbilities.STEEL_BODY)
                  .ability(SupaAbilities.BLADE_SKATES)
                  .ability(SupaAbilities.SPIRAL_HOLLOW)
                  .ability(SupaAbilities.SUPER_SLASH)
            )
            .energy(new EnergyRules(150, 12, 3.0, 8.0))
            .lootWeight(5)
            .theme(FruitTheme.sprites("supa_supa").colors(15398143, 13624063, 10275071, 16770976, 13624063, 12573951))
            .fx(FruitFx.DEFAULT)
            .ultimate(SupaAbilities.SUPER_SLASH)
            .damageType(BLADE_DAMAGE)
            .build()
      );
      SupaEvents.register(ctx.gameBus());
      ctx.modBus().addListener(SupaNetwork::register);
      ctx.registerClientConfig(ID, SupaClientConfig.SPEC);
      PhysicsCommand.registerTest("super_slash", SlashCommandTest::run);
   }

   @Override
   public void registerClient(IEventBus modBus) {
      SupaClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new SupaSupaData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(SupaGameTests.class, SupaSlashGameTests.class);
   }
}
