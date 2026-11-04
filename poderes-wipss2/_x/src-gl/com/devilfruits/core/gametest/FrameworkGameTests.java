package com.devilfruits.core.gametest;

import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FormCollision;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.form.SafeLanding;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.gametest.fixture.DebugLogia;
import com.devilfruits.core.gametest.fixture.DebugZoan;
import com.devilfruits.core.rules.DFGameRules;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFItems;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class FrameworkGameTests {
   private static final String PLATFORM = "platform";

   private static DevilFruit zoan() {
      return DebugZoan.fruit();
   }

   private static FakePlayer user(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "framework_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      FruitRules.grantPower(player, zoan(), false);
      return player;
   }

   private static void done(GameTestHelper helper, FakePlayer... players) {
      for (FakePlayer p : players) {
         AbilityManager.cancelAll(p);
         AbilityManager.forget(p.getUUID());
         FruitRules.removePower(p);
      }

      for (DevilFruit fruit : FruitRegistry.all()) {
         WorldFruitData.get(helper.getLevel().getServer()).setFree(fruit);
      }

      helper.succeed();
   }

   private static double attribute(FakePlayer player, Holder<Attribute> attribute) {
      return player.getAttributeValue(attribute);
   }

   @GameTest(
      template = "platform"
   )
   public static void formSwitchAppliesAndRemovesAttributes(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerFruitData data = DFAttachments.data(player);
      double scale = attribute(player, Attributes.SCALE);
      double health = attribute(player, Attributes.MAX_HEALTH);
      helper.assertTrue(FormManager.request(player, "beast", FormManager.Reason.PLAYER) == FormManager.Result.SWITCHED, "switch to beast must succeed");
      helper.assertTrue("beast".equals(data.formName()), "form must be stored");
      helper.assertTrue(Math.abs(attribute(player, Attributes.SCALE) - (scale + 0.5)) < 1.0E-6, "scale modifier must apply");
      helper.assertTrue(Math.abs(attribute(player, Attributes.MAX_HEALTH) - (health + 10.0)) < 1.0E-6, "health modifier must apply");
      helper.assertTrue(data.isOnCooldown(PlayerFruitData.FORM_COOLDOWN, helper.getLevel().getGameTime()), "a voluntary switch starts the form cooldown");
      helper.assertTrue(FormManager.revert(player, FormManager.Reason.PLAYER) == FormManager.Result.SWITCHED, "revert must succeed");
      helper.assertTrue(data.formName().isEmpty(), "base form is stored as empty name");
      helper.assertTrue(Math.abs(attribute(player, Attributes.SCALE) - scale) < 1.0E-6, "scale must return to normal");
      helper.assertTrue(Math.abs(attribute(player, Attributes.MAX_HEALTH) - health) < 1.0E-6, "health must return to normal");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void abilityListsResolvePerForm(GameTestHelper helper) {
      FruitForm base = zoan().baseForm();
      FruitForm beast = zoan().form("beast");
      FruitForm winged = zoan().form("winged");
      helper.assertTrue(beast != null && winged != null, "fixture forms must exist");
      List<String> baseIds = base.abilities().stream().map(e -> e.ability().name()).toList();
      List<String> beastIds = beast.abilities().stream().map(e -> e.ability().name()).toList();
      List<String> wingedIds = winged.abilities().stream().map(e -> e.ability().name()).toList();
      helper.assertTrue(baseIds.equals(List.of("strike", "guard", "transform")), "base list was " + baseIds);
      helper.assertTrue(beastIds.equals(List.of("strike", "guard", "transform", "breath", "flurry")), "inherited + added list was " + beastIds);
      helper.assertTrue(wingedIds.equals(List.of("strike", "transform")), "remove must drop guard, was " + wingedIds);
      FormAbility baseStrike = base.find(DebugZoan.STRIKE.id());
      FormAbility beastStrike = beast.find(DebugZoan.STRIKE.id());
      helper.assertTrue(baseStrike != null && baseStrike.modifiers().isDefault(), "base strike has default modifiers");
      helper.assertTrue(
         beastStrike != null && "beast".equals(beastStrike.modifiers().variant()) && beastStrike.modifiers().damage() == 2.0F,
         "beast overrides strike with a stronger variant"
      );
      helper.assertTrue(winged.canFly() && winged.dimensions() != null, "winged form flies and has a hitbox");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void overriddenAbilityUsesVariantAndSharesCooldown(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      DebugZoan.STRIKE.log.clear();
      helper.assertTrue(AbilityManager.press(player, DebugZoan.STRIKE) == CastResult.SUCCESS, "base strike must cast");
      FormManager.request(player, "beast", FormManager.Reason.COMMAND);
      helper.assertTrue(AbilityManager.press(player, DebugZoan.STRIKE) == CastResult.COOLDOWN, "cooldown is shared between forms");
      DFAttachments.data(player).clearCooldowns();
      helper.assertTrue(AbilityManager.press(player, DebugZoan.STRIKE) == CastResult.SUCCESS, "beast strike must cast");
      String base = DebugZoan.STRIKE.log.get(0);
      String beast = DebugZoan.STRIKE.log.get(1);
      float baseDamage = Float.parseFloat(base.substring(base.indexOf(58) + 1));
      float beastDamage = Float.parseFloat(beast.substring(beast.indexOf(58) + 1));
      helper.assertTrue(base.startsWith(":") && beast.startsWith("beast:"), "variants must reach the ability: " + DebugZoan.STRIKE.log);
      helper.assertTrue((double)Math.abs(beastDamage - 2.0F * baseDamage) < 1.0E-4, "beast variant doubles damage: " + DebugZoan.STRIKE.log);
      helper.assertTrue(AbilityManager.press(player, DebugZoan.BREATH) == CastResult.CHANNELLING, "beast-only ability castable in beast form");
      FormManager.revert(player, FormManager.Reason.COMMAND);
      helper.assertTrue(DebugZoan.BREATH.log.contains("interrupted"), "leaving the form ends abilities it does not list");
      helper.assertTrue(AbilityManager.press(player, DebugZoan.BREATH) == CastResult.NOT_IN_FORM, "beast-only ability unavailable in base form");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void formRequirementsAreValidated(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerFruitData data = DFAttachments.data(player);
      helper.assertTrue(FormManager.request(player, "winged", FormManager.Reason.PLAYER) == FormManager.Result.LOCKED, "winged needs mastery 3");
      data.setEnergy(1.0F);
      helper.assertTrue(FormManager.request(player, "beast", FormManager.Reason.PLAYER) == FormManager.Result.NO_ENERGY, "entering costs energy");
      helper.assertTrue(FormManager.request(player, "nope", FormManager.Reason.PLAYER) == FormManager.Result.UNKNOWN_FORM, "unknown forms are refused");
      helper.assertTrue(FormManager.request(player, "winged", FormManager.Reason.COMMAND) == FormManager.Result.SWITCHED, "commands bypass the checks");
      helper.assertTrue(attribute(player, NeoForgeMod.CREATIVE_FLIGHT) > 0.0, "winged form grants flight");
      FormManager.revert(player, FormManager.Reason.COMMAND);
      helper.assertTrue(attribute(player, NeoForgeMod.CREATIVE_FLIGHT) <= 0.0, "flight is removed again");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void formAutoRevertsOnDepletionAndWater(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(1, 1, 1));
      PlayerFruitData data = DFAttachments.data(player);
      FormManager.request(player, "beast", FormManager.Reason.COMMAND);
      data.setEnergy(0.1F);
      CoreEvents.tickServer(player, data);
      CoreEvents.tickServer(player, data);
      helper.assertTrue(data.formName().isEmpty(), "draining to zero reverts to the base form");
      FormManager.request(player, "beast", FormManager.Reason.COMMAND);
      data.setEnergy(50.0F);
      helper.setBlock(new BlockPos(1, 1, 1), Blocks.WATER);
      helper.setBlock(new BlockPos(1, 2, 1), Blocks.WATER);
      CoreEvents.tickServer(player, data);
      helper.assertTrue(data.formName().isEmpty(), "deep water reverts to the base form");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void everyCastTypeWorks(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerFruitData data = DFAttachments.data(player);
      DebugZoan.GUARD.log.clear();
      DebugZoan.FLURRY.log.clear();
      DebugZoan.BREATH.log.clear();
      helper.assertTrue(AbilityManager.press(player, DebugZoan.GUARD) == CastResult.TOGGLED, "toggle on");
      helper.assertTrue(data.isToggleActive(DebugZoan.GUARD.id()), "toggle must be active");
      float before = data.energy();
      CoreEvents.tickServer(player, data);
      helper.assertTrue(data.energy() < before, "toggle drains energy");
      helper.assertTrue(AbilityManager.press(player, DebugZoan.GUARD) == CastResult.TOGGLED, "toggle off");
      helper.assertTrue(
         !data.isToggleActive(DebugZoan.GUARD.id()) && data.isOnCooldown(DebugZoan.GUARD.id(), helper.getLevel().getGameTime()),
         "toggle off starts the cooldown"
      );
      helper.assertTrue(AbilityManager.press(player, DebugZoan.TRANSFORM) == CastResult.SUCCESS, "form switch ability transforms");
      helper.assertTrue("beast".equals(data.formName()), "now in beast form");
      helper.assertTrue(AbilityManager.press(player, DebugZoan.BREATH) == CastResult.CHANNELLING, "held ability channels");
      CoreEvents.tickServer(player, data);
      AbilityManager.release(player, DebugZoan.BREATH);
      helper.assertTrue(DebugZoan.BREATH.log.equals(List.of("start", "tick", "end")), "channel lifecycle was " + DebugZoan.BREATH.log);
      helper.assertTrue(data.isOnCooldown(DebugZoan.BREATH.id(), helper.getLevel().getGameTime()), "channel end starts the cooldown");
      helper.assertTrue(AbilityManager.press(player, DebugZoan.FLURRY) == CastResult.COMBO_STEP, "combo step 1");
      helper.assertTrue(AbilityManager.press(player, DebugZoan.FLURRY) == CastResult.COMBO_STEP, "combo step 2");
      helper.assertTrue(AbilityManager.press(player, DebugZoan.FLURRY) == CastResult.SUCCESS, "combo finisher");
      helper.assertTrue(DebugZoan.FLURRY.log.equals(List.of("step0", "step1", "step2")), "combo steps were " + DebugZoan.FLURRY.log);
      helper.assertTrue(AbilityManager.press(player, DebugZoan.FLURRY) == CastResult.COOLDOWN, "combo cooldown after the finisher");
      helper.assertTrue(AbilityManager.press(player, DebugZoan.TRANSFORM) == CastResult.SUCCESS, "pressing the form switch again reverts (always free)");
      helper.assertTrue(data.formName().isEmpty(), "back in the base form");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void oneFruitPerPlayerAcrossFruits(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));

      for (DevilFruit other : FruitRegistry.all()) {
         if (other != zoan()) {
            FruitRules.EatOutcome outcome = FruitRules.eat(player, other, ItemStack.EMPTY);
            helper.assertTrue(outcome == FruitRules.EatOutcome.KILLED, "a second, different fruit must kill, was " + outcome);
            break;
         }
      }

      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void worldStateIsPerFruit(GameTestHelper helper) {
      WorldFruitData world = WorldFruitData.get(helper.getLevel().getServer());
      DevilFruit other = FruitRegistry.all().stream().filter(f -> f != zoan()).findFirst().orElseThrow();
      world.setFree(other);
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      helper.assertTrue(world.state(zoan()) == WorldFruitData.State.EATEN, "eaten fruit is tracked");
      helper.assertTrue(world.state(other) == WorldFruitData.State.FREE, "other fruits stay free");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void seastoneSuppressesThePowers(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)DFItems.SEASTONE_INGOT.get()));
      helper.assertTrue(Weakness.isSuppressed(player), "holding seastone suppresses the powers");
      helper.assertTrue(AbilityManager.press(player, DebugZoan.STRIKE) == CastResult.SUPPRESSED, "casting is blocked by seastone");
      player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      helper.assertTrue(!Weakness.isSuppressed(player), "dropping it lifts the suppression");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void playerDataSurvivesSerialization(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerFruitData data = DFAttachments.data(player);
      FormManager.request(player, "beast", FormManager.Reason.COMMAND);
      AbilityManager.press(player, DebugZoan.GUARD);
      data.startCooldown(DebugZoan.STRIKE.id(), 1234L, 60);
      data.setMasteryXp(42.5F);
      data.setSelected(3);
      DataResult<Tag> encoded = PlayerFruitData.CODEC.encodeStart(NbtOps.INSTANCE, data);
      Tag tag = (Tag)encoded.getOrThrow();
      PlayerFruitData decoded = (PlayerFruitData)PlayerFruitData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
      helper.assertTrue(decoded.sameState(data), "round trip must preserve the state: " + tag);
      helper.assertTrue(DebugZoan.ID.equals(decoded.fruitId()) && "beast".equals(decoded.formName()), "fruit and form must survive");
      helper.assertTrue(decoded.isToggleActive(DebugZoan.GUARD.id()), "toggles must survive");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void formCastTimeDelaysTheSwitch(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerFruitData data = DFAttachments.data(player);
      FruitForm giant = zoan().form("giant");
      helper.assertTrue(FormManager.check(player, "giant") == FormManager.Result.SWITCHED, "dry run allows the switch");
      helper.assertTrue(FormManager.request(player, "giant", FormManager.Reason.PLAYER) == FormManager.Result.WINDUP, "a cast-time form winds up");
      helper.assertTrue(data.formName().isEmpty() && FormManager.windupTarget(player) == giant, "the form only changes after the wind-up");
      helper.assertTrue(FormManager.request(player, "beast", FormManager.Reason.PLAYER) == FormManager.Result.BUSY, "other switches wait");
      helper.assertTrue(AbilityManager.press(player, DebugZoan.STRIKE) == CastResult.BUSY, "no casting while winding up");
      helper.runAfterDelay(11L, () -> {
         CoreEvents.tickServer(player, data);
         helper.assertTrue("giant".equals(data.formName()), "the wind-up completes the switch, form was '" + data.formName() + "'");
         helper.assertTrue(player.getForcedPose() == Pose.STANDING, "giant locks the pose");
         FormManager.revert(player, FormManager.Reason.COMMAND);
         helper.assertTrue(player.getForcedPose() == null, "the base form releases the pose");
         data.clearCooldowns();
         helper.assertTrue(FormManager.request(player, "giant", FormManager.Reason.PLAYER) == FormManager.Result.WINDUP, "second wind-up");
         helper.assertTrue(FormManager.revert(player, FormManager.Reason.PLAYER) == FormManager.Result.CANCELLED, "the revert key cancels it");
         helper.assertTrue(FormManager.windupTarget(player) == null && data.formName().isEmpty(), "nothing pending after cancelling");
         helper.assertTrue(AbilityManager.press(player, DebugZoan.STRIKE) == CastResult.SUCCESS, "casting works again");
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void formRegenMultiplierScalesRegeneration(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerFruitData data = DFAttachments.data(player);
      data.setEnergy(0.0F);

      for (int i = 0; i < 20; i++) {
         CoreEvents.tickServer(player, data);
      }

      float base = data.energy();
      FormManager.request(player, "giant", FormManager.Reason.COMMAND);
      data.setEnergy(0.0F);

      for (int i = 0; i < 20; i++) {
         CoreEvents.tickServer(player, data);
      }

      float giant = data.energy();
      helper.assertTrue(base > 0.0F && Math.abs(giant - base * 0.5F) < 0.05F, "giant regenerates at half speed: base " + base + ", giant " + giant);
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void formUltimateFallsBackToTheFruit(GameTestHelper helper) {
      helper.assertTrue(zoan().form("giant").ultimate() == DebugZoan.STRIKE, "a form's own ultimate wins");
      helper.assertTrue(zoan().ultimate() == null && zoan().baseForm().ultimate() == null, "no fruit ultimate, no form ultimate");
      helper.assertTrue(
         zoan().form("giant").cameraDistance() == 8.0F && zoan().form("giant").id().getPath().equals("debug_zoan/form/giant"), "camera distance and form id"
      );
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void boostDrainsOnlyInBoostableForms(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerFruitData data = DFAttachments.data(player);
      FormManager.setBoosting(player, true);
      helper.assertTrue(!data.hasFlag(1), "the base form cannot boost");
      FormManager.request(player, "winged", FormManager.Reason.COMMAND);
      data.setEnergy(100.0F);

      for (int i = 0; i < 20; i++) {
         FormManager.tick(player, data);
      }

      float plain = 100.0F - data.energy();
      FormManager.setBoosting(player, true);
      helper.assertTrue(data.hasFlag(1) && FormCollision.passesThroughLeaves(player), "winged boosts and passes leaves");
      data.setEnergy(100.0F);

      for (int i = 0; i < 20; i++) {
         FormManager.tick(player, data);
      }

      float boosted = 100.0F - data.energy();
      helper.assertTrue(Math.abs(plain - 4.0F) < 0.05F && Math.abs(boosted - 7.0F) < 0.05F, "drain 4/s, boosted 7/s: " + plain + " / " + boosted);
      FormManager.revert(player, FormManager.Reason.COMMAND);
      helper.assertTrue(data.flags() == 0, "flags are cleared on every form change");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void flyingFormEndingMidAirLandsSafely(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 3, 2));
      PlayerFruitData data = DFAttachments.data(player);
      FormManager.request(player, "winged", FormManager.Reason.COMMAND);
      player.setOnGround(false);
      data.setEnergy(0.05F);
      FormManager.tick(player, data);
      helper.assertTrue(data.formName().isEmpty(), "energy ran out");
      helper.assertTrue(SafeLanding.isActive(player), "a mid-air revert grants a safe landing");
      float health = player.getHealth();
      boolean hurt = player.causeFallDamage(20.0F, 1.0F, player.damageSources().fall());
      helper.assertTrue(!hurt && player.getHealth() == health && !SafeLanding.isActive(player), "the landing is free and consumes the grant");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void gachaTierComesFromTheFruitType(GameTestHelper helper) {
      helper.assertTrue(zoan().defaultGachaTier() == GachaTier.LEGENDARY, "the fixture is a Mythical Zoan: legendary");
      helper.assertTrue(FruitRegistry.get(DebugLogia.ID).defaultGachaTier() == GachaTier.RARE, "a Logia is rare");

      for (DevilFruit fruit : FruitRegistry.all()) {
         helper.assertTrue(fruit.config().gachaTier() == fruit.defaultGachaTier(), fruit.id() + ": the config starts at the definition's tier");
      }

      helper.succeed();
   }

   private static void die(GameTestHelper helper, FakePlayer player, DamageSource source, boolean keepFruit) {
      BooleanValue rule = (BooleanValue)helper.getLevel().getGameRules().getRule(DFGameRules.KEEP_FRUIT);
      boolean old = rule.get();
      rule.set(keepFruit, helper.getLevel().getServer());

      try {
         NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, source));
      } finally {
         rule.set(old, helper.getLevel().getServer());
      }
   }

   private static int fruitItems(GameTestHelper helper) {
      List<ItemEntity> items = helper.getLevel()
         .getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(16.0), e -> e.getItem().has((DataComponentType)DFDataComponents.FRUIT_INSTANCE.get()));
      items.forEach(Entity::discard);
      return items.size();
   }

   @GameTest(
      template = "platform"
   )
   public static void keepFruitKeepsPowerThroughDeath(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      PlayerFruitData data = DFAttachments.data(player);
      Mastery.setLevel(player, data, 3, false);
      helper.assertTrue(FormManager.request(player, "beast", FormManager.Reason.COMMAND) == FormManager.Result.SWITCHED, "switch to beast");
      data.setEnergy(1.0F);
      data.startCooldown(DebugZoan.STRIKE.id(), helper.getLevel().getGameTime(), 400);
      die(helper, player, player.damageSources().genericKill(), true);
      helper.assertTrue(data.hasFruit() && zoan().id().equals(data.fruitId()), "keepFruit keeps the power");
      helper.assertTrue(data.mastery() == 3, "keepFruit keeps the mastery, was " + data.mastery());
      helper.assertTrue(data.formName().isEmpty(), "back to the base form, was " + data.formName());
      helper.assertTrue(data.energy() == (float)Mastery.maxEnergy(zoan(), 3), "energy starts over full, was " + data.energy());
      helper.assertTrue(data.cooldowns().isEmpty(), "cooldowns reset");
      if ((Boolean)CoreConfig.ONE_PER_WORLD.get()) {
         WorldFruitData world = WorldFruitData.get(helper.getLevel().getServer());
         helper.assertTrue(
            world.state(zoan()) == WorldFruitData.State.EATEN && world.owner(zoan()).filter(player.getUUID()::equals).isPresent(),
            "the fruit stays this player's in the one-per-world record"
         );
      }

      helper.assertTrue(fruitItems(helper) == 0, "no fruit appears in the world");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void keepFruitOffLosesPowerAsBefore(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      die(helper, player, player.damageSources().genericKill(), false);
      PlayerFruitData data = DFAttachments.data(player);
      helper.assertTrue(data.hasFruit() != (Boolean)CoreConfig.LOSE_ON_DEATH.get(), "keepFruit=false follows loseOnDeath");
      if ((Boolean)CoreConfig.LOSE_ON_DEATH.get() && (Boolean)CoreConfig.ONE_PER_WORLD.get()) {
         helper.assertTrue(
            WorldFruitData.get(helper.getLevel().getServer()).owner(zoan()).filter(player.getUUID()::equals).isEmpty(),
            "the dead user no longer owns the fruit"
         );
      }

      helper.assertTrue(fruitItems(helper) <= 1, "at most the one reincarnated fruit appears");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void keepFruitNeverSavesASecondFruitDeath(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      die(helper, player, DFDamageTypes.secondFruit(helper.getLevel()), true);
      helper.assertTrue(DFAttachments.data(player).hasFruit() != (Boolean)CoreConfig.LOSE_ON_DEATH.get(), "a second fruit's death is handled as before");
      helper.assertTrue(fruitItems(helper) <= 1, "at most the one reincarnated fruit appears");
      done(helper, player);
   }
}
