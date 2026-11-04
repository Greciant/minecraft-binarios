package com.devilfruits.fruits.supa_supa.gametest;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.config.AbilityConfig;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.fruits.supa_supa.SupaAbilities;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.fruits.supa_supa.ability.SparClawAbility;
import com.devilfruits.fruits.supa_supa.ability.SpiralHollowAbility;
import com.devilfruits.fruits.supa_supa.slash.SlashPlane;
import com.devilfruits.fruits.supa_supa.slash.SuperSlash;
import com.devilfruits.registry.DFAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class SupaSlashGameTests {
   private static final String PLATFORM = "platform";

   private static AbilityContext slashContext(FakePlayer player) {
      return new AbilityContext(new FormAbility(SupaAbilities.SUPER_SLASH), player, DFAttachments.data(player), 1.0F);
   }

   private static void release(GameTestHelper helper, FakePlayer player) {
      AbilityManager.cancelAll(player);
      AbilityManager.forget(player.getUUID());
      FruitRules.removePower(player);
      WorldFruitData.get(helper.getLevel().getServer()).setFree(SupaSupa.fruit());
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60
   )
   public static void supaSlashPierce(GameTestHelper helper) {
      FakePlayer player = SupaGameTests.user(helper, new BlockPos(2, 1, 0));
      Zombie first = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
      Zombie second = (Zombie)helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
      first.setNoAi(true);
      second.setNoAi(true);

      for (int y = 1; y <= 4; y++) {
         for (int x = 0; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, y, 4), Blocks.OBSIDIAN);
         }
      }

      Vec3 base = Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(2, 1, 0))).add(0.5, 0.0, 0.6);
      SuperSlash.launch(slashContext(player), 1.0F, base, new Vec3(0.0, 0.0, 1.0));
      helper.succeedWhen(() -> {
         helper.assertTrue(first.getHealth() < first.getMaxHealth(), "the first creature is cut");
         helper.assertTrue(second.getHealth() < second.getMaxHealth(), "and the one behind it");
         helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(2, 1, 4));
         release(helper, player);
      });
   }

   @GameTest(
      template = "platform",
      timeoutTicks = 60
   )
   public static void supaSlashSlit(GameTestHelper helper) {
      FakePlayer player = SupaGameTests.user(helper, new BlockPos(2, 1, 0));

      for (int y = 1; y <= 5; y++) {
         for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
         }

         helper.setBlock(new BlockPos(2, y, 3), Blocks.OBSIDIAN);
      }

      Vec3 base = Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(2, 1, 0))).add(0.5, 0.0, 0.6);
      SuperSlash.launch(slashContext(player), 1.0F, base, new Vec3(0.0, 0.0, 1.0));
      helper.succeedWhen(() -> {
         for (int y = 1; y <= 5; y++) {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, y, 2));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(1, y, 2));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(3, y, 2));
            helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(2, y, 3));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, y, 4));
         }

         release(helper, player);
      });
   }

   @GameTest(
      template = "platform"
   )
   public static void supaSlashPlane(GameTestHelper helper) {
      SlashPlane p = SlashPlane.of(new Vec3(0.2, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0), new Vec3(3.4, 70.3, 8.2));
      helper.assertTrue(p.along() == Axis.Z && p.sign() == 1, "travel along +Z: " + p);
      helper.assertTrue(p.across() == Axis.X && p.boundary() == 3, "the plane on the boundary x = 3: " + p);
      helper.assertTrue(p.side(new BlockPos(2, 0, 0)) == -1 && p.side(new BlockPos(3, 0, 0)) == 1, "blocks either side, none on it");
      helper.assertTrue(p.against(new BlockPos(2, 5, 9)) && p.against(new BlockPos(3, 5, 9)) && !p.against(new BlockPos(4, 5, 9)), "the layers against it");
      List<BlockPos> hull = new ArrayList<>();

      for (int x = 0; x <= 4; x++) {
         for (int z = 8; z <= 14; z++) {
            hull.add(new BlockPos(x, 70, z));
         }
      }

      SlashPlane.Partition parts = p.partition(hull);
      helper.assertTrue(parts.minus().size() == 21 && parts.plus().size() == 14, "3 columns | 2 columns");
      helper.assertTrue(parts.smaller() == parts.plus() && parts.smallerSide() == 1, "the smaller side moves");
      helper.assertTrue(Math.abs(p.glideLength(8.2, hull) - 6.8) < 1.0E-6, "glides to the hull's far end: " + p.glideLength(8.2, hull));
      Vec3 local = new Vec3(0.0, 0.0, 1.0).yRot((float)Math.toRadians(90.0)).zRot((float)Math.toRadians(18.0));
      Vec3 localUp = new Vec3(0.0, 1.0, 0.0).yRot((float)Math.toRadians(90.0)).zRot((float)Math.toRadians(18.0));
      SlashPlane turned = SlashPlane.of(local, localUp, new Vec3(5.6, 64.2, -2.3));
      helper.assertTrue(turned.along() == Axis.X, "turned: along local X: " + turned + " from " + local);
      helper.assertTrue(turned.sign() == (local.x > 0.0 ? 1 : -1), "turned: the travel's sign");
      helper.assertTrue(turned.across() == Axis.Z && turned.boundary() == -2, "turned: snapped on local Z = -2: " + turned);
      helper.assertTrue(
         Math.abs(turned.normal().dot(new Vec3(0.0, 1.0, 0.0))) < 1.0E-9 && Math.abs(turned.normal().dot(turned.travel())) < 1.0E-9,
         "the plane holds the local up and the travel"
      );
      SlashPlane fallen = SlashPlane.of(new Vec3(0.0, 0.0, 1.0), new Vec3(1.0, 0.0, 0.0), new Vec3(2.2, 70.6, -4.0));
      helper.assertTrue(fallen.across() == Axis.Y && fallen.boundary() == 71, "fallen: the plane across local Y: " + fallen);
      helper.assertTrue(fallen.along() == Axis.Z && fallen.span() == Axis.X && fallen.spanSign() == 1, "fallen: along Z, standing along X");
      SlashPlane rising = SlashPlane.of(new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, -1.0), new Vec3(0.4, 60.0, 3.0));
      helper.assertTrue(
         rising.along() == Axis.Y && rising.sign() == 1 && rising.across() == Axis.X && rising.span() == Axis.Z && rising.spanSign() == -1,
         "rising: up along Y: " + rising
      );
      SlashPlane back = SlashPlane.unpack(fallen.onPlane(new Vec3(2.2, 70.6, -4.0)), fallen.travel().scale(0.2), fallen.pack(3));
      helper.assertTrue(back.equals(fallen) && SlashPlane.index(fallen.pack(3)) == 3, "packed and unpacked: " + back);
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void supaSlashTimings(GameTestHelper helper) {
      AbilityConfig config = SupaAbilities.SUPER_SLASH.config();
      double glide = config.extra("structureGlideSpeed");
      int maxTicks = (int)config.extra("maxGlideTicks");
      double full = config.extra("speed");
      double exit = config.extra("exitDistance");
      helper.assertTrue(
         Math.abs(glide - 0.15) < 1.0E-9 && maxTicks == 100 && exit == 5.0 && (int)config.extra("splitDelayTicks") == 40,
         "the defaults: 0.15 b/t, 100 ticks, 5 blocks, a 40-tick pause"
      );
      double shortSpeed = SlashPlane.glideSpeed(7.5, glide, maxTicks);
      helper.assertTrue(shortSpeed == glide && SlashPlane.glideTicks(7.5, shortSpeed) == 50, "a raft: 50 ticks at 0.15");
      double longSpeed = SlashPlane.glideSpeed(60.0, glide, maxTicks);
      helper.assertTrue(Math.abs(longSpeed - 0.6) < 1.0E-9 && SlashPlane.glideTicks(60.0, longSpeed) == 100, "a 60-block hull in 100 ticks");
      int ticks = SlashPlane.exitTicks(glide, full, exit);
      double flown = 0.0;

      for (int k = 0; k < ticks; k++) {
         double v = SlashPlane.exitSpeed(k, glide, full);
         helper.assertTrue(k == 0 || v >= SlashPlane.exitSpeed(k - 1, glide, full), "the exit eases in");
         flown += v;
      }

      helper.assertTrue(flown >= exit && flown - SlashPlane.exitSpeed(ticks - 1, glide, full) < exit, "it flies the 5 blocks: " + flown + " in " + ticks);
      helper.assertTrue(SlashPlane.exitSpeed(4, glide, full) == full, "back to full speed");
      double tempo = config.extra("cutAnimationSpeed");
      helper.assertTrue(tempo == 1.5, "the cut plays 1.5 times faster by default");
      helper.assertTrue(SlashPlane.glideTicks(7.5, SlashPlane.glideSpeed(7.5, glide, maxTicks, tempo)) == 34, "a raft: 34 ticks");
      helper.assertTrue(SlashPlane.glideTicks(60.0, SlashPlane.glideSpeed(60.0, glide, maxTicks, tempo)) == 67, "a 60-block hull: 67 ticks");
      helper.assertTrue(SlashPlane.pauseTicks(40, tempo) == 27 && SlashPlane.pauseTicks(40, 1.0) == 40, "the pause: 27 ticks");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void supaSlashAim(GameTestHelper helper) {
      Vec3 flat = new Vec3(0.0, 0.0, 1.0);
      Vec3 slight = new Vec3(0.0, -0.05, 1.0).normalize();
      helper.assertTrue(SuperSlash.aim(slight, flat).equals(flat), "a look 3 degrees down flies level");
      Vec3 climb = new Vec3(0.0, Math.sin(Math.toRadians(30.0)), Math.cos(Math.toRadians(30.0)));
      helper.assertTrue(SuperSlash.aim(climb, flat).distanceTo(climb) < 1.0E-9, "a look 30 degrees up climbs");
      helper.assertTrue(SuperSlash.bladeUp(flat, flat).distanceTo(new Vec3(0.0, 1.0, 0.0)) < 1.0E-9, "a level cut stands upright");
      Vec3 u = SuperSlash.bladeUp(climb, flat);
      helper.assertTrue(Math.abs(u.dot(climb)) < 1.0E-9 && u.y > 0.0 && u.z < 0.0, "a climbing cut leans back, across its flight: " + u);
      Vec3 straight = SuperSlash.bladeUp(new Vec3(0.0, 1.0, 0.0), flat);
      helper.assertTrue(straight.distanceTo(new Vec3(0.0, 0.0, -1.0)) < 1.0E-9, "straight up: lies back along the facing: " + straight);
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void supaSlashSplitRefused(GameTestHelper helper) {
      helper.assertTrue(SlashPlane.check(false, true, 10, 100, true, true) == SlashPlane.Refusal.DISABLED, "splitStructures off");
      helper.assertTrue(SlashPlane.check(true, false, 10, 100, true, true) == SlashPlane.Refusal.PROTECTED, "claims / spawn / poneglyph");
      helper.assertTrue(SlashPlane.check(true, true, 101, 100, true, true) == SlashPlane.Refusal.TOO_BIG, "over maxSplitBlocks");
      helper.assertTrue(SlashPlane.check(true, true, 10, 100, true, false) == SlashPlane.Refusal.ONE_SIDED, "one side empty");
      helper.assertTrue(SlashPlane.check(true, true, 100, 100, true, true) == SlashPlane.Refusal.NONE, "a ship at the limit is cut");
      helper.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void supaAtomicSparX(GameTestHelper helper) {
      FakePlayer player = SupaGameTests.user(helper, new BlockPos(2, 1, 0));

      for (int y = 1; y <= 3; y++) {
         for (int x = 0; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(x, y, 3), Blocks.OBSIDIAN);
         }
      }

      Vec3 centre = Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(2, 2, 0))).add(0.5, 0.5, 0.5);
      int cut = SparClawAbility.cutX(
         helper.getLevel(), player, centre, new Vec3(0.0, 0.0, 1.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0), 4.0, 1.2, new CastBudget(70, 0)
      );
      helper.assertTrue(cut > 0, "the X cuts the stone: " + cut);
      helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 2));

      for (int y = 1; y <= 3; y++) {
         for (int x = 0; x <= 4; x++) {
            helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(x, y, 3));
         }
      }

      helper.assertTrue(true, "the claw lands before the X");
      SupaGameTests.done(helper, player);
   }

   @GameTest(
      template = "platform"
   )
   public static void supaSpiralShred(GameTestHelper helper) {
      FakePlayer player = SupaGameTests.user(helper, new BlockPos(2, 1, 0));
      helper.setBlock(new BlockPos(2, 1, 1), Blocks.STONE);
      helper.setBlock(new BlockPos(2, 2, 1), Blocks.STONE);
      helper.setBlock(new BlockPos(2, 1, 2), Blocks.OBSIDIAN);
      Vec3 from = Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(2, 1, 0))).add(0.5, 1.2, 0.5);
      int shredded = SpiralHollowAbility.shred(helper.getLevel(), player, from, new Vec3(0.0, 0.0, 1.0), 2.6, 3, new CastBudget(80, 0));
      helper.assertTrue(shredded >= 2 && shredded <= 3, "the stones go, at most 3 a tick: " + shredded);
      helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 1));
      helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 1));
      helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(2, 1, 2));
      SupaGameTests.done(helper, player);
   }
}
