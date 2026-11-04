package com.devilfruits.fruits.yomi_yomi;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.command.DevilFruitCommand;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormCondition;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitType;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.kilo_kilo.weight.WeightCurves;
import com.devilfruits.fruits.yomi_yomi.client.YomiClient;
import com.devilfruits.fruits.yomi_yomi.datagen.YomiYomiData;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
import com.devilfruits.fruits.yomi_yomi.gametest.YomiGameTests;
import com.devilfruits.fruits.yomi_yomi.soul.SoulPhase;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import com.devilfruits.fruits.yomi_yomi.soul.YomiSoul;
import com.devilfruits.fruits.yomi_yomi.soulking.SoulKing;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

public final class YomiYomi implements FruitModule {
   public static final ResourceLocation ID = DevilFruits.id("yomi_yomi");
   public static final String BASE = "base";
   public static final String SOUL = "soul";
   public static final String SOUL_KING = "soul_king";
   public static final ResourceKey<DamageType> SOUL_DAMAGE = DFDamageTypes.key("yomi_yomi/soul");
   public static final ResourceKey<DamageType> ROAR_DAMAGE = DFDamageTypes.key("yomi_yomi/soul_king_roar");
   public static final double SKELETON_JUMP_BLOCKS = 3.0;
   public static final int POSSESSING = FormFlags.fruit(0);
   private static DevilFruit fruit;

   public static ResourceLocation id(String path) {
      return DevilFruits.id("yomi_yomi/" + path);
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

   @Nullable
   public static Vessel vessel(Player player) {
      return isUser(player) ? Vessel.byName(DFAttachments.data(player).homeName()) : null;
   }

   public static boolean isSoul(Player player) {
      return isUser(player) && "soul".equals(DFAttachments.data(player).formName());
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
      YomiItems.init();
      YomiEntities.init();
      YomiSounds.init();
      YomiTriggers.init();
      YomiSoul.init();
      SoulProjection.init();
      YomiFrost.init(ctx.modBus());
      ctx.modBus().addListener(YomiNetwork::register);
      double jumpBonus = WeightCurves.takeoffSpeed(3.0, 0.08) - 0.42;
      fruit = ctx.registerFruit(
         DevilFruit.builder(ID, DevilFruitType.PARAMECIA)
            .item(YomiItems.FRUIT)
            .form(FruitForm.builder("base"))
            .form(
               FruitForm.builder(Vessel.HUMAN.formName())
                  .ability(YomiAbilities.SOUL_PROJECTION)
                  .enterFx(YomiFx.QUIET)
                  .exitFx(YomiFx.QUIET)
                  .condition(vesselOnly(Vessel.HUMAN))
            )
            .form(
               FruitForm.builder(Vessel.CORPSE.formName())
                  .ability(YomiAbilities.AUBADE)
                  .ability(YomiAbilities.POLKA)
                  .ability(YomiAbilities.SOUL_PROJECTION)
                  .enterFx(YomiFx.QUIET)
                  .exitFx(YomiFx.QUIET)
                  .condition(vesselOnly(Vessel.CORPSE))
            )
            .form(
               FruitForm.builder(Vessel.SKELETON.formName())
                  .ability(YomiAbilities.AUBADE)
                  .ability(YomiAbilities.GAVOTTE)
                  .ability(YomiAbilities.FUBUKI_GIRI)
                  .ability(YomiAbilities.POLKA)
                  .ability(YomiAbilities.SOUL_PROJECTION)
                  .ability(YomiAbilities.SOUL_KING)
                  .attribute(Attributes.MOVEMENT_SPEED, 0.2, Operation.ADD_MULTIPLIED_BASE)
                  .attribute(Attributes.JUMP_STRENGTH, jumpBonus, Operation.ADD_VALUE)
                  .attribute(Attributes.SAFE_FALL_DISTANCE, 2.0, Operation.ADD_VALUE)
                  .enterFx(YomiFx.QUIET)
                  .exitFx(YomiFx.QUIET)
                  .renderer(id("skeleton_body"))
                  .condition(vesselOnly(Vessel.SKELETON))
            )
            .form(
               FruitForm.builder("soul")
                  .ability(YomiAbilities.SOUL_PROJECTION)
                  .ability(YomiAbilities.SOUL_DASH)
                  .ability(YomiAbilities.COLD_SOUL)
                  .flight()
                  .renderer(id("soul"))
                  .enterFx(YomiFx.SOUL_RISE)
                  .exitFx(YomiFx.QUIET)
                  .condition((player, data) -> SoulPhase.inSoul(player) ? null : Component.translatable("message.devilfruits.yomi_yomi.not_a_soul"))
            )
            .form(
               FruitForm.builder("soul_king")
                  .ability(YomiAbilities.EARTH_CRUSHING)
                  .ability(YomiAbilities.SOUL_KING_ROAR)
                  .ability(YomiAbilities.SOUL_KING)
                  .primaryAttack(YomiAbilities.KING_STRIKE)
                  .attribute(Attributes.MOVEMENT_SPEED, 0.2, Operation.ADD_MULTIPLIED_BASE)
                  .attribute(Attributes.JUMP_STRENGTH, jumpBonus, Operation.ADD_VALUE)
                  .attribute(Attributes.SAFE_FALL_DISTANCE, 2.0, Operation.ADD_VALUE)
                  .stats(9, 0, 0.0, 1800)
                  .timeLimit(60)
                  .freeAbilities()
                  .regen(0.0)
                  .castTime(30)
                  .windupAnimation(id("guitar_riff"))
                  .enterFx(YomiFx.KING_SUMMON)
                  .exitFx(YomiFx.KING_VANISH)
                  .renderer(id("skeleton_body"))
                  .condition(YomiYomi::soulKingCondition)
            )
            .energy(new EnergyRules(150, 10, 3.0, 8.0))
            .gachaTier(GachaTier.UNCOMMON)
            .lootWeight(6)
            .theme(FruitTheme.sprites("yomi_yomi").colors(8191802, 14221248, 8191802, 13172586, 14221248, 13172586))
            .ultimate(YomiAbilities.SOUL_KING)
            .damageType(SOUL_DAMAGE)
            .configUpdate(1, "forms.soul_king.castTicks", "forms.soul_king.freeAbilities", "forms.soul_king.regenMultiplier")
            .setting("soulSeconds", 180.0, 10.0, 3600.0, "Seconds the soul has after the first death to fly back and possess its body.")
            .setting("soulHeight", 30.0, 0.0, 200.0, "Blocks above the place of death the soul appears.")
            .setting("corpseHealth", 20.0, 1.0, 400.0, "Health of the body left on the ground; destroyed, the soul dissipates and the power is lost.")
            .setting("possessReach", 6.0, 1.0, 32.0, "Blocks from the body the soul may be to possess it.")
            .setting("villagerFearRadius", 10.0, 0.0, 64.0, "Villagers within this many blocks flee from a Living Corpse.")
            .setting("golemAggroRadius", 16.0, 0.0, 64.0, "Iron golems within this many blocks attack a Living Corpse.")
            .setting("waterRunSpeed", 0.65, 0.1, 1.5, "A Skeleton running on water goes this share of its sprint speed.")
            .setting("waterRunEnergy", 4.0, 0.0, 100.0, "Energy a second a Skeleton spends running on water.")
            .flag("giftCane", true, "Possession gives the user a Soul Cane if they have none.")
            .flag("freezeWater", true, "Fubuki Giri freezes surface water on its path into frosted ice for a while.")
            .setting("soulLeash", 64.0, 8.0, 256.0, "Blocks the projected soul may go from its kneeling body; beyond, a soft pull draws it back.")
            .setting("shellResistance", 0.5, 0.0, 1.0, "Share of a blow on the kneeling body its owner is spared.")
            .setting("secondWindMinutes", 20.0, 1.0, 600.0, "Minutes of game time between two Second Winds of a Skeleton.")
            .setting("secondWindRadius", 6.0, 0.0, 32.0, "Blocks the Second Wind's cold-flame shockwave throws things back.")
            .setting("kingAuraRadius", 4.0, 0.0, 16.0, "Enemies within this many blocks of a Soul King's user are slightly slowed.")
            .build()
      );
      SoulPhase.register(ctx.gameBus());
      SoulProjection.register(ctx.gameBus());
      SecondWind.register(ctx.gameBus());
      SoulKing.register(ctx.gameBus());
      YomiEvents.register(ctx.gameBus());
      Weakness.onWaterWhile(WaterRun::onWater);
      DevilFruitCommand.addNode(YomiCommand::node);
      FruitRules.onDeathIntercept(SoulPhase::interceptDeath);
      FruitRules.onPowerRemoved((player, f) -> {
         if (f == fruit) {
            SoulProjection.onPowerRemoved(player);
            SoulPhase.onPowerRemoved(player);
         }
      });
   }

   @Nullable
   private static Component soulKingCondition(Player player, PlayerFruitData data) {
      if (!Vessel.SKELETON.formName().equals(data.homeName())) {
         return Component.translatable("message.devilfruits.yomi_yomi.skeleton_only");
      } else {
         return !"soul_king".equals(data.formName()) && !SoulKing.holdsGuitar(player)
            ? Component.translatable("message.devilfruits.yomi_yomi.need_guitar")
            : null;
      }
   }

   private static FormCondition vesselOnly(Vessel vessel) {
      return (player, data) -> vessel.formName().equals(data.homeName()) ? null : Component.translatable("message.devilfruits.yomi_yomi.not_your_vessel");
   }

   @Override
   public void registerClient(IEventBus modBus) {
      YomiClient.init(modBus);
   }

   @Override
   public FruitDataContributor data() {
      return new YomiYomiData();
   }

   @Override
   public List<Class<?>> gameTests() {
      return List.of(YomiGameTests.class);
   }
}
