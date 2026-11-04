package com.devilfruits.fruits.gura_gura;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitFx;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.physics.PhysicsEvents;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.gura_gura.client.GuraGuraClient;
import com.devilfruits.fruits.gura_gura.datagen.GuraGuraData;
import com.devilfruits.fruits.gura_gura.gametest.GuraGameTests;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFCreativeTabs;
import com.devilfruits.registry.DFDamageTypes;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;

public final class GuraGura implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("gura_gura");
   public static final ResourceKey<DamageType> QUAKE_DAMAGE = DFDamageTypes.key("gura_gura/quake");
   public static final TagKey<Item> QUAKE_WEAPONS = TagKey.create(Registries.ITEM, id("quake_weapons"));
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("gura_gura/" + path);
   }

   public static ResourceLocation anim(String name) {
      return id(name);
   }

   public static float destruction(int mastery) {
      double max = fruit().config().setting("destructionAtMaxMastery");
      return (float)(1.0 + (max - 1.0) * (double)(Mth.clamp(mastery, 1, 10) - 1) / 9.0);
   }

   public static int blockBudget(float destruction) {
      return Math.round((float)((Integer)CoreConfig.MAX_BLOCKS_PER_CAST.get()).intValue() * destruction);
   }

   public static DevilFruit fruit() {
      return fruit;
   }

   public static boolean isUser(Player player) {
      PlayerFruitData data = DFAttachments.data(player);
      return ID.equals(data.fruitId());
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   @Override
   public void register(FruitModuleContext ctx) {
      GuraItems.init();
      GuraSounds.init();
      GuraParticles.init();
      GuraEntities.init();
      GuraTriggers.init();
      GuraLegacy.register();
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.PARAMECIA)
            .item(GuraItems.FRUIT)
            .form(
               FruitForm.builder("base")
                  .ability(GuraAbilities.KAISHIN)
                  .ability(GuraAbilities.GEKISHIN)
                  .ability(GuraAbilities.KABUTOWARI)
                  .ability(GuraAbilities.KAISHO)
                  .ability(GuraAbilities.SHINDO)
                  .ability(GuraAbilities.QUAKE_INFUSION)
                  .ability(GuraAbilities.SHIMA_YURASHI)
            )
            .setting(
               "destructionAtMaxMastery",
               3.5,
               1.0,
               10.0,
               "How much more terrain the techniques tear up at mastery 10 than at mastery 1 (rising evenly in between)."
            )
            .energy(new EnergyRules(200, 20, 2.5, 7.0))
            .configUpdate(1, "energy.baseEnergy", "energy.energyPerLevel")
            .lootWeight(10)
            .gachaTier(GachaTier.UNCOMMON)
            .theme(FruitTheme.sprites("gura_gura").colors(15984318, 12578815, 8382719, 16766282, 8382719, 12580095))
            .fx(FruitFx.DEFAULT.withAwaken(GuraFx.AWAKEN).withLevelUp(GuraFx.LEVEL_UP).withReincarnate(GuraFx.REINCARNATE))
            .ultimate(GuraAbilities.SHIMA_YURASHI)
            .damageType(QUAKE_DAMAGE)
            .build()
      );
      DFCreativeTabs.addExtra(GuraItems.BISENTO);
      GuraEvents.register(ctx.gameBus());
      PhysicsEvents.registerEmpowerment(p -> isUser(p) && DFAttachments.data(p).isToggleActive(GuraAbilities.SHINDO.id()) ? 2.5 : 0.0);
   }

   @Override
   public void registerClient(IEventBus modBus) {
      GuraGuraClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new GuraGuraData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(GuraGameTests.class);
   }
}
