package com.devilfruits.fruits.doru_doru;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitFx;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.core.world.TechniqueHeat;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.doru_doru.ability.ChampionForm;
import com.devilfruits.fruits.doru_doru.atelier.CastSlots;
import com.devilfruits.fruits.doru_doru.atelier.DoruNetwork;
import com.devilfruits.fruits.doru_doru.client.DoruClient;
import com.devilfruits.fruits.doru_doru.coating.WaxCoating;
import com.devilfruits.fruits.doru_doru.datagen.DoruDoruData;
import com.devilfruits.fruits.doru_doru.gametest.DoruGameTests;
import com.devilfruits.fruits.doru_doru.wax.WaxHeatService;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

public final class DoruDoru implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("doru_doru");
   public static final String BASE = "base";
   public static final String CHAMPION = "champion";
   public static final int SCULPTING = FormFlags.fruit(0);
   public static final ResourceKey<DamageType> WAX_DAMAGE = DFDamageTypes.key("doru_doru/wax");
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("doru_doru/" + path);
   }

   public static DevilFruit fruit() {
      return fruit;
   }

   public static boolean isUser(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) && ID.equals(DFAttachments.data(player).fruitId());
   }

   public static boolean canWax(Player player) {
      return isUser(player) && !player.isSpectator() && !Weakness.isSuppressed(player) && !WaxHeatService.tooHot(player.level());
   }

   @Nullable
   public static Component heatBlock(ServerPlayer player) {
      return WaxHeatService.tooHot(player.level()) ? Component.translatable("message.devilfruits.doru_doru.too_hot") : null;
   }

   public static double setting(String name) {
      return fruit.config().setting(name);
   }

   public static boolean flag(String name) {
      return fruit.config().flag(name);
   }

   public static double blockCost(int mastery) {
      double lo = setting("blockCostAtMastery2");
      double hi = setting("blockCostAtMastery10");
      double t = Math.max(0.0, Math.min(1.0, (double)(mastery - 2) / 8.0));
      return lo * Math.pow(hi / lo, t);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   @Override
   public void register(FruitModuleContext ctx) {
      TechniqueHeat.register(WaxHeatService::sear);
      DoruItems.init();
      DoruBlocks.init(ctx.modBus());
      DoruSounds.init();
      DoruTriggers.init();
      CastSlots.init(ctx.modBus());
      DoruEntities.init();
      WaxCoating.init(ctx.modBus());
      ChampionForm.init();
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.PARAMECIA)
            .item(DoruItems.FRUIT)
            .form(
               FruitForm.builder("base")
                  .ability(DoruAbilities.TOOLS)
                  .ability(DoruAbilities.ATELIER)
                  .ability(DoruAbilities.WALL)
                  .ability(DoruAbilities.LANCE)
                  .ability(DoruAbilities.SERVICE)
                  .ability(DoruAbilities.CHAMPION)
            )
            .form(
               FruitForm.builder("champion")
                  .ability(DoruAbilities.CHAMPION)
                  .attribute(Attributes.SCALE, 1.78, Operation.ADD_VALUE)
                  .attribute(Attributes.ATTACK_DAMAGE, 7.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ATTACK_KNOCKBACK, 1.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ARMOR, 8.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ARMOR_TOUGHNESS, 2.0, Operation.ADD_VALUE)
                  .attribute(Attributes.KNOCKBACK_RESISTANCE, 0.6, Operation.ADD_VALUE)
                  .attribute(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)
                  .attribute(Attributes.STEP_HEIGHT, 1.0, Operation.ADD_VALUE)
                  .attribute(Attributes.ENTITY_INTERACTION_RANGE, 2.5, Operation.ADD_VALUE)
                  .attribute(Attributes.MAX_ABSORPTION, 60.0, Operation.ADD_VALUE)
                  .attribute(Attributes.FALL_DAMAGE_MULTIPLIER, -1.0, Operation.ADD_MULTIPLIED_TOTAL)
                  .renderer(id("champion"))
                  .castTime(30)
                  .windupAnimation(id("champion_build"))
                  .enterAnimation(id("champion/transform"))
                  .exitAnimation(id("champion/revert"))
                  .enterFx(DoruFx.CHAMPION_RISE)
                  .exitFx(DoruFx.CHAMPION_MELT)
                  .condition((player, data) -> heatBlock(player))
                  .stats(10, 60, 1.0, 600)
            )
            .energy(new EnergyRules(150, 10, 3.0, 8.0))
            .lootWeight(6)
            .theme(FruitTheme.sprites("doru_doru").colors(16774108, 15918016, 15254138, 16766282, 15918016, 16769184))
            .fx(FruitFx.DEFAULT)
            .damageType(WAX_DAMAGE)
            .setting("maxWaxBlocksPerPlayer", 20000.0, 0.0, 1000000.0, "Most wax blocks one player's wax may occupy at once (performance and anti-grief).")
            .setting(
               "structureLifetimeMinutes",
               -1.0,
               -1.0,
               100000.0,
               "Minutes a sculpted wax block lasts before it melts on its own (-1 = forever, also after the owner died or lost the fruit)."
            )
            .setting("hotBiomeMeltMultiplier", 1.25, 1.0, 5.0, "Heat melts wax this much faster in hot biomes (desert, badlands, savanna; 1 = off).")
            .setting("meltRate", 0.18, 0.01, 2.0, "Melt progress per half second at full heat (a block next to lava starts melting after about 2 seconds).")
            .setting("maxHeatChecksPerTick", 96.0, 1.0, 4096.0, "Most warm wax blocks the heat service looks at in one tick (the rest wait a tick).")
            .setting("blockCostAtMastery2", 1.5, 0.0, 100.0, "Energy per block of wax poured at mastery 2.")
            .setting("blockCostAtMastery10", 0.06, 0.0, 100.0, "Energy per block of wax poured at mastery 10 (geometric in between).")
            .setting("honeycombEnergy", 8.0, 0.0, 200.0, "Energy a honeycomb gives back when a Doru Doru user takes it in (right-click).")
            .setting("candleEnergy", 5.0, 0.0, 200.0, "Energy a candle gives back when taken in.")
            .setting("lumpRefundShare", 0.8, 0.0, 1.0, "A wax lump gives back this share of what its wax cost the user (never a profit).")
            .flag("coatPlayers", true, "Wax Coating (Candle Service, Wax Lance) works on other players (PvP and team rules still apply).")
            .setting("coatingSlowPerStack", 0.06, 0.0, 0.2, "Movement slowed per stack of Wax Coating (share of the speed).")
            .setting("coatingSecondsMoving", 2.0, 0.1, 20.0, "Seconds of falling flakes per stack on a target running about.")
            .setting("coatingSecondsStill", 0.4, 0.05, 20.0, "Seconds of falling flakes per stack on a target standing still.")
            .setting("encaseSeconds", 3.0, 0.5, 30.0, "Seconds a mob with 10 stacks stays encased in wax (a player's shell holds until shaken off).")
            .setting("bossEncaseSeconds", 1.0, 0.0, 30.0, "Seconds a boss stays encased.")
            .setting("encaseImmunitySeconds", 6.0, 0.0, 60.0, "Seconds after breaking free during which a target cannot be encased again.")
            .setting("coatingDecayDelaySeconds", 3.0, 0.0, 60.0, "Seconds out of the flakes before a mob's stacks start to wear off (a player's never do).")
            .setting("coatingDecaySeconds", 1.5, 0.1, 60.0, "Seconds per stack wearing off a mob.")
            .setting("coatingShakesPerStack", 2.0, 1.0, 20.0, "Turns of strafing from side to side that shake one stack off a player.")
            .setting("encaseShakes", 12.0, 1.0, 100.0, "Turns of shaking (strafing side to side, attacks, jumps) that crack a player's shell.")
            .configUpdate(
               1,
               "forms.champion.attributes.generic_scale",
               "forms.champion.attributes.generic_step_height",
               "forms.champion.attributes.player_entity_interaction_range"
            )
            .setting(
               "championIntegrity",
               40.0,
               1.0,
               60.0,
               "Candle Champion: the suit's integrity (absorption points; blows take from it first, heat three times over)."
            )
            .build()
      );
      DoruEvents.register(ctx.gameBus());
      ctx.modBus().addListener(DoruNetwork::register);
   }

   @Override
   public void registerClient(IEventBus modBus) {
      DoruClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new DoruDoruData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(DoruGameTests.class);
   }
}
