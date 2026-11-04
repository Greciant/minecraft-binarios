package com.devilfruits.fruits.kilo_kilo.gametest;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.physics.EntityMass;
import com.devilfruits.core.physics.PhysicsConfig;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.core.rules.DFGameRules;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.fruits.kilo_kilo.KiloAbilities;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.fruits.kilo_kilo.weight.BodyWeight;
import com.devilfruits.fruits.kilo_kilo.weight.HeavyBody;
import com.devilfruits.fruits.kilo_kilo.weight.WeightCurves;
import com.devilfruits.fruits.kilo_kilo.weight.WeightManager;
import com.devilfruits.registry.DFAttachments;
import com.mojang.authlib.GameProfile;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class KiloGameTests {
   private static final String PLATFORM = "platform";
   private static final WeightCurves.Tuning T = WeightCurves.Tuning.DEFAULT;

   private static FakePlayer user(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "kilo_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(KiloKilo.fruit());
      FruitRules.grantPower(player, KiloKilo.fruit(), false);
      return player;
   }

   private static void weigh(FakePlayer player, double kg) {
      BodyWeight w = KiloKilo.weight(player);
      w.setKg(kg);
      WeightManager.applyAttributes(player, kg, WeightManager.tuning());
   }

   private static void done(GameTestHelper helper, FakePlayer player) {
      AbilityManager.cancelAll(player);
      AbilityManager.forget(player.getUUID());
      HeavyBody.forget(player);
      FruitRules.removePower(player);
      WeightManager.tick(player);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(KiloKilo.fruit());
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void oneKiloStillFalls(GameTestHelper helper) {
      double g = WeightCurves.gravity(1.0, T);
      double hang = fallen(g, 200, false);
      double sink = fallen(g, 200, true);
      helper.assertTrue(hang == 0.0, "vanilla alone: 1 kg hangs in the air: " + hang);
      helper.assertTrue(sink > 3.0 && sink < WeightCurves.terminal(g) * 200.0, "1 kg sinks slowly: " + sink + " blocks in 10 s");
      helper.succeed();
   }

   private static double fallen(double gravity, int ticks, boolean hook) {
      double y = 0.0;
      double v = 0.0;

      for (int i = 0; i < ticks; i++) {
         if (hook) {
            v = WeightCurves.keepFalling(v);
         }

         if (Math.abs(v) < 0.003) {
            v = 0.0;
         }

         y -= v;
         v = (v - gravity) * 0.98;
      }

      return y;
   }

   @GameTest(
      template = "platform"
   )
   public static void weightCurvesAreMonotonicAndContinuous(GameTestHelper helper) {
      double prevG = 0.0;
      double prevSpeed = Double.MAX_VALUE;
      double prevJump = Double.MAX_VALUE;
      double prevStep = Double.MAX_VALUE;
      double prevResist = -1.0;
      double prevCap = Double.MAX_VALUE;
      double prevKg = 0.0;

      for (int i = 0; i <= 400; i++) {
         double kg = WeightCurves.kgAt(-1.0 + (double)i / 200.0);
         double g = WeightCurves.gravity(kg, T);
         double speed = WeightCurves.speedMultiplier(kg, T);
         double jump = WeightCurves.jumpHeight(kg, T);
         double step = WeightCurves.stepHeight(kg, T);
         double resist = WeightCurves.knockbackResistance(kg);
         double cap = WeightCurves.fallCap(kg, T);
         helper.assertTrue(kg > prevKg, "weights rise along the scale");
         helper.assertTrue(
            g >= prevG
               && speed <= prevSpeed + 1.0E-12
               && jump <= prevJump + 1.0E-9
               && step <= prevStep + 1.0E-12
               && resist >= prevResist
               && cap <= prevCap + 1.0E-12,
            "monotonic at " + kg + " kg"
         );
         if (i > 0) {
            helper.assertTrue(
               Math.abs(Math.log(g / prevG)) < 0.05
                  && Math.abs(speed - prevSpeed) < 0.02
                  && Math.abs(jump - prevJump) < 0.05 * Math.max(1.0, jump)
                  && Math.abs(step - prevStep) < 0.02
                  && Math.abs(resist - prevResist) < 0.08
                  && Math.abs(cap - prevCap) < 0.05,
               "continuous at " + kg + " kg"
            );
         }

         prevKg = kg;
         prevG = g;
         prevSpeed = speed;
         prevJump = jump;
         prevStep = step;
         prevResist = resist;
         prevCap = cap;
      }

      helper.assertTrue(
         Math.abs(WeightCurves.gravity(70.0, T) - 0.08) < 1.0E-9 && Math.abs(WeightCurves.jumpStrength(70.0, T) - 0.42) < 0.001,
         "70 kg is a vanilla body: " + WeightCurves.jumpStrength(70.0, T)
      );
      helper.assertTrue(Math.abs(WeightCurves.kgAt(WeightCurves.scale(1234.0)) - 1234.0) < 1.0E-6, "scale and kgAt are inverses");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void lightJumpPeaksAtFiftyBlocks(GameTestHelper helper) {
      double v = WeightCurves.jumpStrength(1.0, T);
      double g = WeightCurves.gravity(1.0, T);
      double apex = WeightCurves.apex(v, g);
      helper.assertTrue(Math.abs(apex - 50.0) / 50.0 < 0.03, "apex " + apex + " at takeoff " + v);
      helper.assertTrue(Math.abs(WeightCurves.terminal(g) - 0.1) < 1.0E-6, "floaty fall at 0.1 blocks/tick: " + WeightCurves.terminal(g));
      helper.assertTrue(v <= 32.0, "within the jump attribute's range");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void fallSpeedIsCapped(GameTestHelper helper) {
      double heavy = WeightCurves.fallSpeed(500.0, WeightCurves.gravity(10000.0, T), WeightCurves.fallCap(10000.0, T));
      helper.assertTrue(Math.abs(heavy - T.maxFallSpeed()) < 1.0E-9, "10 t falls at the cap: " + heavy);
      double heavyDrop = WeightCurves.fallDistanceFor(2.9, WeightCurves.gravity(10000.0, T), WeightCurves.fallCap(10000.0, T));
      double normalDrop = WeightCurves.fallDistanceFor(2.9, WeightCurves.gravity(70.0, T), WeightCurves.fallCap(70.0, T));
      helper.assertTrue(heavyDrop * 4.0 < normalDrop, "the heavier, the sooner fast: " + heavyDrop + " vs " + normalDrop);
      helper.assertTrue(
         WeightCurves.fallCap(10000.0, T) <= 3.9 && WeightCurves.fallSpeed(1000.0, WeightCurves.gravity(1.0, T), WeightCurves.fallCap(1.0, T)) < 0.11,
         "the light body drifts down at its terminal speed"
      );
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void noFallDamageAtAnyWeight(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));

      for (double kg : new double[]{1.0, 9.0, 70.0, 700.0, 10000.0}) {
         weigh(player, kg);
         helper.assertTrue(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) == 0.0, "no fall damage at " + kg + " kg");
      }

      weigh(player, 10000.0);
      helper.assertTrue(
         player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.999 && player.getAttributeValue(Attributes.STEP_HEIGHT) < 1.0E-6,
         "10 t: immune to knockback, no step"
      );
      weigh(player, 1.0);
      helper.assertTrue(
         Math.abs(player.getAttributeValue(Attributes.GRAVITY) - WeightCurves.gravity(1.0, T)) < 1.0E-9
            && player.getAttributeValue(Attributes.JUMP_STRENGTH) > 1.0,
         "1 kg: low gravity, a huge jump"
      );
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void rampTakesItsTime(GameTestHelper helper) {
      double rate = WeightCurves.rate(3.0);
      BodyWeight w = new BodyWeight();
      w.setKg(1.0);
      w.aim(2, 10000.0, 0.0);
      int ticks = 0;

      for (double prev = w.kg(); w.ramping() && ticks < 1000; ticks++) {
         w.advance(rate);
         helper.assertTrue(w.kg() > prev, "rising every tick");
         prev = w.kg();
      }

      helper.assertTrue(Math.abs(ticks - 60) <= 1, "1 kg to 10 t in 60 ticks: " + ticks);
      int half = WeightCurves.rampTicks(70.0, 1.0, rate);
      helper.assertTrue(half >= 27 && half <= 28, "70 kg to 1 kg in proportion: " + half);
      double fast = WeightCurves.rateBetween(70.0, 10000.0, 0.35);
      helper.assertTrue(WeightCurves.rampTicks(70.0, 10000.0, fast) == 7, "the Press switch takes 0.35 s");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void energyDrainAndReset(GameTestHelper helper) {
      helper.assertTrue(
         WeightCurves.drainPerSecond(70.0, T) == 0.0 && WeightCurves.drainPerSecond(80.0, T) == 0.0 && WeightCurves.drainPerSecond(60.0, T) == 0.0,
         "no drain near normal"
      );
      helper.assertTrue(
         WeightCurves.drainPerSecond(10000.0, T) > WeightCurves.drainPerSecond(1000.0, T)
            && WeightCurves.drainPerSecond(1000.0, T) > 0.0
            && WeightCurves.drainPerSecond(1.0, T) > WeightCurves.drainPerSecond(10.0, T),
         "drain grows with the log distance"
      );
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      AbilityManager.press(player, KiloAbilities.PRESS);
      helper.assertTrue(DFAttachments.data(player).isToggleActive(KiloAbilities.PRESS.id()), "the Press is on");
      float before = DFAttachments.data(player).energy();

      for (int i = 0; i < 70; i++) {
         WeightManager.tick(player);
      }

      BodyWeight w = KiloKilo.weight(player);
      helper.assertTrue(w.kg() == 10000.0 && DFAttachments.data(player).energy() < before, "ramped to 10 t, paying energy: " + w.kg());
      WeightManager.settle(player);
      helper.assertTrue(w.kg() == 70.0 && w.normal(), "settling resets to 70 kg");
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void kineticEnergyMapsToBlockDamage(GameTestHelper helper) {
      double e = WeightCurves.kineticEnergy(10000.0, 3.0);
      helper.assertTrue(Math.abs(e - 1.8E7) < 1.0, "10 t at 3 blocks/tick = 18 MJ");
      double stone = WeightCurves.blockCost(1.5, 6.0, 120000.0);
      double glass = WeightCurves.blockCost(0.3, 0.3, 120000.0);
      helper.assertTrue(glass < stone, "glass is cheaper than stone");
      double[] costs = new double[200];
      Arrays.fill(costs, stone);
      int heavy = WeightCurves.smash(e, costs, null);
      int light = WeightCurves.smash(WeightCurves.kineticEnergy(1000.0, 3.0), costs, null);
      int normal = WeightCurves.smash(WeightCurves.kineticEnergy(70.0, 1.0), costs, null);
      helper.assertTrue(heavy > 40 && light > 0 && light < heavy && normal == 0, "blocks smashed: 10 t " + heavy + ", 1 t " + light + ", 70 kg " + normal);
      helper.assertTrue(
         WeightCurves.impactRadius(e) > WeightCurves.impactRadius(e / 10.0) && WeightCurves.crushDamage(e, 60.0) == 60.0,
         "the shockwave scales, crushing is capped"
      );
      helper.succeed();
   }

   @GameTest(
      template = "platform",
      batch = "kilo_smash"
   )
   public static void punchThroughStopsWhenEnergyIsSpent(GameTestHelper helper) {
      for (int y = 1; y <= 3; y++) {
         helper.setBlock(2, y, 2, Blocks.STONE);
      }

      FakePlayer player = user(helper, new BlockPos(2, 4, 2));
      weigh(player, 10000.0);
      HeavyBody.Smash s = HeavyBody.smash(player, 10000.0, new Vec3(0.0, -1.0, 0.0), 0.5, 3.5, false);
      helper.assertTrue(s.broken() == 1 && s.stopped(), "one block, then stopped: " + s);
      helper.assertBlockPresent(Blocks.AIR, 2, 3, 2);
      helper.assertBlockPresent(Blocks.STONE, 2, 2, 2);
      helper.assertBlockPresent(Blocks.STONE, 2, 1, 2);
      helper.assertTrue(s.energyLeft() < WeightCurves.blockCost(1.5, 6.0, 120000.0) && s.energyLeft() > 0.0, "the rest of the energy: " + s.energyLeft());
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "kilo_smash"
   )
   public static void smashingStopsTenBlocksDeep(GameTestHelper helper) {
      for (int y = 1; y <= 14; y++) {
         helper.setBlock(2, y, 2, Blocks.DIRT);
      }

      FakePlayer player = user(helper, new BlockPos(2, 15, 2));
      weigh(player, 10000.0);
      HeavyBody.Smash s = HeavyBody.smash(player, 10000.0, new Vec3(0.0, -1.0, 0.0), 3.0, 16.0, false);
      helper.assertTrue(s.broken() == 10 && s.stopped(), "ten blocks, then the depth limit: " + s);
      helper.assertBlockPresent(Blocks.AIR, 2, 5, 2);
      helper.assertBlockPresent(Blocks.DIRT, 2, 4, 2);
      HeavyBody.Smash again = HeavyBody.smash(player, 10000.0, new Vec3(0.0, -1.0, 0.0), 3.0, 16.0, false);
      helper.assertTrue(again.broken() == 0 && again.energyLeft() <= s.energyLeft(), "no new energy, no deeper: " + again);
      helper.assertBlockPresent(Blocks.DIRT, 2, 1, 2);
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "kilo_smash"
   )
   public static void thinFloorGivesWay(GameTestHelper helper) {
      helper.assertTrue(
         WeightCurves.creakTicks(0.4, 50.0, 5000.0, 5000.0) > WeightCurves.creakTicks(0.4, 0.5, 5000.0, 5000.0)
            && WeightCurves.creakTicks(0.4, 0.5, 5000.0, 5000.0) > WeightCurves.creakTicks(0.4, 0.5, 10000.0, 5000.0),
         "creak time by hardness and weight"
      );
      helper.setBlock(1, 3, 2, Blocks.DIRT);
      helper.setBlock(3, 3, 2, Blocks.DIRT);
      helper.setBlock(3, 2, 2, Blocks.DIRT);
      FakePlayer player = user(helper, new BlockPos(3, 4, 2));
      weigh(player, 10000.0);

      for (int i = 0; i < 40; i++) {
         helper.assertTrue(HeavyBody.pressFloor(player, 10000.0) == 0, "a floor two blocks thick holds");
      }

      helper.assertBlockPresent(Blocks.DIRT, 3, 3, 2);
      BlockPos thin = helper.absolutePos(new BlockPos(1, 4, 2));
      player.moveTo((double)thin.getX() + 0.5, (double)thin.getY(), (double)thin.getZ() + 0.5, 0.0F, 0.0F);

      for (int i = 0; i < 40; i++) {
         helper.assertTrue(HeavyBody.pressFloor(player, 70.0) == 0, "70 kg does not break it");
      }

      helper.assertBlockPresent(Blocks.DIRT, 1, 3, 2);
      int need = WeightCurves.creakTicks(WeightManager.setting("thinFloorSeconds", 0.4), 0.5, 10000.0, WeightManager.setting("thinFloorMinKg", 5000.0));
      int gaveAt = -1;

      for (int i = 1; i <= 40 && gaveAt < 0; i++) {
         if (HeavyBody.pressFloor(player, 10000.0) > 0) {
            gaveAt = i;
         }
      }

      helper.assertTrue(gaveAt == need, "the thin floor gives way after its creak (" + need + " ticks): " + gaveAt);
      helper.assertBlockPresent(Blocks.AIR, 1, 3, 2);
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void massProviderFeedsStructureForces(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      double unit = (Double)PhysicsConfig.PLAYER_MASS.get();
      weigh(player, 10000.0);
      double heavy = StructureForces.bodyMass(player);
      helper.assertTrue(
         Math.abs(heavy - unit * 10000.0 / 70.0 * (Double)PhysicsConfig.MASS_SCALE.get()) < 1.0E-6 && EntityMass.immovable(player),
         "10 t: " + heavy + " mass units, immovable"
      );
      weigh(player, 1.0);
      helper.assertTrue(StructureForces.bodyMass(player) < unit * 0.05 && !EntityMass.immovable(player), "1 kg weighs almost nothing");
      FakePlayer other = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "kilo_bystander"));
      helper.assertTrue(
         Math.abs(StructureForces.bodyMass(other) - unit) < 1.0E-4 && Math.abs(EntityMass.kg(other) - 70.0) < 0.001, "everyone else weighs a player"
      );
      done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void keepFruitResetsTheWeightByDefault(GameTestHelper helper) {
      FakePlayer player = user(helper, new BlockPos(2, 1, 2));
      BooleanValue rule = (BooleanValue)helper.getLevel().getGameRules().getRule(DFGameRules.KEEP_FRUIT);
      boolean was = rule.get();
      rule.set(true, helper.getLevel().getServer());
      AbilityManager.press(player, KiloAbilities.ONE_KILO);

      for (int i = 0; i < 40; i++) {
         WeightManager.tick(player);
      }

      helper.assertTrue(KiloKilo.weight(player).kg() == 1.0, "1 kg reached");
      FruitRules.keepOnDeath(player);
      WeightManager.settle(player);
      helper.assertTrue(KiloKilo.isUser(player) && KiloKilo.weight(player).normal() && !WeightManager.persists(player), "power kept, weight back to 70 kg");
      rule.set(was, helper.getLevel().getServer());
      done(helper, player);
   }
}
