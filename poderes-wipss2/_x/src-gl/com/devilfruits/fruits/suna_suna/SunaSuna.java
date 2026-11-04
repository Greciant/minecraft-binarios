package com.devilfruits.fruits.suna_suna;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitFx;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.logia.CounterTrigger;
import com.devilfruits.core.logia.LogiaCounter;
import com.devilfruits.core.logia.LogiaEnvironment;
import com.devilfruits.core.logia.LogiaProfile;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.suna_suna.client.SunaClient;
import com.devilfruits.fruits.suna_suna.datagen.SunaData;
import com.devilfruits.fruits.suna_suna.gametest.SunaGameTests;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import com.devilfruits.registry.DFTags;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags.Biomes;

public final class SunaSuna implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("suna_suna");
   public static final ResourceLocation ELEMENT = DevilFruits.id("sand");
   public static final String BASE = "base";
   public static final String WET = "wet";
   public static final String SEASTONE = "seastone";
   public static final ResourceKey<DamageType> SAND_DAMAGE = DFDamageTypes.key("suna_suna/sand");
   public static final ResourceKey<DamageType> DEHYDRATION_DAMAGE = DFDamageTypes.key("suna_suna/dehydration");
   public static final ResourceKey<DamageType> QUICKSAND_DAMAGE = DFDamageTypes.key("suna_suna/quicksand");
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("suna_suna/" + path);
   }

   public static DevilFruit fruit() {
      return fruit;
   }

   public static boolean isUser(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) && ID.equals(DFAttachments.data(player).fruitId());
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   @Override
   public void register(FruitModuleContext ctx) {
      SunaItems.init();
      SunaBlocks.init();
      SunaSounds.init();
      SunaParticles.init();
      SunaEffects.init();
      SandTravel.register();
      SunaTriggers.init();
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.LOGIA)
            .item(SunaItems.FRUIT)
            .form(
               FruitForm.builder("base")
                  .ability(SunaAbilities.SABLES_PESADO)
                  .ability(SunaAbilities.DESERT_SPADA)
                  .ability(SunaAbilities.SAND_CLOUD)
                  .ability(SunaAbilities.BARJAN)
                  .ability(SunaAbilities.GROUND_SECCO)
                  .ability(SunaAbilities.DESERT_GIRASOLE)
                  .ability(SunaAbilities.DESERT_LA_SPADA)
                  .ability(SunaAbilities.DESERT_ENCIERRO)
                  .ability(SunaAbilities.SAND_TSUNAMI)
                  .ability(SunaAbilities.SABLES)
                  .ultimate(SunaAbilities.SABLES)
            )
            .logia(profile())
            .energy(new EnergyRules(140, 14, 3.0, 8.0))
            .configUpdate(
               1, "abilities.ground_secco.range", "abilities.desert_girasole.radius", "abilities.desert_encierro.range", "settings.desiccationBlocksPerCast"
            )
            .lootWeight(4)
            .theme(FruitTheme.sprites("suna_suna").colors(15915424, 15918003, 15254378, 16766282, 15918003, 16771248))
            .fx(FruitFx.DEFAULT.withAwaken(SunaFx.AWAKEN).withLevelUp(SunaFx.LEVEL_UP).withReincarnate(SunaFx.REINCARNATE))
            .damageType(SAND_DAMAGE)
            .flag(
               "desiccationChangesBlocks",
               true,
               "Drying techniques turn soil into sand, wither plants and dry up water (block destruction, mobGriefing and claims still apply)."
            )
            .setting("desiccationRestoreSeconds", 45.0, 1.0, 3600.0, "Seconds before dried land heals in TEMPORARY debris mode.")
            .setting(
               "desiccationBlocksPerCast",
               6000.0,
               0.0,
               16384.0,
               "Blocks one drying technique may change (replaces the global maxBlocksPerCast for drying: it only swaps blocks, it tears nothing out)."
            )
            .build()
      );
   }

   private static LogiaProfile.Builder profile() {
      return LogiaProfile.builder(ELEMENT)
         .counter(
            LogiaCounter.builder("wet")
               .trigger(CounterTrigger.Kind.IN_WATER)
               .trigger(CounterTrigger.Kind.RAIN)
               .trigger(CounterTrigger.Kind.SPLASH_WATER)
               .trigger(CounterTrigger.Kind.WATER_BUCKET)
               .trigger(CounterTrigger.damage(DFTags.LOGIA_WATER_DAMAGE))
               .action(LogiaCounter.Action.SUPPRESS_INTANGIBILITY, LogiaCounter.Action.FORCE_REFORM)
               .weaken(0.6F)
               .slow(0.15F)
               .linger(100)
               .icon(DevilFruits.id("logia/suna_suna/wet"))
         )
         .counter(
            LogiaCounter.builder("seastone")
               .trigger(CounterTrigger.Kind.SEASTONE_TOUCH)
               .trigger(CounterTrigger.seastoneNear(2.0))
               .action(LogiaCounter.Action.SUPPRESS_INTANGIBILITY, LogiaCounter.Action.FORCE_REFORM, LogiaCounter.Action.BLOCK_ABILITIES)
               .linger(10)
               .icon(DevilFruits.id("logia/suna_suna/seastone"))
         )
         .environment(new LogiaEnvironment(SunaTags.SAND_SOURCE, Biomes.IS_DRY, 1.5, Biomes.IS_WET, 0.5, true))
         .elementalBody(true, 3.0F);
   }

   @Override
   public void registerClient(IEventBus modBus) {
      SunaClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new SunaData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(SunaGameTests.class);
   }
}
