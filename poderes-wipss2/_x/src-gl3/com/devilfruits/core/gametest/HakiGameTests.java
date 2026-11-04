package com.devilfruits.core.gametest;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gametest.fixture.DebugLogia;
import com.devilfruits.core.haki.HakiClash;
import com.devilfruits.core.haki.HakiCombat;
import com.devilfruits.core.haki.HakiFormParts;
import com.devilfruits.core.haki.HakiManager;
import com.devilfruits.core.haki.HakiParts;
import com.devilfruits.core.haki.HakiTier;
import com.devilfruits.core.haki.HakiTraining;
import com.devilfruits.core.haki.PlayerHakiData;
import com.devilfruits.core.logia.CounterTrigger;
import com.devilfruits.core.logia.HitRule;
import com.devilfruits.core.logia.LogiaBypassSource;
import com.devilfruits.core.logia.LogiaDamage;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFItems;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class HakiGameTests {
   private static final String PLATFORM = "platform";

   private static FakePlayer fighter(GameTestHelper helper, BlockPos relative, int level) {
      FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "haki_test")) {
         public boolean isInvulnerableTo(DamageSource source) {
            return false;
         }
      };
      player.setGameMode(GameType.SURVIVAL);
      ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, player, 0, "spawnInvulnerableTime");
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      if (level > 0) {
         HakiManager.unlock(player);
         HakiManager.setLevel(player, level);
      }

      return player;
   }

   private static void done(GameTestHelper helper, FakePlayer... players) {
      for (FakePlayer p : players) {
         HakiManager.reset(p);
         LogiaManager.forget(p);
         FruitRules.removePower(p);
      }

      for (DevilFruit fruit : FruitRegistry.all()) {
         WorldFruitData.get(helper.getLevel().getServer()).setFree(fruit);
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform",
      batch = "haki"
   )
   public static void fistKillsTeachWeaponKillsDoNot(GameTestHelper helper) {
      FakePlayer player = fighter(helper, new BlockPos(2, 2, 2), 1);
      Zombie first = (Zombie)helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 3));
      first.hurt(player.damageSources().playerAttack(player), 1000.0F);
      float afterFist = HakiManager.data(player).xp();
      helper.assertTrue(afterFist > 0.0F, "a bare-fist kill teaches Haki: " + afterFist);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
      Zombie second = (Zombie)helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
      second.hurt(player.damageSources().playerAttack(player), 1000.0F);
      helper.assertTrue(HakiManager.data(player).xp() == afterFist, "a sword kill teaches nothing");
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "haki"
   )
   public static void repeatingTheSameOpponentTeachesLess(GameTestHelper helper) {
      FakePlayer player = fighter(helper, new BlockPos(2, 2, 2), 1);
      Zombie zombie = (Zombie)helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 3));
      DamageSource fist = player.damageSources().playerAttack(player);
      long now = helper.getLevel().getGameTime();
      float a = HakiTraining.killXp(player, zombie, fist, now);
      float b = HakiTraining.killXp(player, zombie, fist, now + 20L);
      helper.assertTrue(a > 0.0F && b < a, "the second zombie in a row teaches less: " + a + " then " + b);
      Chicken chicken = (Chicken)helper.spawnWithNoFreeWill(EntityType.CHICKEN, new BlockPos(1, 2, 3));
      helper.assertTrue(HakiTraining.killXp(player, chicken, fist, now) == 0.0F, "a chicken teaches nothing");
      zombie.discard();
      chicken.discard();
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "haki"
   )
   public static void levelsOpenPartsAndRyuo(GameTestHelper helper) {
      FakePlayer player = fighter(helper, new BlockPos(2, 2, 2), 1);
      HakiManager.addXp(player, HakiManager.xpToNext(1));
      helper.assertTrue(HakiManager.level(player) == 2, "XP for level 1 makes level 2");
      helper.assertTrue((HakiParts.allowed(HakiTier.of(3)) & 12) == 0, "a novice coats no arms");
      helper.assertTrue((HakiParts.allowed(HakiTier.of(4)) & 12) == 12, "an adept coats the arms");
      helper.assertTrue(!HakiTier.of(6).ryuo() && HakiTier.of(7).ryuo(), "Ryuo opens at level 7");
      helper.assertTrue(HakiManager.coatOn(player), "the coat goes on");
      helper.assertTrue(HakiManager.mask(player) == 259, "a novice coats the fists and the weapon");
      HakiManager.select(player, 511);
      helper.assertTrue(HakiManager.mask(player) == 259, "and no more, whatever is picked");
      HakiManager.setLevel(player, 7);
      HakiManager.select(player, 511);
      helper.assertTrue(HakiManager.mask(player) == 511, "an expert hardens the whole body");
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "haki"
   )
   public static void coatedBlowsReachLogiaBodies(GameTestHelper helper) {
      FakePlayer victim = fighter(helper, new BlockPos(2, 2, 3), 0);
      FruitRules.grantPower(victim, DebugLogia.fruit(), false);
      FakePlayer attacker = fighter(helper, new BlockPos(2, 2, 1), 1);
      DamageSource fist = attacker.damageSources().playerAttack(attacker);
      helper.assertTrue(LogiaDamage.decide(victim, fist).rule() == HitRule.PASS, "a bare fist passes through");
      HakiManager.coatOn(attacker);
      LogiaDamage.Decision low = LogiaDamage.decide(victim, fist);
      helper.assertTrue(
         low.bypass() == LogiaBypassSource.HAKI && low.rule() == HitRule.PARTIAL && low.factor() < 1.0F,
         "a novice's coated fist reaches part of the way: " + low
      );
      HakiManager.setLevel(attacker, 4);
      LogiaDamage.Decision full = LogiaDamage.decide(victim, fist);
      helper.assertTrue(full.bypass() == LogiaBypassSource.HAKI && full.rule() == HitRule.CONNECT && full.factor() == 1.0F, "an adept's lands in full");
      done(helper, attacker, victim);
   }

   @GameTest(
      template = "platform",
      batch = "haki"
   )
   public static void hakiIgnoresDevilFruitWeaknesses(GameTestHelper helper) {
      FakePlayer player = fighter(helper, new BlockPos(2, 2, 2), 4);
      FruitRules.grantPower(player, DebugLogia.fruit(), false);
      LogiaManager.trigger(player, CounterTrigger.Kind.WATER_BUCKET, null);
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack((ItemLike)DFItems.SEASTONE_INGOT.get()));
      PlayerFruitData data = DFAttachments.data(player);

      for (int i = 0; i < 5; i++) {
         CoreEvents.tickServer(player, data);
      }

      helper.assertTrue(HakiManager.coatOn(player), "wet and holding seastone, the coat still goes on");

      for (int i = 0; i < 20; i++) {
         CoreEvents.tickServer(player, data);
         HakiManager.tick(player);
      }

      helper.assertTrue(HakiManager.mask(player) != 0, "and stays on");
      helper.assertTrue(HakiCombat.coatedStrike(player.damageSources().playerAttack(player)), "its blows are coated");
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "haki"
   )
   public static void aSecondScrollTeaches(GameTestHelper helper) {
      FakePlayer player = fighter(helper, new BlockPos(2, 2, 2), 0);
      ItemStack scroll = new ItemStack((ItemLike)DFItems.HAKI_SCROLL.get(), 2);
      player.setItemInHand(InteractionHand.MAIN_HAND, scroll);
      scroll.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(HakiManager.unlocked(player) && HakiManager.level(player) == 1, "the first scroll awakens it");
      scroll.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(HakiManager.level(player) > 1 || HakiManager.data(player).xp() > 0.0F, "the second one teaches");
      helper.assertTrue(scroll.isEmpty(), "both read");
      done(helper, player);
   }

   @GameTest(
      template = "platform",
      batch = "haki"
   )
   public static void hakiIsSavedAndSurvivesDeath(GameTestHelper helper) {
      FakePlayer player = fighter(helper, new BlockPos(2, 2, 2), 6);
      HakiManager.addXp(player, 37.0F);
      PlayerHakiData data = HakiManager.data(player);
      PlayerHakiData copy = (PlayerHakiData)PlayerHakiData.CODEC
         .parse(JsonOps.INSTANCE, (JsonElement)PlayerHakiData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow())
         .getOrThrow();
      helper.assertTrue(copy.armament() && copy.level() == 6 && copy.xp() == data.xp(), "saved and loaded the same");
      FakePlayer reborn = fighter(helper, new BlockPos(2, 2, 2), 0);
      NeoForge.EVENT_BUS.post(new Clone(reborn, player, true));
      helper.assertTrue(HakiManager.level(reborn) == 6, "kept through death");
      done(helper, player, reborn);
   }

   @GameTest(
      template = "platform",
      batch = "haki"
   )
   public static void formsMapTheCoatToTheirParts(GameTestHelper helper) {
      ResourceLocation seiryu = DevilFruits.id("uo_uo_seiryu");
      List<String> claw = HakiFormParts.parts(seiryu, "hybrid", 1);
      helper.assertTrue(claw.contains("right_hand") && !claw.contains("right_forearm"), "the Hybrid's right fist is its hand: " + claw);
      helper.assertTrue(HakiFormParts.parts(seiryu, "hybrid", 4).contains("right_forearm"), "its arm takes the forearm too");
      List<String> tail = HakiFormParts.parts(seiryu, "dragon", 256);
      helper.assertTrue(tail.contains("tail") && tail.contains("tail_tip"), "the Dragon's weapon is its tail: " + tail);
      helper.assertTrue(HakiFormParts.parts(seiryu, "dragon", 64).contains("body"), "its torso is the whole body");
      helper.assertTrue((HakiFormParts.support(seiryu, "hybrid") & 12) == 12, "the Hybrid can show coated arms");
      helper.assertTrue(HakiFormParts.parts(seiryu, "unknown", 511).isEmpty(), "an unknown form maps nothing");
      helper.succeed();
   }

   @GameTest(
      template = "platform",
      batch = "haki"
   )
   public static void theStrongerHakiWinsTheClash(GameTestHelper helper) {
      RandomSource random = RandomSource.create(7L);
      helper.assertTrue(HakiClash.resolve(10, 1.0F, false, 2, 0.1F, 1.0F, random).attackerWins(), "a master beats a novice");
      helper.assertTrue(!HakiClash.resolve(2, 0.1F, false, 10, 1.0F, 1.0F, random).attackerWins(), "a novice loses to a master's guard");
      helper.succeed();
   }
}
