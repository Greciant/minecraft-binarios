package com.devilfruits.fruits.uo_uo_seiryu.gametest;

import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.ChargedJump;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.network.JumpInputPayload;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuAbilities;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.ability.DraconicAscensionAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.DraconicTailSlamAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.DraconicThunderfallAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.HellfireRayAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.HybridCombat;
import com.devilfruits.fruits.uo_uo_seiryu.ability.HybridStun;
import com.devilfruits.fruits.uo_uo_seiryu.ability.ThundercladVeilAbility;
import com.devilfruits.fruits.uo_uo_seiryu.ability.ThunderclawRushAbility;
import com.devilfruits.registry.DFAttachments;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class HybridMovesetGameTests {
   private static final String PLATFORM = "platform";

   static FakePlayer hybrid(GameTestHelper helper, BlockPos relative) {
      FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "seiryu_gpo_test"));
      BlockPos pos = helper.absolutePos(relative);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      FruitRules.grantPower(player, UoUoSeiryu.fruit(), false);
      PlayerFruitData data = DFAttachments.data(player);
      data.setMastery(10);
      data.setEnergy(1000.0F);
      FormManager.request(player, "hybrid", FormManager.Reason.COMMAND);
      player.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
      return player;
   }

   static void done(GameTestHelper helper, FakePlayer player) {
      AbilityManager.cancelAll(player);
      AbilityManager.forget(player.getUUID());
      HybridCombat.unroot(player);
      FormManager.forget(player);
      FruitRules.removePower(player);

      for (DevilFruit fruit : FruitRegistry.all()) {
         WorldFruitData.get(helper.getLevel().getServer()).setFree(fruit);
      }

      helper.succeed();
   }

   private static float blow(Mob mob, FakePlayer attacker) {
      mob.setHealth(mob.getMaxHealth());
      mob.invulnerableTime = 0;
      float before = mob.getHealth();
      mob.hurt(attacker.damageSources().mobAttack(attacker), 4.0F);
      return before - mob.getHealth();
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60
   )
   public static void thundercladVeilBuffsThenExpires(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 2));
      ThundercladVeilAbility veil = SeiryuAbilities.THUNDERCLAD_VEIL;
      Mob husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 4));
      float plain = blow(husk, player);
      helper.assertTrue(HybridCombat.incomingFactor(player) == 1.0F && HybridCombat.outgoingFactor(player) == 1.0F, "no buff without the veil");
      helper.assertTrue(AbilityManager.press(player, veil) == CastResult.TOGGLED, "the veil goes up");
      helper.assertTrue(HybridCombat.veiled(player) && FormFlags.has(player, ThundercladVeilAbility.FLAG), "veiled, and every client is told");
      helper.assertTrue(veil.remainingTicks(player) == veil.durationTicks() && veil.durationTicks() == 600, "it lasts 30 s");
      float buffed = blow(husk, player);
      helper.assertTrue(Math.abs(buffed - plain * 1.1F) < 0.05F, "+10 % damage dealt: " + plain + " -> " + buffed);
      helper.assertTrue(Math.abs(HybridCombat.incomingFactor(player) - 0.75F) < 1.0E-4F, "-25 % damage taken");
      helper.assertTrue(AbilityManager.press(player, SeiryuAbilities.THUNDERCLAW_RUSH) == CastResult.BUSY, "the cocoon holds the user for a moment");
      veil.backdate(player, veil.durationTicks());
      CoreEvents.tickServer(player, DFAttachments.data(player));
      helper.assertFalse(HybridCombat.veiled(player) || FormFlags.has(player, ThundercladVeilAbility.FLAG), "the veil ends after its time");
      helper.assertTrue(DFAttachments.data(player).isOnCooldown(veil.id(), helper.getLevel().getGameTime()), "and its cooldown starts");
      helper.assertTrue(Math.abs(blow(husk, player) - plain) < 0.05F, "back to plain damage");
      DFAttachments.data(player).clearCooldowns();
      AbilityManager.clearBusy(player);
      helper.assertTrue(AbilityManager.press(player, veil) == CastResult.TOGGLED, "raised again");
      FormManager.revert(player, FormManager.Reason.COMMAND);
      helper.assertFalse(HybridCombat.veiled(player), "reverting to the human form drops the veil");
      done(helper, player);
   }

   static Mob dummy(GameTestHelper helper, BlockPos relative, double health) {
      Mob husk = helper.spawnWithNoFreeWill(EntityType.HUSK, relative);
      husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
      husk.setHealth((float)health);
      return husk;
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80,
      batch = "seiryu_gpo_thunderfall_short"
   )
   public static void thunderfallTapStillCallsTheMinimum(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 1));
      Mob husk = dummy(helper, new BlockPos(2, 2, 4), 600.0);
      husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
      DraconicThunderfallAbility fall = SeiryuAbilities.DRACONIC_THUNDERFALL;
      helper.assertTrue(AbilityManager.press(player, fall) == CastResult.CHANNELLING, "Thunderfall is held");
      helper.onEachTick(() -> CoreEvents.tickServer(player, DFAttachments.data(player)));
      helper.runAfterDelay(3L, () -> AbilityManager.release(player, fall));
      helper.runAfterDelay(40L, () -> {
         float dealt = 600.0F - husk.getHealth();
         float six = expected(fall.config().damage()) * 6.0F;
         helper.assertTrue(Math.abs(dealt - six) < six * 0.05F, "a tap still calls six strikes, all on the only enemy: " + dealt + " of " + six);
         helper.assertTrue(DFAttachments.data(player).isOnCooldown(fall.id(), helper.getLevel().getGameTime()), "then the cooldown runs");
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 120,
      batch = "seiryu_gpo_thunderfall_long"
   )
   public static void thunderfallHeldToTheEndCallsTheMaximum(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 1));
      Mob husk = dummy(helper, new BlockPos(2, 2, 4), 900.0);
      husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
      DraconicThunderfallAbility fall = SeiryuAbilities.DRACONIC_THUNDERFALL;
      helper.assertTrue(AbilityManager.press(player, fall) == CastResult.CHANNELLING, "Thunderfall is held");
      helper.onEachTick(() -> CoreEvents.tickServer(player, DFAttachments.data(player)));
      helper.runAfterDelay(30L, () -> {
         int mid = fall.strikes(player);
         helper.assertTrue(mid >= 6 && mid < 18, "strikes keep coming while it is held: " + mid);
      });
      helper.runAfterDelay((long)(fall.maxChannelTicks() + 8), () -> {
         helper.assertTrue(fall.strikes(player) == -1, "the channel ends by itself at the maximum");
         float dealt = 900.0F - husk.getHealth();
         float all = expected(fall.config().damage()) * 18.0F;
         helper.assertTrue(Math.abs(dealt - all) < all * 0.05F, "eighteen strikes: " + dealt + " of " + all);
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 110,
      batch = "seiryu_gpo_hellfire"
   )
   public static void hellfireRayHitsFortyNineTimes(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 1));
      Mob husk = dummy(helper, new BlockPos(2, 2, 3), 600.0);
      husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
      HellfireRayAbility ray = SeiryuAbilities.HELLFIRE_RAY;
      int windup = 8;
      helper.assertTrue(ray.rayTicks() == 70 && ray.maxChannelTicks() == windup + 70, "a 0.4 s wind-up, then 3.5 s of ray at most");
      helper.assertTrue(AbilityManager.press(player, ray) == CastResult.CHANNELLING, "Hellfire Ray is held");
      helper.onEachTick(() -> CoreEvents.tickServer(player, DFAttachments.data(player)));
      helper.runAfterDelay(
         (long)(windup - 2), () -> helper.assertTrue(ray.hits(player) == 0 && husk.getHealth() == 600.0F, "nothing is hit during the wind-up")
      );
      helper.runAfterDelay((long)(windup + 35), () -> {
         int mid = ray.hits(player);
         helper.assertTrue(mid >= 22 && mid <= 27, "the hits are spread over the channel: " + mid + " at half time");
         helper.assertTrue(HybridStun.stunned(husk), "what is caught in the rings is held there");
      });
      helper.runAfterDelay((long)(ray.maxChannelTicks() + 6), () -> {
         helper.assertTrue(ray.hits(player) == -1, "the channel ends by itself");
         float dealt = 600.0F - husk.getHealth();
         float all = expected(ray.config().damage()) * 49.0F;
         helper.assertTrue(Math.abs(dealt - all) < all * 0.05F, "49 hits: " + dealt + " of " + all);
         helper.assertTrue(DFAttachments.data(player).isOnCooldown(ray.id(), helper.getLevel().getGameTime()), "then the cooldown runs");
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60,
      batch = "seiryu_gpo_hellfire_short"
   )
   public static void hellfireRayEndsOnRelease(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 1));
      Mob husk = dummy(helper, new BlockPos(2, 2, 3), 600.0);
      HellfireRayAbility ray = SeiryuAbilities.HELLFIRE_RAY;
      helper.assertTrue(AbilityManager.press(player, ray) == CastResult.CHANNELLING, "Hellfire Ray is held");
      helper.onEachTick(() -> CoreEvents.tickServer(player, DFAttachments.data(player)));
      int[] atRelease = new int[]{0};
      helper.runAfterDelay(28L, () -> {
         atRelease[0] = ray.hits(player);
         AbilityManager.release(player, ray);
      });
      helper.runAfterDelay(38L, () -> {
         helper.assertTrue(atRelease[0] >= 12 && atRelease[0] <= 15, "about 14 hits in a second: " + atRelease[0]);
         float dealt = 600.0F - husk.getHealth();
         float expectedDealt = expected(ray.config().damage()) * (float)atRelease[0];
         helper.assertTrue(Math.abs(dealt - expectedDealt) < expectedDealt * 0.06F, "and nothing after the release: " + dealt + " of " + expectedDealt);
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60,
      batch = "seiryu_gpo_hellfire_tap"
   )
   public static void hellfireRayTappedStillFires(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 1));
      Mob husk = dummy(helper, new BlockPos(2, 2, 3), 600.0);
      HellfireRayAbility ray = SeiryuAbilities.HELLFIRE_RAY;
      helper.assertTrue(AbilityManager.press(player, ray) == CastResult.CHANNELLING, "Hellfire Ray is held");
      helper.onEachTick(() -> CoreEvents.tickServer(player, DFAttachments.data(player)));
      helper.runAfterDelay(2L, () -> AbilityManager.release(player, ray));
      helper.runAfterDelay((long)(ray.minChannelTicks() - 3), () -> helper.assertTrue(ray.hits(player) > 0, "the release still fires the ray"));
      helper.runAfterDelay((long)(ray.minChannelTicks() + 4), () -> {
         helper.assertTrue(ray.hits(player) == -1, "a tapped ray ends after its shortest burst");
         float dealt = 600.0F - husk.getHealth();
         float one = expected(ray.config().damage());
         helper.assertTrue(dealt >= one * 3.0F && dealt <= one * 9.0F, "a short burst of hits: " + dealt / one);
         helper.assertTrue(DFAttachments.data(player).isOnCooldown(ray.id(), helper.getLevel().getGameTime()), "and the cooldown runs");
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 20,
      batch = "seiryu_gpo_hellfire_tap"
   )
   public static void hellfireRayBeatsMatchTheAnimation(GameTestHelper helper) {
      String path = "/assets/devilfruits/geo_animations/uo_uo_seiryu/hybrid.animation.json";

      try (InputStream in = HybridMovesetGameTests.class.getResourceAsStream(path)) {
         helper.assertTrue(in != null, "the Hybrid clips are packed");
         JsonObject clips = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
         float release = clips.getAsJsonObject("animation.hybrid.hellfire_ray_windup").getAsJsonObject("devilfruits:markers").get("release").getAsFloat();
         float explode = clips.getAsJsonObject("animation.hybrid.hellfire_ray_end").getAsJsonObject("devilfruits:markers").get("explode").getAsFloat();
         helper.assertTrue(release == 8.0F, "the wind-up ends on the clip's release marker: " + release);
         helper.assertTrue(explode == 4.0F, "the explosion lands on the end clip's explode marker: " + explode);
      } catch (IOException var8) {
         helper.fail("clips unreadable: " + var8);
      }

      helper.succeed();
   }

   private static float clipMarker(String clip, String name) {
      String path = "/assets/devilfruits/geo_animations/uo_uo_seiryu/hybrid.animation.json";

      try {
         float var5;
         try (InputStream in = HybridMovesetGameTests.class.getResourceAsStream(path)) {
            if (in == null) {
               return Float.NaN;
            }

            JsonObject c = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
               .getAsJsonObject()
               .getAsJsonObject("animations")
               .getAsJsonObject("animation.hybrid." + clip);
            var5 = c != null && c.has("devilfruits:markers") && c.getAsJsonObject("devilfruits:markers").has(name)
               ? c.getAsJsonObject("devilfruits:markers").get(name).getAsFloat()
               : Float.NaN;
         }

         return var5;
      } catch (IOException var8) {
         return Float.NaN;
      }
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 20
   )
   public static void gpoBeatsMatchTheAnimation(GameTestHelper helper) {
      int[][] expected = new int[][]{
         {8, Math.round(clipMarker("thunderclad_veil", "cocoon"))},
         {24, Math.round(clipMarker("thunderclad_veil", "burst"))},
         {3, Math.round(clipMarker("thunderclaw_rush", "lunge"))},
         {2, Math.round(clipMarker("thunderclaw_claws", "claw1"))},
         {8, Math.round(clipMarker("thunderclaw_claws", "claw2"))},
         {14, Math.round(clipMarker("thunderclaw_claws", "claw3"))},
         {22, Math.round(clipMarker("thunderclaw_claws", "finisher"))},
         {4, Math.round(clipMarker("thunderfall_release", "volley"))},
         {8, Math.round(clipMarker("draconic_ascension", "launch"))},
         {8, Math.round(clipMarker("draconic_cataclysm", "leap"))},
         {29, Math.round(clipMarker("draconic_cataclysm", "slam"))},
         {3, Math.round(clipMarker("draconic_tail_slam", "sweep_start"))},
         {8, Math.round(clipMarker("draconic_tail_slam", "sweep_end"))}
      };

      for (int i = 0; i < expected.length; i++) {
         helper.assertTrue(expected[i][0] == expected[i][1], "beat " + i + ": server tick " + expected[i][0] + " vs clip marker " + expected[i][1]);
      }

      helper.succeed();
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60,
      batch = "seiryu_gpo_ascension"
   )
   public static void draconicAscensionLeapsAndSlams(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 1));
      Mob husk = dummy(helper, new BlockPos(2, 2, 3), 300.0);
      DraconicAscensionAbility dive = SeiryuAbilities.DRACONIC_ASCENSION;
      helper.assertTrue(AbilityManager.press(player, dive) == CastResult.SUCCESS, "the dive casts");
      helper.assertTrue(AbilityManager.press(player, SeiryuAbilities.THUNDERCLAW_RUSH) == CastResult.BUSY, "nothing else meanwhile");
      helper.runAfterDelay(11L, () -> helper.assertTrue(player.getDeltaMovement().y > 0.2, "the body launches upwards: " + player.getDeltaMovement()));
      helper.runAfterDelay((long)(8 + (int)dive.config().extra("flightTicks") + 3), () -> {
         float dealt = 300.0F - husk.getHealth();
         float one = expected(dive.config().damage());
         helper.assertTrue(Math.abs(dealt - one) < one * 0.05F, "one slam: " + dealt + " of " + one);
         helper.assertTrue(HybridStun.stunned(husk), "which stuns");
         helper.assertTrue(player.fallDistance == 0.0F, "the leap causes no fall damage");
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 40,
      batch = "seiryu_gpo_tail_slam"
   )
   public static void tailSlamSweepsTheFrontHalf(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 2));
      Mob left = dummy(helper, new BlockPos(4, 2, 3), 200.0);
      Mob right = dummy(helper, new BlockPos(0, 2, 3), 200.0);
      Mob behind = dummy(helper, new BlockPos(2, 2, 0), 200.0);
      DraconicTailSlamAbility slam = SeiryuAbilities.DRACONIC_TAIL_SLAM;
      helper.assertTrue(AbilityManager.press(player, slam) == CastResult.SUCCESS, "the tail slam casts");
      helper.runAfterDelay(
         5L,
         () -> helper.assertTrue(
               left.getHealth() < 200.0F && right.getHealth() >= 200.0F, "the tail comes round from the left: " + left.getHealth() + " / " + right.getHealth()
            )
      );
      helper.runAfterDelay(
         11L,
         () -> {
            float one = expected(slam.config().damage());
            helper.assertTrue(
               Math.abs(200.0F - left.getHealth() - one) < one * 0.05F && Math.abs(200.0F - right.getHealth() - one) < one * 0.05F,
               "both sides of the front are hit once: " + left.getHealth() + " / " + right.getHealth() + " of " + one
            );
            helper.assertTrue(behind.getHealth() >= 200.0F, "but not what stands behind");
            left.discard();
            right.discard();
            behind.discard();
            done(helper, player);
         }
      );
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 40,
      batch = "seiryu_gpo_breath"
   )
   public static void boloBreathFiresTheHybridBeam(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 1));
      player.setXRot(30.0F);
      Mob husk = dummy(helper, new BlockPos(2, 2, 5), 200.0);
      helper.assertTrue(AbilityManager.press(player, SeiryuAbilities.BLAST_BREATH) == CastResult.CHARGING, "the breath charges");
      helper.assertTrue(AbilityManager.release(player, SeiryuAbilities.BLAST_BREATH) == CastResult.SUCCESS, "and fires");
      helper.assertTrue(AbilityManager.press(player, SeiryuAbilities.THUNDERCLAW_RUSH) == CastResult.BUSY, "the beam keeps the jaws busy");
      helper.runAfterDelay(6L, () -> {
         helper.assertTrue(husk.getHealth() < 200.0F && husk.isOnFire(), "the beam burns what it plays over: " + husk.getHealth());
         husk.discard();
         done(helper, player);
      });
   }

   static float expected(double weight) {
      return (float)(weight * (double)HybridCombat.baseHit() * (double)Mastery.damageMultiplier(10));
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80,
      batch = "seiryu_gpo_rush"
   )
   public static void thunderclawRushRakesThreeTimes(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 1));
      Mob husk = dummy(helper, new BlockPos(2, 2, 3), 400.0);
      ThunderclawRushAbility rush = SeiryuAbilities.THUNDERCLAW_RUSH;
      helper.assertTrue(AbilityManager.press(player, rush) == CastResult.SUCCESS, "the rush casts");
      helper.assertTrue(DFAttachments.data(player).isOnCooldown(rush.id(), helper.getLevel().getGameTime()), "its cooldown starts");
      int arrival = 13;
      helper.runAfterDelay((long)(arrival + 2 + 2), () -> helper.assertTrue(HybridStun.stunned(husk), "the first claw stuns"));
      helper.runAfterDelay((long)(arrival + 22 + 4), () -> {
         float dealt = 400.0F - husk.getHealth();
         float three = expected(rush.config().damage()) * 3.0F;
         helper.assertTrue(Math.abs(dealt - three) < three * 0.05F, "three claws, no finisher without the veil: " + dealt + " of " + three);
         helper.assertFalse(HybridStun.stunned(husk), "the third claw throws it out of the stun");
         husk.discard();
         done(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 90,
      batch = "seiryu_gpo_rush_veil"
   )
   public static void thunderclawRushUnderTheVeilCallsLightning(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 1));
      Mob husk = dummy(helper, new BlockPos(2, 2, 3), 400.0);
      ThunderclawRushAbility rush = SeiryuAbilities.THUNDERCLAW_RUSH;
      helper.assertTrue(AbilityManager.press(player, SeiryuAbilities.THUNDERCLAD_VEIL) == CastResult.TOGGLED, "the veil goes up");
      AbilityManager.clearBusy(player);
      helper.assertTrue(AbilityManager.press(player, rush) == CastResult.SUCCESS, "the rush casts under the veil");
      int arrival = 13;
      helper.runAfterDelay((long)(arrival + 22 + 4), () -> {
         float dealt = 400.0F - husk.getHealth();
         float all = (expected(rush.config().damage()) * 3.0F + expected(rush.config().extra("finisherWeight"))) * 1.1F;
         helper.assertTrue(Math.abs(dealt - all) < all * 0.05F, "three claws and the lightning, +10 %: " + dealt + " of " + all);
         husk.discard();
         done(helper, player);
      });
   }

   private static void jump(FakePlayer player, JumpInputPayload.Action action, float dx, float dz) {
      ChargedJump.handle(player, new JumpInputPayload(action, dx, dz));
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 80,
      batch = "seiryu_charged_jump"
   )
   public static void chargedJumpLeapsByTheCharge(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 2));
      player.setOnGround(true);
      jump(player, JumpInputPayload.Action.START, 0.0F, 0.0F);
      helper.assertTrue(ChargedJump.charging(player), "Shift + Space on the ground starts the charge");
      jump(player, JumpInputPayload.Action.RELEASE, 0.0F, 0.0F);
      helper.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0E-6 && !ChargedJump.charging(player), "too short a charge: no leap");
      jump(player, JumpInputPayload.Action.START, 0.0F, 0.0F);
      helper.runAfterDelay(
         15L,
         () -> {
            jump(player, JumpInputPayload.Action.RELEASE, 0.0F, 0.0F);
            double v = player.getDeltaMovement().y;
            double h = ChargedJump.peak(v, player.getGravity());
            helper.assertTrue(
               Math.abs(h - 13.125) < 1.2 && Math.abs(player.getDeltaMovement().x) + Math.abs(player.getDeltaMovement().z) < 1.0E-6,
               "half a charge: 13 blocks straight up, got " + h
            );
            ChargedJump.forget(player);
            player.setDeltaMovement(0.0, 0.0, 0.0);
            jump(player, JumpInputPayload.Action.START, 0.0F, 0.0F);
            helper.runAfterDelay(32L, () -> {
               jump(player, JumpInputPayload.Action.RELEASE, 0.0F, 1.0F);
               Vec3 m = player.getDeltaMovement();
               double full = ChargedJump.peak(m.y, player.getGravity());
               helper.assertTrue(Math.abs(full - 30.0) < 0.1, "a full charge peaks 30 blocks up, got " + full);
               helper.assertTrue(Math.abs(m.z - 3.1) < 0.001 && Math.abs(m.x) < 1.0E-6, "and drives hard along the keys: " + m);
               ChargedJump.forget(player);
               FormManager.revert(player, FormManager.Reason.COMMAND);
               jump(player, JumpInputPayload.Action.START, 0.0F, 0.0F);
               helper.assertFalse(ChargedJump.charging(player), "the human form has no charged jump");
               ChargedJump.forget(player);
               done(helper, player);
            });
         }
      );
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 20
   )
   public static void hybridDoesNotBurn(GameTestHelper helper) {
      FakePlayer player = hybrid(helper, new BlockPos(2, 2, 2));
      player.igniteForSeconds(5.0F);
      helper.assertTrue(player.fireImmune() && !player.isOnFire(), "the Hybrid does not catch fire");
      float health = player.getHealth();
      player.invulnerableTime = 0;
      player.hurt(player.damageSources().inFire(), 4.0F);
      helper.assertTrue(player.getHealth() == health, "nor take fire damage");
      FormManager.revert(player, FormManager.Reason.COMMAND);
      helper.assertFalse(player.fireImmune(), "the human form burns as before");
      player.clearFire();
      done(helper, player);
   }
}
